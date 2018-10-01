package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.EffectAddEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class SkillWraithForm extends ActiveSkill {
    static final String SKILL_NAME = "WraithForm";
    static final String EFFECT_NAME = "WraithForm";

    public SkillWraithForm(Heroes plugin) {
        super(plugin, SKILL_NAME);
        setDescription("Dissolute your physical form, becoming a shadowy wraith for $1 second(s). You become " +
        "untargetable and invulnerable, but cannot use skills or deal damage. Removes CC effects on cast and grants" +
                " immunity to new CC effects while in effect.");
        setUsage("/skill wraithform");
        setIdentifiers("skill wraithform");
        setArgumentRange(0, 0);
        Bukkit.getPluginManager().registerEvents(new WraithFormListener(), plugin);
    }

    public String getDescription(Hero hero) {
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, true);
        String formatted = Util.decFormat.format(duration / 1000);
        return getDescription().replace("$1", formatted);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 4000);

        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        if (hero.hasEffect(EFFECT_NAME)) {
            hero.removeEffect(hero.getEffect(EFFECT_NAME));
            return SkillResult.NORMAL;
        } else {
            long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, true);
            Set<com.herocraftonline.heroes.characters.effects.Effect> toRemove = new HashSet<>();
            for (com.herocraftonline.heroes.characters.effects.Effect effect : hero.getEffects()) {
                if (effect.types.contains(EffectType.ROOT) || effect.types.contains(EffectType.SLOW)
                        || effect.types.contains(EffectType.STUN)) toRemove.add(effect);
            }
            hero.getEffects().removeAll(toRemove);
            hero.addEffect(new WraithFormEffect(this, plugin, player, duration));
        }

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public class WraithFormEffect extends ExpirableEffect {
        private Skill skill;

        public WraithFormEffect(Skill skill, Heroes plugin, Player applier, long duration) {
            super(skill, plugin, EFFECT_NAME, applier, duration);
            setTypes(SkillType.SILENCING);
            this.skill = skill;
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            for (Player p : Bukkit.getServer().getOnlinePlayers())
                p.hidePlayer(player);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, 1.0f, 0.5f);
//            player.getWorld().spigot().playEffect(player.getLocation().add(0, 1, 0), Effect.LARGE_SMOKE, 0, 0,
//                    0.3f, 1.0f, 0.3f, 0.2f, 55, 128);
            player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 55, 0.3, 1, 0.3, 0.2, true);
            final Hero h = hero;
            new BukkitRunnable() {
                public void run() {
                    if (!h.hasEffect(EFFECT_NAME)) cancel();
//                    player.getWorld().spigot().playEffect(player.getLocation().add(0, 1, 0), Effect.LARGE_SMOKE, 0, 0,
//                            2.3f, 0.5f, 2.3f, 0.0f, 15, 128);
                    player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 15, 2.3, 0.5, 2.3, 0, true);
                    player.setFoodLevel(0);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10, 0, true, false));
                    // player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20, 0, true, false));
                }
            }.runTaskTimer(plugin, 0, 1);
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            for (Player p : Bukkit.getServer().getOnlinePlayers())
                p.showPlayer(player);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.5f);
//            player.getWorld().spigot().playEffect(player.getLocation().add(0, 1, 0), Effect.LARGE_SMOKE, 0, 0,
//                    0.3f, 1.0f, 0.3f, 0.2f, 55, 128);
            player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1,0), 55, 0.3, 1, 0.3, 0.2, true);
        }
    }

    public class WraithFormListener implements Listener {
        @EventHandler
        public void cancelDamage(EntityDamageEvent e) {
            if (!(e.getEntity() instanceof Player)) return;
            Player player = (Player) e.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect(EFFECT_NAME)) e.setCancelled(true);
        }

        @EventHandler
        public void cancelOtherDamage(EntityDamageByEntityEvent e) {
            if (!(e.getDamager() instanceof Player)) return;
            Player player = (Player) e.getDamager();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect(EFFECT_NAME)) {
                e.setDamage(0.0d);
                e.setCancelled(true);
            }
        }


        /*@EventHandler
        public void cancelCCEffects(EffectAddEvent e) {
            if (!e.getCharacter().hasEffect(EFFECT_NAME)) return;
            com.herocraftonline.heroes.characters.effects.Effect effect = e.getEffect();
            if (effect.types.contains(EffectType.ROOT) || effect.types.contains(EffectType.SLOW)
                    || effect.types.contains(EffectType.STUN)) e.setCancelled(true);
        }*/
    }
}
