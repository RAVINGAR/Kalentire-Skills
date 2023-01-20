package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;


public class SkillHealingSpring extends ActiveSkill {
    public SkillHealingSpring(final Heroes plugin) {
        super(plugin, "HealingSpring");
        setDescription("A Healing Spring wells up beneath you, restoring $1 health to your party (within $4 blocks) every $2 second(s) for $3 second(s).");
        setUsage("/skill healingspring");
        setArgumentRange(0, 0);
        setIdentifiers("skill healingspring", "skill spring");
        setTypes(SkillType.BUFFING, SkillType.HEALING);
    }

    @Override
	public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.RADIUS.node(), 12);
        node.set(SkillSetting.HEALING.node(), 13);
        node.set(SkillSetting.DURATION.node(), 16000);
        node.set(SkillSetting.PERIOD.node(), 2000);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.07);
        return node;
    }

    @Override
	public String getDescription(final Hero hero) {
        double healthRestored = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 15, true);
        healthRestored += SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.07, true) *
                hero.getAttributeValue(AttributeType.WISDOM);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 16000, false);
        final long radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 12, false);

        return getDescription().replace("$1", healthRestored + "")
                .replace("$2", String.valueOf(period / 1000))
                .replace("$3", String.valueOf(duration / 1000))
                .replace("$4", radius + "");
    }

    @Override
	public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 16000, false);
        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 12, false);

        double healthRestoreTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 15, true);
        healthRestoreTick += SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.07, true) *
                hero.getAttributeValue(AttributeType.WISDOM);

        if (hero.hasParty()) {
            final int radiusSquared = radius * radius;
            final Location playerLocation = player.getLocation();
            for (final Hero partyMember : hero.getParty().getMembers()) {
                final Location memberLocation = partyMember.getPlayer().getLocation();
                if (memberLocation.getWorld().equals(playerLocation.getWorld())) {
					if (memberLocation.distanceSquared(playerLocation) <= radiusSquared) {
						partyMember.addEffect(new HealingSpringEffect(this, period, duration, healthRestoreTick, player));
					}
				}
            }
        } else {
			hero.addEffect(new HealingSpringEffect(this, period, duration, healthRestoreTick, player));
		}

        broadcastExecuteText(hero);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WATER_AMBIENT, 2.0F, 1.0F);

        return SkillResult.NORMAL;
    }

    public static class HealingSpringEffect extends PeriodicHealEffect {

        public HealingSpringEffect(final Skill skill, final int period, final int duration, final double healPerTick, final Player applier) {
            super(skill, "HealingSpring", applier, period, duration, healPerTick, ChatColor.WHITE + "$1" + ChatColor.GRAY + "'s healing sprint soothes your injuries", ChatColor.GRAY + "The healing spring has dried up!");
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
        }

        @Override
        public void tick(final CharacterTemplate character) {
            super.tick(character);
            final Location location = character.getEntity().getLocation().add(0, 0.5, 0);
            final World world = location.getWorld();
            world.spawnParticle(Particle.COMPOSTER, location, 25, 1.6, 0.3, 1.6, 0);
            world.spawnParticle(Particle.WATER_SPLASH, location, 25, 0.3, 3, 0.3, 0);
            world.spawnParticle(Particle.COMPOSTER, location, 25, 0.7, 1, 0.7, 0);
            world.spawnParticle(Particle.WATER_SPLASH, location, 25, 1.4, 1, 1.4, 0);

            world.playSound(location, Sound.BLOCK_WATER_AMBIENT, 0.7F, 1.0F);
        }
    }
}
