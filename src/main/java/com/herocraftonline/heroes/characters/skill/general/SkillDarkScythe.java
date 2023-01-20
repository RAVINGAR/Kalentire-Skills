package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
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

import java.util.List;

public class SkillDarkScythe extends ActiveSkill implements Listenable {

    private final Listener listener;
    private String applyText;
    private String expireText;

    public SkillDarkScythe(final Heroes plugin) {
        super(plugin, "Darkscythe");
        setDescription("Exhumes suffering from your blade for $1 second(s). While active, your attacks cause " +
                "great pain to your enemies, dealing an extra $2 damage.");
        setUsage("/skill darkscythe");
        setArgumentRange(0, 0);
        setIdentifiers("skill darkscythe");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.BUFFING);

        listener = new SkillDamageListener(this);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.DURATION, false);
        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000.0))
                .replace("$2", Util.decFormat.format(damage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set("weapons", Util.axes);
        config.set(SkillSetting.DURATION.node(), 10000);
        config.set(SkillSetting.DAMAGE.node(), 5);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), (double) 2);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has coated his weapons with suffering.");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s weapons are no longer enhanced.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% has coated his weapons with suffering.").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero%'s weapons are no longer enhanced.").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final int duration = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.DURATION, false);
        hero.addEffect(new DarkscytheEffect(this, hero.getPlayer(), duration));

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public Listener getListener() {
        return listener;
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

            // Check for both isArrow shots and left click attacks. Determine player based on which we're dealing with
            boolean isArrow = false;
            final Player player;
            final Entity damagingEntity = ((EntityDamageByEntityEvent) event).getDamager();
            if (damagingEntity instanceof Arrow) {
                if (!(((Projectile) damagingEntity).getShooter() instanceof Player)) {
                    return;
                }

                player = (Player) ((Projectile) damagingEntity).getShooter();
                isArrow = true;
            } else {
                if (event.getCause() != DamageCause.ENTITY_ATTACK) {
                    return;
                }

                final LivingEntity target = (LivingEntity) event.getEntity();
                if (plugin.getDamageManager().isSpellTarget(target) || !(subEvent.getDamager() instanceof Player)) {
                    return;
                }

                player = (Player) subEvent.getDamager();
            }

            final Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.hasEffect("Darkscythe")) {
                return;
            }

            final LivingEntity target = (LivingEntity) event.getEntity();

            final ItemStack item = NMSHandler.getInterface().getItemInMainHand(player.getInventory());
            final List<String> allowedWeapons = SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.axes);
            if (allowedWeapons.contains(item.getType().name()) || isArrow) {
                dealDarkscytheDamage(hero, target);
            }
        }

        private void dealDarkscytheDamage(final Hero hero, final LivingEntity target) {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (!damageCheck(hero.getPlayer(), target)) {
                    return;
                }

                final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.DAMAGE, false);

                // Damage the target
                addSpellTarget(target, hero);
                damageEntity(target, hero.getPlayer(), damage, DamageCause.MAGIC, false);
                //hero.getPlayer().getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.PORTAL, 0, 0, 0.0F, 0.0F, 0.0F, 0.5F, 45, 16);
                hero.getPlayer().getWorld().spawnParticle(Particle.PORTAL, target.getLocation().add(0, 0.5, 0), 45, 0, 0, 0, 0.5);
            }, 2L);
        }
    }

    public class DarkscytheEffect extends ExpirableEffect {

        public DarkscytheEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, "Darkscythe", applier, duration, applyText, expireText);

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
