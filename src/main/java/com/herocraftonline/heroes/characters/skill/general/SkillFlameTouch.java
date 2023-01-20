package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillFlameTouch extends ActiveSkill {
    private String applyText, expireText;

    public SkillFlameTouch(final Heroes plugin) {
        super(plugin, "FlameTouch");
        setDescription("Imbues your strikes with fire for $1 second(s), causing you to deal $2 more damage and igniting your target for 2 second(s).");
        setUsage("/skill flametouch");
        setArgumentRange(0, 0);
        setIdentifiers("skill flametouch");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_FIRE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.1);
        config.set(SkillSetting.DAMAGE.node(), 5);
        config.set(SkillSetting.DURATION.node(), 10000);
        config.set(SkillSetting.APPLY_TEXT.node(), " %hero%'s strikes are imbued with flame!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), " %hero%'s strikes have returned to normal.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                " %hero%'s strikes are imbued with flame!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                " %hero%'s strikes have returned to normal.").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public String getDescription(final Hero hero) {
        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        final int duration = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.DURATION, false);
        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000))
                .replace("$2", damage + "");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                        ChatComponents.GENERIC_SKILL + " %hero%'s strikes are imbued with flame!")
                .replace("%hero%", ChatColor.WHITE + hero.getName() + ChatColor.GRAY);
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                        ChatComponents.GENERIC_SKILL + " %hero%'s strikes have returned to normal.")
                .replace("%hero%", ChatColor.WHITE + hero.getName() + ChatColor.GRAY);

        hero.addEffect(new FlameTouchEffect(plugin, this, hero.getPlayer(), duration));

        //player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.3, 0), Effect.FLAME, 0, 0, 1.2F, 1.6F, 1.2F, 0.5F, 100, 16);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 0.3, 0), 100, 1.2, 1.6, 1.2, 0.5);
        //player.getWorld().spigot().playEffect(player.getLocation(), Effect.LAVA_POP, 0, 0, 1.2F, 0.2F, 1.2F, 0.5F, 50, 16);
        player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 50, 1.2, 0.2, 1.2, 0.5);
        player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 2);

        new BukkitRunnable() {
            private final int maxTicks = 40; // 4 times a second for 10 seconds
            private int ticks = 0;
            private boolean isNoise = false; // toggles back and forth

            @Override
            public void run() {
                final Location location = player.getLocation().add(0, 0.5, 0);
                //p.getWorld().spigot().playEffect(location, Effect.FLAME, 0, 0, 0.3F, 0.5F, 0.3F, 0.0F, 25, 16);
                player.getWorld().spawnParticle(Particle.FLAME, location, 25, 0.3, 0.5, 0.3, 0);
                ticks++;

                if (isNoise) {
                    player.getWorld().playSound(location, Sound.BLOCK_FIRE_AMBIENT, 1.2F, 0.8F); // BURRRRN
                } else {
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

    public void registerListener(final Hero hero) {

    }

    public class SkillDamageListener implements Listener {
        private final Skill skill;

        public SkillDamageListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(final EntityDamageEvent event) {
            if (!(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            final EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;

            if (event.getCause() != DamageCause.ENTITY_ATTACK) {
                return;
            }

            final LivingEntity target = (LivingEntity) event.getEntity();
            final Player player;

            if (!(plugin.getDamageManager().isSpellTarget(target))) {
                if (!(subEvent.getDamager() instanceof Player)) {
                    return;
                }

                player = (Player) subEvent.getDamager();
            } else {
                return;
            }

            final Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.hasEffect("FlameTouch")) {
                return;
            }
            extraDamage(hero, target);
        }

        private void extraDamage(final Hero hero, final LivingEntity target) {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (!damageCheck(hero.getPlayer(), target)) {
                    return;
                }

                final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.DAMAGE, false);
                addSpellTarget(target, hero);
                damageEntity(target, hero.getPlayer(), damage, DamageCause.MAGIC, false);

                target.setFireTicks(40);

                //target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.FLAME, 0, 0, 0.3F, 0.5F, 0.3F, 0.5F, 25, 16);
                target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 0.5, 0), 25, 0.3, 0.5, 0.3, 0.5);
                target.getWorld().playEffect(target.getLocation(), Effect.BLAZE_SHOOT, 2);
            }, 2L);
        }
    }

    public class FlameTouchEffect extends ExpirableEffect {
        public FlameTouchEffect(final Heroes plugin, final Skill skill, final Player applier, final long duration) {
            super(skill, plugin, "FlameTouch", applier, duration);

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
            hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.3F, 0.8F);
            //hero.getPlayer().getWorld().spigot().playEffect(hero.getPlayer().getLocation().add(0, 0.5, 0), Effect.LARGE_SMOKE, 0, 0, 0.4F, 0.2F, 0.4F, 0.3F, 45, 16);
            hero.getPlayer().getWorld().spawnParticle(Particle.SMOKE_LARGE, hero.getPlayer().getLocation().add(0, 0.5, 0), 45, 0.4, 0.2, 0.4, 0.3);
        }
    }

}
