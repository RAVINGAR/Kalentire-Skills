package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseBeam;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import static com.herocraftonline.heroes.characters.skill.SkillType.AREA_OF_EFFECT;
import static com.herocraftonline.heroes.characters.skill.SkillType.HEALING;
import static com.herocraftonline.heroes.characters.skill.SkillType.NO_SELF_TARGETTING;
import static com.herocraftonline.heroes.characters.skill.SkillType.SILENCEABLE;
import static com.herocraftonline.heroes.characters.skill.SkillType.UNINTERRUPTIBLE;

public class SkillOrdain extends SkillBaseBeam {

    private static final float PARTICLE_OFFSET_FROM_FACE = 1;
    private static final Particle BEAM_PARTICLE = Particle.VILLAGER_HAPPY;

    public SkillOrdain(final Heroes plugin) {
        super(plugin, "Ordain");
        setDescription("You summon the calling of order to project a beam that heals $1 health in its path.");
        setUsage("/skill ordain");
        setArgumentRange(0, 0);
        setIdentifiers("skill ordain");
        setTypes(HEALING, AREA_OF_EFFECT, NO_SELF_TARGETTING, UNINTERRUPTIBLE, SILENCEABLE);
    }

    @Override
    public String getDescription(final Hero hero) {
        double beamHeal = SkillConfigManager.getUseSetting(hero, SkillOrdain.this, SkillSetting.HEALING, 150d, false);
        final double beamHealIncrease = SkillConfigManager.getUseSetting(hero, SkillOrdain.this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 1d, false);
        beamHeal += hero.getAttributeValue(AttributeType.WISDOM) * beamHealIncrease;

        return getDescription()
                .replace("$1", Util.decFormat.format(beamHeal));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(BEAM_MAX_LENGTH_NODE, 15);
        node.set(BEAM_RADIUS_NODE, 2d);

        node.set(SkillSetting.HEALING.node(), 200);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 1d);

        return node;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] strings) {
        final Player player = hero.getPlayer();

        final int beamMaxLength = SkillConfigManager.getUseSetting(hero, this, BEAM_MAX_LENGTH_NODE, 15, false);
        final double beamRadius = SkillConfigManager.getUseSetting(hero, this, BEAM_RADIUS_NODE, 2d, false);
        final Beam beam = createObstructedBeam(player.getEyeLocation(), beamMaxLength, beamRadius);

        broadcastExecuteText(hero);

        castBeam(hero, beam, (hero1, target, pointData) -> {
            if (target instanceof Player) {
                final Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
                if (targetHero == hero1 || (hero1.hasParty() && hero1.getParty().isPartyMember(targetHero))) {
                    double beamHeal = SkillConfigManager.getUseSetting(hero1, SkillOrdain.this, SkillSetting.HEALING, 150d, false);
                    final double beamHealIncrease = SkillConfigManager.getUseSetting(hero1, SkillOrdain.this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 1d, false);
                    beamHeal += hero1.getAttributeValue(AttributeType.WISDOM) * beamHealIncrease;

                    targetHero.heal(beamHeal);
                }
            }
        });

        renderEyeBeam(player, beam, BEAM_PARTICLE, 60, 10, 40, 0.125, 1);

        new BukkitRunnable() {

            private float volume = 0.25f;

            @Override
            public void run() {
                if ((volume -= 0.025f) <= 0) {
                    cancel();
                } else {
                    player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, volume, 1);
                    player.getWorld().playSound(player.getEyeLocation().add(beam.getTrajectory()), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, volume, 1);
                    player.getWorld().playSound(beam.midPoint().toLocation(player.getWorld()), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, volume, 1);
                }
            }
        }.runTaskTimer(plugin, 0, 1);

        return SkillResult.NORMAL;
    }
}
