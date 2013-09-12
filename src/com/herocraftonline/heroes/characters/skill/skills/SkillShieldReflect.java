package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillShieldReflect extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillShieldReflect(Heroes plugin) {
        super(plugin, "ShieldReflect");
        setDescription("Reflect incoming damage back at your attackers for $1 seconds. Reflected damage is returned at a $2% rate.");
        setUsage("/skill shieldreflect");
        setArgumentRange(0, 0);
        setIdentifiers("skill shieldreflect");
        setTypes(SkillType.SILENCABLE, SkillType.ABILITY_PROPERTY_PHYSICAL);

        Bukkit.getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double damageModifier = SkillConfigManager.getUseSetting(hero, this, "reflected-damage-modifier", Double.valueOf(0.8), false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(3000), false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDamageModifier = Util.decFormat.format(damageModifier * 100);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedDamageModifier);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), Integer.valueOf(3000));
        node.set("reflected-damage-modifier", Double.valueOf(0.8));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% holds up their shield and is now reflecting incoming attacks!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is no longer reflecting attacks!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% holds up their shield and is now reflecting incoming attacks!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% is no longer reflecting attacks!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        switch (player.getItemInHand().getType()) {
            case IRON_DOOR:
            case WOOD_DOOR:
            case TRAP_DOOR:
                broadcastExecuteText(hero);

                int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(3000), false);
                hero.addEffect(new ShieldReflectEffect(this, player, duration));

                player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
                player.getWorld().playSound(player.getLocation(), Sound.ZOMBIE_METAL, 0.8F, 1.0F);

                return SkillResult.NORMAL;
            default:
                Messaging.send(player, "You must have a shield equipped to use this skill");
                return SkillResult.FAIL;
        }
    }

    public class SkillEntityListener implements Listener {
        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (!(event.getEntity() instanceof Player))
                return;

            Hero defenderHero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (defenderHero.hasEffect("ShieldReflect")) {
                CharacterTemplate attackerCT = event.getDamager();
                if ((attackerCT instanceof Player)) {
                    Player attackerPlayer = (Player) attackerCT;
                    if (plugin.getCharacterManager().getHero(attackerPlayer).hasEffect(getName())) {
                        event.setCancelled(true);
                        return;
                    }
                }

                Player defenderPlayer = defenderHero.getPlayer();
                switch (defenderPlayer.getItemInHand().getType()) {
                    case IRON_DOOR:
                    case WOOD_DOOR:
                    case TRAP_DOOR:
                        double damageModifier = SkillConfigManager.getUseSetting(defenderHero, skill, "reflected-damage-modifier", Double.valueOf(0.8), false);
                        double damage = event.getDamage() * damageModifier;
                        LivingEntity target = event.getDamager().getEntity();

                        Skill skill = event.getSkill();

                        addSpellTarget(target, defenderHero);

                        if (skill.isType(SkillType.ABILITY_PROPERTY_PHYSICAL) && !skill.isType(SkillType.ARMOR_PIERCING))
                            damageEntity(target, defenderPlayer, damage, DamageCause.ENTITY_ATTACK);
                        else
                            damageEntity(target, defenderPlayer, damage, DamageCause.MAGIC);

                    default:
                        return;
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (!(event.getEntity() instanceof Player))
                return;

            Hero defenderHero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (defenderHero.hasEffect("ShieldReflect")) {
                CharacterTemplate attackerCT = event.getDamager();
                if ((attackerCT instanceof Player)) {
                    Player attackerPlayer = (Player) attackerCT;
                    if (plugin.getCharacterManager().getHero(attackerPlayer).hasEffect(getName())) {
                        event.setCancelled(true);
                        return;
                    }
                }

                Player defenderPlayer = defenderHero.getPlayer();
                switch (defenderPlayer.getItemInHand().getType()) {
                    case IRON_DOOR:
                    case WOOD_DOOR:
                        double damageModifier = SkillConfigManager.getUseSetting(defenderHero, skill, "reflected-damage-modifier", Double.valueOf(0.8), false);
                        double damage = event.getDamage() * damageModifier;
                        LivingEntity target = event.getDamager().getEntity();
                        addSpellTarget(target, defenderHero);
                        damageEntity(target, defenderPlayer, damage, DamageCause.ENTITY_ATTACK);
                    default:
                        return;
                }
            }
        }
    }

    public class ShieldReflectEffect extends ExpirableEffect {
        public ShieldReflectEffect(Skill skill, Player applier, long duration) {
            super(skill, "ShieldReflect", applier, duration, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
        }
    }
}
