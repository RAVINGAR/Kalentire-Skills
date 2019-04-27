package com.herocraftonline.heroes.characters.skill.reborn.pathfinder;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseGroundEffect;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SkillTrap extends SkillBaseGroundEffect {

    public SkillTrap(Heroes plugin) {
        super(plugin, "Trap");
        setDescription("You set a trap underneath you that is $1 blocks wide and lasts for $2 second(s). " +
                "The first target who sets off the trap will be rooted for $3 second(s).");
        setUsage("/skill trap");
        setIdentifiers("skill trap");
        setArgumentRange(0, 0);
        setTypes(SkillType.AREA_OF_EFFECT);
    }

    @Override
    public String getDescription(Hero hero) {
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3.0, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);
        long rootDuration = SkillConfigManager.getUseSetting(hero, this, "root-duration", 2000, false);

        return getDescription()
                .replace("$2", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format(duration / 1000.0))
                .replace("$3", Util.decFormat.format(rootDuration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.RADIUS.node(), 3.0);
        config.set(HEIGHT_NODE, 2.0);
        config.set(SkillSetting.DELAY.node(), 5000);
        config.set("root-duration", 2000);
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set(SkillSetting.PERIOD.node(), 500);
        return config;
    }

    @Override public SkillResult use(Hero hero, String[] strings) {
        final Player player = hero.getPlayer();
        Location playerLoc = player.getLocation();

        // place on ground only
        Material belowBlockType = playerLoc.getBlock().getRelative(BlockFace.DOWN).getType();
        if (!belowBlockType.isSolid()) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You must be standing on something hard to place a trap.");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);

        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3.0, false);
        double height = SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 2d, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 500, false);
        long rootDuration = SkillConfigManager.getUseSetting(hero, this, "root-duration", 2000, false);
        applyAreaGroundEffectEffect(hero, period, duration, player.getLocation(), radius, height, new GroundEffectActions() {

            @Override
            public void groundEffectTickAction(Hero hero, AreaGroundEffectEffect effect) {
                EffectManager em = new EffectManager(plugin);
                Effect e = new Effect(em) {
                    int particlesPerRadius = 3;
                    Particle particle = Particle.SMOKE_LARGE;

                    @Override
                    public void onRun() {
                        double inc = 1 / (particlesPerRadius * radius);

                        for (double angle = 0; angle <= 2 * Math.PI; angle += inc) {
                            Vector v = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(radius);
                            display(particle, getLocation().add(v));
                            getLocation().subtract(v);
                        }
                    }
                };

                Location location = effect.getLocation().clone();
                e.setLocation(location);
                e.asynchronous = true;
                e.iterations = 1;
                e.type = EffectType.INSTANT;
                e.color = Color.WHITE;

                e.start();
                em.disposeOnTermination();
            }

            @Override
            public void groundEffectTargetAction(Hero hero, final LivingEntity target, final AreaGroundEffectEffect groundEffect) {
                Player player = hero.getPlayer();
                if (!damageCheck(player, target))
                    return;

                SkillTrap skill = SkillTrap.this;
                final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                final RootEffect effect = new RootEffect(skill, player, 100, rootDuration);
                targetCT.addEffect(effect);

                Location targetLocation = target.getLocation();
                targetLocation.getWorld().playSound(targetLocation, Sound.BLOCK_WOOD_PRESSUREPLATE_CLICK_ON, 0.8F, 0.5F);
                targetLocation.getWorld().playSound(targetLocation, Sound.BLOCK_WOOD_PRESSUREPLATE_CLICK_OFF, 0.8F, 0.5F);
                hero.removeEffect(hero.getEffect(skill.getName()));
            }
        });
        return SkillResult.NORMAL;
    }
}
