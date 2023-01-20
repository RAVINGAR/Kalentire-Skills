package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillBloodRage extends ActiveSkill implements Listenable {
    private final Listener listener;
    private String applyText, expireText;

    public SkillBloodRage(final Heroes plugin) {
        super(plugin, "BloodRage");
        setDescription("Increases your attack power by $1 for $2 seconds while rendering you incapable of using skills. While active, you heal for $3 health on every left click.");
        setUsage("/skill bloodrage");
        setArgumentRange(0, 0);
        setIdentifiers("skill bloodrage", "skill rage");
        setTypes(SkillType.BUFFING, SkillType.SILENCEABLE);
        listener = new SkillDamageListener(this);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set("damage-increase", 5);
        node.set("damage-increase-per-strength", 0.04);
        node.set("healing-per-hit", 5);
        node.set("healing-per-hit-increase", 0.04);
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set("max-heal", 100);
        node.set(SkillSetting.APPLY_TEXT.node(), " %hero% is empowered by blood rage!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), " %hero% has calmed down.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, " %hero% is empowered by blood rage!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, " %hero% has calmed down.").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public String getDescription(final Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, "damage-increase", 7.0D, true);
        damage += SkillConfigManager.getUseSetting(hero, this, "damage-increase-per-strength", 0.16, true) * hero.getAttributeValue(AttributeType.STRENGTH);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, true);
        final String formattedDuration = String.valueOf(duration / 1000);
        double healing = SkillConfigManager.getUseSetting(hero, this, "healing-per-hit", 5, true);
        healing += SkillConfigManager.getUseSetting(hero, this, "healing-per-hit-increase", 0.04, true) * hero.getAttributeValue(AttributeType.STRENGTH);
        return getDescription().replace("$1", damage + "").replace("$2", formattedDuration).replace("$3", healing + "");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        // fuck it
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, " %hero% is empowered by blood rage!").replace("%hero%", hero.getName());
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, " %hero% has calmed down.").replace("%hero%", hero.getName());
        if (hero.hasEffect("Bloodstorm")) {
            hero.getPlayer().sendMessage("This skill may not be used when Bloodstorm is applied!");
            return SkillResult.FAIL;
        }
        final SilenceEffect silence = new SilenceEffect(this, player, duration);
        hero.addEffect(silence);

        hero.addEffect(new BloodRageEffect(plugin, this, hero.getPlayer(), duration));

        //player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.3, 0), Effect.COLOURED_DUST, 0, 0, 1.2F, 1.6F, 1.2F, 0.0F, 200, 16);
        player.getWorld().spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 0.3, 0), 200, 1.2, 1.6, 1.2, new Particle.DustOptions(Color.RED, 1));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.0F, 0.5F);

        new BukkitRunnable() {
            private final int maxTicks = 40; // 4 times a second for 10 seconds
            private int ticks = 0;
            private boolean isNoise = false; // toggles back and forth to keep panting sounds reasonable

            @Override
            public void run() {
                final Location location = player.getLocation().add(0, 0.5, 0);
                //player.getWorld().spigot().playEffect(location, Effect.COLOURED_DUST, 0, 0, 0.3F, 0.5F, 0.3F, 0.0F, 50, 16);
                player.getWorld().spawnParticle(Particle.REDSTONE, location, 50, 0.3, 0.5, 0.3, 0, new Particle.DustOptions(Color.RED, 1));
                ticks++;

                if (isNoise) {
                    player.getWorld().playSound(location, Sound.ENTITY_WOLF_PANT, 1.3F, 0.8F); // Haven't you ever heard of a berserker? They pant.
                } else if (!isNoise) {
                    isNoise = true;
                }

                if (ticks == maxTicks) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1, 5);

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    public class SkillDamageListener implements Listener {
        private final Skill skill;
        double healed = 0;

        public SkillDamageListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler
        public void onDamage(final WeaponDamageEvent event) {
            final CharacterTemplate ct = event.getDamager();
            final LivingEntity ent = ct.getEntity();

            if (!(event.getEntity() instanceof LivingEntity)) {
                return;
            }
            final LivingEntity target = (LivingEntity) event.getEntity();

            if (!ct.hasEffect("BloodRage") || !(ct instanceof Hero)) {
                return;
            }
            final Hero h = (Hero) ct;
            extraDamage(h, target);
        }

        private void extraDamage(final Hero hero, final LivingEntity target) {
            double healing = SkillConfigManager.getUseSetting(hero, skill, "healing-per-hit", 5, true);
            healing += SkillConfigManager.getUseSetting(hero, skill, "healing-per-hit-increase", 0.04, true) * hero.getAttributeValue(AttributeType.STRENGTH);

            final double maxHeal = SkillConfigManager.getUseSetting(hero, skill, "max-heal", 100, false);
            if (healed <= maxHeal) {
                hero.heal(healing);
                hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.5F, 1.0F);
                healed += healing;
            }
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (!(damageCheck(hero.getPlayer(), target))) {
                    return;
                }

                double damage = SkillConfigManager.getUseSetting(hero, skill, "damage-increase", 7.0D, true);
                damage += SkillConfigManager.getUseSetting(hero, skill, "damage-increase-per-strength", 0.16, true) * hero.getAttributeValue(AttributeType.STRENGTH);

                addSpellTarget(target, hero);
                damageEntity(target, hero.getPlayer(), damage, DamageCause.MAGIC, false);

                //target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.CRIT, 0, 0, 0.3F, 0.5F, 0.3F, 0.5F, 25, 16);
                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 0.5, 0), 25, 0.3, 0.5, 0.3, 0.5);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 1, 1);
            }, 2L);
        }
    }

    public class BloodRageEffect extends ExpirableEffect {
        public BloodRageEffect(final Heroes plugin, final Skill skill, final Player applier, final long duration) {
            super(skill, plugin, "BloodRage", applier, duration);

            types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            broadcast(hero.getPlayer().getLocation(), applyText);
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            broadcast(hero.getPlayer().getLocation(), expireText);
            hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0F, 1.0F);
            //hero.getPlayer().getWorld().spigot().playEffect(hero.getPlayer().getLocation().add(0, 0.5, 0), Effect.LARGE_SMOKE, 0, 0, 0.4F, 0.2F, 0.4F, 0.3F, 45, 16);
            hero.getPlayer().getWorld().spawnParticle(Particle.SMOKE_LARGE, hero.getPlayer().getLocation().add(0, 0.5, 0), 45, 0.4, 0.2, 0.4, 0.3);
        }
    }

}