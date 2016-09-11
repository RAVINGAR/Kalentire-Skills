package com.herocraftonline.heroes.characters.skill.skills;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillTrack extends ActiveSkill {

    public SkillTrack(Heroes plugin) {
        super(plugin, "Track");
        setDescription("Track your target, and learn of their location in the world, specific up to $1 blocks. Your compass will point to the target for $2 seconds after tracking them.");
        setUsage("/skill track <player>");
        setArgumentRange(1, 1);
        setIdentifiers("skill track");
    }

    @Override
    public String getDescription(Hero hero) {
        int randomness = SkillConfigManager.getUseSetting(hero, this, "randomness", 50, true);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 30000, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", randomness + "").replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("randomness", 50);
        node.set("target-min-combat-level", 10);

        return node;
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
        int minTargetLevel = SkillConfigManager.getUseSetting(hero, this, "target-min-combat-level", 10, false);
        if (targetHero.getTieredLevel(targetHero.getHeroClass()) < minTargetLevel) {
            Messaging.send(player, "$1 isn't powerful enough to be found...", target.getName());
            return SkillResult.NORMAL;
        }
        if(!target.getWorld().equals(player.getWorld())) {
        	Messaging.send(player, "$1 is in world: $2", target.getName(), target.getWorld().getName());
        	return SkillResult.NORMAL;
        }

        broadcastExecuteText(hero);

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 30000, false);

        hero.addEffect(new TrackingEffect(this, player, period, duration, target));

        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_PLAYER_LEVELUP.value(), 0.8F, 5.0F);

        return SkillResult.NORMAL;
    }

    public class TrackingEffect extends PeriodicExpirableEffect {
        private Player target;

        public TrackingEffect(Skill skill, Player applier, long period, long duration, Player target) {
            super(skill, "Tracking", applier, period, duration, null, null);

            types.add(EffectType.PHYSICAL);

            this.target = target;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            Location location = target.getLocation();

            player.setCompassTarget(location);

            long time = System.currentTimeMillis();
            Random ranGen = new Random((int) ((time / 2.0) * 12));

            int randomness = SkillConfigManager.getUseSetting(hero, skill, "randomness", 50, true);

            int randomX = (int) (randomness * ranGen.nextGaussian());
            int randomY = (int) ((randomness * Math.abs(ranGen.nextGaussian())) / 10);
            int randomZ = (int) (randomness * ranGen.nextGaussian());

            int x = location.getBlockX() + randomX;
            int y = location.getBlockY() + randomY;
            int z = location.getBlockZ() + randomZ;

            Messaging.send(player, "Tracked $1: $2, $3, $4", target.getName(), x, y, z);
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();

            player.setCompassTarget(target.getLocation());
        }

        @Override
        public void tickMonster(Monster monster) {}
    }
}
