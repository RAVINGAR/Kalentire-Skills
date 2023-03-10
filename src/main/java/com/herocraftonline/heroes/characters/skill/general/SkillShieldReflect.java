package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.lang.reflect.Field;
import java.util.List;

import static com.herocraftonline.heroes.characters.skill.SkillConfigManager.getRaw;
import static com.herocraftonline.heroes.characters.skill.SkillConfigManager.getUseSetting;

public class SkillShieldReflect extends ActiveSkill {
    private static Field shieldItemsField = null;

    static {
        final Skill skill = Heroes.getInstance().getSkillManager().getSkill("Shield");
        if (skill != null) {
            final Class<?> skillClass = skill.getClass();

            try {
                shieldItemsField = skillClass.getDeclaredField("shieldItems");
                shieldItemsField.setAccessible(true);
            } catch (final NoSuchFieldException ex) {
                // This space intentionally left blank.
            }
        }
    }

    private String applyText;
    private String expireText;

    public SkillShieldReflect(final Heroes plugin) {
        super(plugin, "ShieldReflect");
        setDescription("Reflect incoming damage back at your attackers for $1 second(s). Reflected damage is returned at a $2% rate.");
        setUsage("/skill shieldreflect");
        setArgumentRange(0, 0);
        setIdentifiers("skill shieldreflect");
        setTypes(SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_PHYSICAL);

        Bukkit.getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double damageModifier = getUseSetting(hero, this, "reflected-damage-modifier", 0.8, false);
        final int duration = getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);

        final String formattedDuration = Util.decFormat.format(duration / 1000.0);
        final String formattedDamageModifier = Util.decFormat.format(damageModifier * 100);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedDamageModifier);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 3000);
        node.set("reflected-damage-modifier", 0.8);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% holds up their shield and is now reflecting incoming attacks!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer reflecting attacks!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% holds up their shield and is now reflecting incoming attacks!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer reflecting attacks!").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        if (isWieldingShield(hero)) {
            broadcastExecuteText(hero);

            final int duration = getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
            hero.addEffect(new ShieldReflectEffect(this, player, duration));

            player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.8F, 1.0F);

            return SkillResult.NORMAL;
        } else {
            player.sendMessage("You must have a shield equipped to use this skill");
            return SkillResult.FAIL;
        }
    }

    @SuppressWarnings("unchecked") // Probably not the best way, but it's a pain to do it any other.
    private boolean isWieldingShield(final Hero hero) {
        final Material type = NMSHandler.getInterface().getItemInOffHand(hero.getPlayer().getInventory()).getType();
        if (type == Material.SHIELD) {
            return true;
        } else if (shieldItemsField != null) {
            final Skill skill = Heroes.getInstance().getSkillManager().getSkill("Shield");
            if (skill != null && hero.hasAccessToSkill(skill)) {
                try {
                    final List<Material> shieldItems = (List<Material>) shieldItemsField.get(skill);
                    if (shieldItems.contains(type)) {
                        return true;
                    }
                } catch (final IllegalAccessException ex) {
                    // This space intentionally left blank.
                }
            }
        }
        return false;
    }

    public class SkillEntityListener implements Listener {
        private final Skill skill;

        public SkillEntityListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(final SkillDamageEvent event) {
            if (!(event.getEntity() instanceof Player)) {
                return;
            }

            final Hero defenderHero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (defenderHero.hasEffect("ShieldReflect")) {
                final CharacterTemplate attackerCT = event.getDamager();
                if ((attackerCT instanceof Player)) {
                    final Player attackerPlayer = (Player) attackerCT;
                    if (plugin.getCharacterManager().getHero(attackerPlayer).hasEffect(getName())) {
                        event.setCancelled(true);
                        return;
                    }
                    if (attackerPlayer.getName().equalsIgnoreCase(defenderHero.getPlayer().getName())) {
                        event.setCancelled(true);
                        return;
                    }
                }

                final Player defenderPlayer = defenderHero.getPlayer();
                if (isWieldingShield(defenderHero)) {
                    final double damageModifier = getUseSetting(defenderHero, skill, "reflected-damage-modifier", 0.8, false);
                    final double damage = event.getDamage() * damageModifier;
                    final LivingEntity target = event.getDamager().getEntity();

                    final Skill eSkill = event.getSkill();

                    addSpellTarget(target, defenderHero);
                    if (eSkill.isType(SkillType.ABILITY_PROPERTY_PHYSICAL) && !eSkill.isType(SkillType.ARMOR_PIERCING)) {
                        damageEntity(target, defenderPlayer, damage, DamageCause.ENTITY_ATTACK, false);
                    } else {
                        damageEntity(target, defenderPlayer, damage, DamageCause.MAGIC, false);
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamage(final EntityDamageEvent event) {
            if (!(event instanceof EntityDamageByEntityEvent)) {
                return;
            }

            final EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;
            final Entity defender = edbe.getEntity();
            final Entity attacker = edbe.getDamager();
            if (((attacker instanceof LivingEntity)) && ((defender instanceof Player))) {
                final Player defenderPlayer = (Player) defender;
                final Hero defenderHero = plugin.getCharacterManager().getHero(defenderPlayer);
                if (defenderHero.hasEffect("ShieldReflect")) {
                    if ((attacker instanceof Player)) {
                        final Player attackerPlayer = (Player) attacker;
                        if (plugin.getCharacterManager().getHero(attackerPlayer).hasEffect(getName())) {
                            event.setCancelled(true);
                            return;
                        }
                    }

                    if (isWieldingShield(defenderHero)) {
                        final double damageModifier = getUseSetting(defenderHero, skill, "reflected-damage-modifier", 0.8, false);
                        final double damage = event.getDamage() * damageModifier;

                        final LivingEntity target = (LivingEntity) attacker;
                        addSpellTarget(target, defenderHero);
                        damageEntity(target, defenderPlayer, damage, DamageCause.ENTITY_ATTACK, false);
                    }
                }
            }
        }

    }

    public class ShieldReflectEffect extends ExpirableEffect {
        public ShieldReflectEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, "ShieldReflect", applier, duration, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
        }
    }
}
