package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseBeamShot;
import org.bukkit.configuration.ConfigurationSection;

import static org.bukkit.util.NumberConversions.square;

public class SkillChaoticEnergy extends SkillBaseBeamShot {

    private static final String MIN_DAMAGE_NODE = "min-damage";
    private static final String MAX_DAMAGE_NODE = "max-damage";

    public SkillChaoticEnergy(Heroes plugin) {
        super(plugin, "ChaoticEnergy");
        setDescription("");
        setIdentifiers("skill chaoticenergy");
        setUsage("/skill chaoticenergy");
        setArgumentRange(0, 0);
        setTypes(SkillType.AGGRESSIVE, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        return null;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(MIN_DAMAGE_NODE, 100d);
        node.set(MAX_DAMAGE_NODE, 350d);
        node.set(SkillSetting.MAX_DISTANCE.node(), 20d);
        node.set(SkillSetting.RADIUS.node(), 0.5);
        node.set(VELOCITY_NODE, 0.5);
        node.set(PENETRATION_NODE, 0);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {

        final double minDamage = SkillConfigManager.getUseSetting(hero, this, MIN_DAMAGE_NODE, 100d, false);
        final double maxDamage = SkillConfigManager.getUseSetting(hero, this, MAX_DAMAGE_NODE, 350d, false);
        double maxDistance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 20d, false);
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 0.5, false);
        double velocity = SkillConfigManager.getUseSetting(hero, this, VELOCITY_NODE, 0.5, false);
        int penetration = SkillConfigManager.getUseSetting(hero, this, PENETRATION_NODE, 0, false);

        broadcastExecuteText(hero);

        final double maxDistanceSq = square(maxDistance);

        /*
        fireBeamShot(hero, maxDistance, radius, velocity, penetration, new BeamShotHit() {

            @Override
            public void onHit(Hero hero, LivingEntity target, Location origin, Capsule shot, int count, boolean first, boolean last) {
                AABB targetAABB = NMSHandler.getInterface().getNMSPhysics().getEntityAABB(target);
                //todo
            }

            @Override
            public void onRenderShot(Location origin, Capsule shot, int frame, boolean first, boolean last) {

            }
        }, new Predicate<Block>() {

            @Override
            public boolean apply(Block block) {
                return false;
            }
        }, EnumSet.of(RayCastFlag.BLOCK_IGNORE_NON_SOLID, RayCastFlag.BLOCK_HIGH_DETAIL));*/

        return SkillResult.NORMAL;
    }
}
