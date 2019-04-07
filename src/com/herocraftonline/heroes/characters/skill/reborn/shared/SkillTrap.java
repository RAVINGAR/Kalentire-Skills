package com.herocraftonline.heroes.characters.skill.reborn.shared;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseGroundEffect;
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
        setDescription("You check for something every $2 for $3");
        setUsage("/skill trap");
        setIdentifiers("skill trap");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.MULTI_GRESSIVE, SkillType.AREA_OF_EFFECT, SkillType.NO_SELF_TARGETTING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 500, false);
        return getDescription()
                .replace("$2", Util.decFormat.format((double) period / 1000))
                .replace("$3", Util.decFormat.format((double) duration / 1000));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 3d);
        node.set(HEIGHT_NODE, 2d);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.PERIOD.node(), 500);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        final Player player = hero.getPlayer();
        Location playerLoc = player.getLocation();

        // place on ground only
        Material standingBlockType = playerLoc.getBlock().getType();
        Material belowBlockType = playerLoc.getBlock().getRelative(BlockFace.DOWN).getType();
        if (!belowBlockType.isSolid()) {
            player.sendMessage("You must be standing on something hard to place the trap");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);

        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3d, false);
        double height = SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 2d, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 500, false);
        applyAreaGroundEffectEffect(hero, period, duration, player.getLocation(), radius, height, new GroundEffectActions() {

            @Override
            public void groundEffectTickAction(Hero hero, AreaGroundEffectEffect effect) {
                EffectManager em = new EffectManager(plugin);
                Effect e = new Effect(em) {

                    int particlesPerRadius = 3;
                    Particle particle = Particle.REDSTONE;

                    @Override
                    public void onRun() {

                        double inc = 1 / (particlesPerRadius * radius);

                        for (double angle = 0; angle <= 2 * Math.PI; angle += inc) {
                            Vector v = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(radius);
                            display(particle, getLocation().add(v));
                            getLocation().subtract(v);
                        }

                        Location originalLocation = getLocation();
                        Color originalColor = color;
                        color = Color.BLUE;

                        int particles = (int) (2 * radius * particlesPerRadius);
                        Vector crossXLine = new Vector(-radius * 2, 0, 0).multiply(1d / particles);
                        Vector crossZLine = new Vector(0, 0, -radius * 2).multiply(1d / particles);

                        setLocation(new Vector(radius, 0, radius / 10).toLocation(getLocation().getWorld()).add(originalLocation));
                        for (int l = 0; l < particles; l++, getLocation().add(crossXLine)) {
                            display(particle, getLocation());
                        }

                        setLocation(new Vector(radius / 10, 0, radius).toLocation(getLocation().getWorld()).add(originalLocation));
                        for (int l = 0; l < particles; l++, getLocation().add(crossZLine)) {
                            display(particle, getLocation());
                        }

                        setLocation(new Vector(radius, 0, radius / -10).toLocation(getLocation().getWorld()).add(originalLocation));
                        for (int l = 0; l < particles; l++, getLocation().add(crossXLine)) {
                            display(particle, getLocation());
                        }

                        setLocation(new Vector(radius / -10, 0, radius).toLocation(getLocation().getWorld()).add(originalLocation));
                        for (int l = 0; l < particles; l++, getLocation().add(crossZLine)) {
                            display(particle, getLocation());
                        }

                        setLocation(originalLocation);
                        color = originalColor;
                    }
                };

                e.setLocation(effect.getLocation().clone());
                e.asynchronous = true;
                e.iterations = 1;
                e.type = EffectType.INSTANT;
                e.color = Color.WHITE;

                e.start();
                em.disposeOnTermination();

                player.getWorld().playSound(effect.getLocation(), Sound.ENTITY_GENERIC_BURN, 0.25f, 0.0001f);
            }

            @Override
            public void groundEffectTargetAction(Hero hero, final LivingEntity target, final AreaGroundEffectEffect groundEffect) {
                Player player = hero.getPlayer();
                if (!damageCheck(player, target))
                    return;

                SkillTrap skill = SkillTrap.this;
                final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                final RootEffect effect = new RootEffect(skill, player, 100, 5000);
                targetCT.addEffect(effect);

                if (skill.isAreaGroundEffectApplied(hero))
                    hero.removeEffect(hero.getEffect(skill.getName()));
            }
        });
        return SkillResult.NORMAL;
    }
}
