package com.herocraftonline.heroes.characters.skill.reborn.ninja;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillDeathMark extends ActiveSkill {

    public SkillDeathMark(Heroes plugin) {
        super(plugin, "DeathMark");
        setDescription("Mark a target for death for $1 second(s). " +
                "While marked, your compass will point directly to their location, and they will take $2% extra damage from your attacks.");
        setUsage("/skill deathmark <player>");
        setIdentifiers("skill deathmark");
        setArgumentRange(1, 1);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, "damage-increase-percent", 0.1, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDamageIncrease = Util.decFormat.format(damageIncrease * 100);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedDamageIncrease);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 60000);
        config.set(SkillSetting.PERIOD.node(), 5000);
        config.set("damage-increase-percent", 0.1);
        config.set("target-min-combat-level", 10);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        if (args.length < this.getMinArguments() || args.length > this.getMaxArguments()) {
            return SkillResult.INVALID_TARGET;
        }

        Player player = hero.getPlayer();

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null)
            return SkillResult.INVALID_TARGET;
        Hero targetHero = plugin.getCharacterManager().getHero(target);
        int minTargetHeroLevel = SkillConfigManager.getUseSetting(hero, this, "target-min-combat-level", 10, false);
        if (targetHero.getTieredLevel(targetHero.getHeroClass()) < minTargetHeroLevel) {
            player.sendMessage(target.getName() + " isn't powerful enough to be found...");
            return SkillResult.NORMAL;
        }
        if (!target.getWorld().equals(player.getWorld())) {
            player.sendMessage(target.getName() + " is not in this world...");
            return SkillResult.NORMAL;
        }

        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 5000, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, "damage-increase-percent", 0.1, false);
        hero.addEffect(new DeathMarkingEffect(this, player, period, duration, target, damageIncrease));

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 5.0F);

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        public SkillHeroListener() {}

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.getDamage() == 0)
                return;

            // Handle outgoing
            if (event.getDamager() instanceof Hero && event.getEntity() instanceof Player) {
                Hero hero = (Hero) event.getDamager();

                if (hero.hasEffect("DeathMarking")) {
                    DeathMarkingEffect dmEffect = (DeathMarkingEffect) hero.getEffect("DeathMarking");

                    if (dmEffect.getTarget().equals(event.getEntity())) {
                        double damageIncreasePercent = dmEffect.getDamageIncreasePercent();
                        event.setDamage(event.getDamage() * (1 + damageIncreasePercent));
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getDamage() == 0)
                return;

            // Handle outgoing
            if (event.getDamager() instanceof Hero && event.getEntity() instanceof Player) {
                Hero hero = (Hero) event.getDamager();

                if (hero.hasEffect("DeathMarking")) {
                    DeathMarkingEffect dmEffect = (DeathMarkingEffect) hero.getEffect("DeathMarking");

                    if (dmEffect.getTarget().equals(event.getEntity())) {
                        double damageIncreasePercent = dmEffect.getDamageIncreasePercent();
                        event.setDamage(event.getDamage() * (1 + damageIncreasePercent));
                    }
                }
            }
        }
    }

    public class DeathMarkingEffect extends PeriodicExpirableEffect {
        private Player target;
        private double damageIncreasePercent;

        public DeathMarkingEffect(Skill skill, Player applier, long period, long duration, Player target, double damageIncreasePercent) {
            super(skill, "DeathMarking", applier, period, duration, null, null);

            types.add(EffectType.PHYSICAL);

            this.target = target;
            this.damageIncreasePercent = damageIncreasePercent;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            player.setCompassTarget(target.getLocation());

            player.sendMessage(ChatColor.WHITE + target.getName() + ChatColor.GRAY + " has been marked for death!");
            double distance = player.getLocation().distance(target.getLocation());

            if (distance > 1000)
                player.sendMessage("You sense your target is a good distance away...");
            else
                player.sendMessage("You sense your target is close by...");

            //            if (distance > 1500)
            //                player.sendMessage("You sense your target is a great distance away...");
            //            else if (distance > 1000)
            //                player.sendMessage("You sense your target is a good distance away...");
            //            else if (distance > 500)
            //                player.sendMessage("You sense your target is close by...");
            //            else
            //                player.sendMessage("Your target is very close!");
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();

            player.sendMessage(target.getName() + " is no longer marked for death.");
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();

            player.setCompassTarget(target.getLocation());
        }

        @Override
        public void tickMonster(Monster monster) {}

        public Player getTarget() {
            return target;
        }

        public void setTarget(Player target) {
            this.target = target;
        }

        public double getDamageIncreasePercent() {
            return damageIncreasePercent;
        }

        public void setDamageIncreasePercent(double damageIncreasePercent) {
            this.damageIncreasePercent = damageIncreasePercent;
        }
    }
}
