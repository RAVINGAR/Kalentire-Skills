package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseHeal;
import com.herocraftonline.heroes.util.CompatSound;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

public class SkillPray extends SkillBaseHeal {

    public SkillPray(Heroes plugin) {
        super(plugin, "Pray");
        setDescription("You pray for renewal, healing your target (within 8 blocks) for $1 health. If healing yourself, only $2 health is restored.");
        setUsage("/skill pray");
        setArgumentRange(0, 1);
        setIdentifiers("skill pray");
        setTypes(SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.HEALING.node(), 10);
        node.set(SkillSetting.MAX_DISTANCE.node(), 25);
        return node;
    }

    @Override
    protected void removeEffects(Hero hero) {
        // No effects are removed by this Skill.
    }

    protected void applySoundEffects(World world, LivingEntity target) {
        world.playSound(target.getLocation(), CompatSound.ENTITY_EXPERIENCE_ORB_PICKUP.value(), 0.5f, 1.0f);
    }

    protected void applyParticleEffects(World world, LivingEntity target) {
        world.spawnParticle(Particle.VILLAGER_HAPPY, // particle
                target.getLocation().add(0, 0.5, 0), // location
                25, // particle count
                1, 1, 1, // offset
                1, // extra (typically speed)
                null // data
                 );
        // SpawnParticle does not use an 'id' value like PlayEffect.
    }
}
