package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseGroundEffect;
import com.herocraftonline.heroes.libs.slikey.effectlib.EffectType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SkillTrap extends SkillBaseGroundEffect {

    public SkillTrap(final Heroes plugin) {
        super(plugin, "Trap");
        setDescription("You set a trap underneath that lasts for $1s. The first player who sets of the trap will be rooted for $2s");
        setUsage("/skill trap");
        setIdentifiers("skill trap");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.MULTI_GRESSIVE, SkillType.AREA_OF_EFFECT, SkillType.NO_SELF_TARGETTING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final long warmUp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY, 3000, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);
        return getDescription()
                .replace("$1", Util.decFormat.format((double) duration / 1000));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.RADIUS.node(), 3d);
        node.set(HEIGHT_NODE, 2d);
        node.set(SkillSetting.DELAY.node(), 5000);
        node.set("root-duration", 2000);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.PERIOD.node(), 500);
        return node;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] strings) {
        final Player player = hero.getPlayer();
        final Location playerLoc = player.getLocation();

        // place on ground only
        final Material belowBlockType = playerLoc.getBlock().getRelative(BlockFace.DOWN).getType();
        if (!belowBlockType.isSolid()) {
            player.sendMessage("You must be standing on something hard to place the trap");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);

        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3d, false);
        final double height = SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 2d, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 500, false);
        final long rootDuration = SkillConfigManager.getUseSetting(hero, this, "root-duration", 2000, false);
        applyAreaGroundEffectEffect(hero, period, duration, player.getLocation(), radius, height, new GroundEffectActions() {

            @Override
            public void groundEffectTickAction(final Hero hero, final AreaGroundEffectEffect effect) {
                final com.herocraftonline.heroes.libs.slikey.effectlib.Effect e = new com.herocraftonline.heroes.libs.slikey.effectlib.Effect(effectLib) {
                    final int particlesPerRadius = 3;
                    final Particle particle = Particle.SMOKE_LARGE;

                    @Override
                    public void onRun() {
                        final double inc = 1 / (particlesPerRadius * radius);

                        for (double angle = 0; angle <= 2 * Math.PI; angle += inc) {
                            final Vector v = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(radius);
                            display(particle, getLocation().add(v));
                            getLocation().subtract(v);
                        }
                    }
                };

                final Location location = effect.getLocation().clone();
                e.setLocation(location);
                e.asynchronous = true;
                e.iterations = 1;
                e.type = EffectType.INSTANT;
                e.color = Color.WHITE;

                effectLib.start(e);
            }

            @Override
            public void groundEffectTargetAction(final Hero hero, final LivingEntity target, final AreaGroundEffectEffect groundEffect) {
                final Player player = hero.getPlayer();
                if (!damageCheck(player, target)) {
                    return;
                }

                final SkillTrap skill = SkillTrap.this;
                final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                final RootEffect effect = new RootEffect(skill, player, 100, rootDuration);
                targetCT.addEffect(effect);

                final Location targetLocation = target.getLocation();
                targetLocation.getWorld().playSound(targetLocation, Sound.BLOCK_WOODEN_PRESSURE_PLATE_CLICK_ON, 0.8F, 0.5F);
                targetLocation.getWorld().playSound(targetLocation, Sound.BLOCK_WOODEN_PRESSURE_PLATE_CLICK_OFF, 0.8F, 0.5F);
                hero.removeEffect(hero.getEffect(skill.getName()));
            }
        });
        return SkillResult.NORMAL;
    }
}
