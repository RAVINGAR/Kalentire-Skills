package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

public class SkillRuneshard extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillRuneshard(final Heroes plugin) {
        super(plugin, "Runeshard");
        setDescription("Bestow magical shards to your melee weapon for $1 second(s). While active, your attacks deal an extra $2 damage to the target.");
        setUsage("/skill runeshard");
        setArgumentRange(0, 0);
        setIdentifiers("skill runeshard");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.BUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 5, false);
        final double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 2.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        final String formattedDamage = Util.decFormat.format(damage);
        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set("weapons", Util.swords);
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.DAMAGE.node(), 5);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), (double) 2);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has imbues his weapons with rune shards.");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s shards fall off their weapon.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has imbues his weapons with rune shards").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s shards fall off their weapon.").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        hero.addEffect(new RuneshardEffect(this, hero.getPlayer(), duration));

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public class SkillDamageListener implements Listener {
        private final Skill skill;

        public SkillDamageListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(final EntityDamageEvent event) {
            if ((!(event instanceof EntityDamageByEntityEvent)) || (!(event.getEntity() instanceof LivingEntity))) {
                return;
            }

            final EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;

            // Check for both arrow shots and left click attacks. Determine player based on which we're dealing with
            boolean arrow = false;
            final Player player;
            final Entity damagingEntity = ((EntityDamageByEntityEvent) event).getDamager();
            if (damagingEntity instanceof Arrow) {
                if (!(((Projectile) damagingEntity).getShooter() instanceof Player)) {
                    return;
                }

                player = (Player) ((Projectile) damagingEntity).getShooter();
                arrow = true;
            } else {
                if (event.getCause() != DamageCause.ENTITY_ATTACK) {
                    return;
                }

                final LivingEntity target = (LivingEntity) event.getEntity();
                if (!(plugin.getDamageManager().isSpellTarget(target))) {
                    if (!(subEvent.getDamager() instanceof Player)) {
                        return;
                    }

                    player = (Player) subEvent.getDamager();
                } else {
                    return;
                }
            }

            final Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.hasEffect("Runeshard")) {
                return;
            }

            final LivingEntity target = (LivingEntity) event.getEntity();

            final ItemStack item = NMSHandler.getInterface().getItemInMainHand(player.getInventory());
            if (!SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.swords).contains(item.getType().name())) {
                if (arrow) {
                    dealRuneshardDamage(hero, target);
                }
            } else {
                dealRuneshardDamage(hero, target);
            }

        }

        private void dealRuneshardDamage(final Hero hero, final LivingEntity target) {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (!(damageCheck(hero.getPlayer(), target))) {
                    return;
                }

                double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 5, false);
                final double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 2.0, false);
                damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

                // Damage the target
                addSpellTarget(target, hero);
                damageEntity(target, hero.getPlayer(), damage, DamageCause.MAGIC, false);
            }, 2L);
        }
    }

    public class RuneshardEffect extends ExpirableEffect {

        public RuneshardEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, "Runeshard", applier, duration, applyText, expireText);

            types.add(EffectType.IMBUE);
            types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            for (final Effect effect : hero.getEffects()) {
                if (effect.equals(this)) {
                    continue;
                }

                if (effect.isType(EffectType.IMBUE)) {
                    hero.removeEffect(effect);
                }
            }
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
        }
    }
}
