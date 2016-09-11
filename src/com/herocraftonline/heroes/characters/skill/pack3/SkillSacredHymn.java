package com.herocraftonline.heroes.characters.skill.pack3;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.skills.*;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.base.SkillBaseHeal;

public class SkillSacredHymn extends SkillBaseHeal {

    public SkillSacredHymn(Heroes plugin) {
        super(plugin, "SacredHymn");
        setDescription("Bless your target with a Sacred Hymn, restoring $1 health to your target. You are only healed for $2 health from this ability.");
        setUsage("/skill sacredhymn <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill sacredhymn");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.DISPELLING, SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.HEALING.node(), 150);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 3.75);

        return node;
    }

    @Override
    protected void removeEffects(Hero hero) {
        
    }
    
    protected void applySoundEffects(World world, LivingEntity target) {
        world.playSound(target.getLocation(), CompatSound.BLOCK_NOTE_PLING.value(), 0.5f, 1.0f);
    }

    protected void applyParticleEffects(World world, LivingEntity target) {
        world.spigot().playEffect(target.getLocation().add(0, 0.5, 0), // location
                org.bukkit.Effect.NOTE, // effect
                0, // id
                0, // data
                1, 1, 1, // offset
                0.0f, // speed
                25, // particle count
                1); // radius
    }
}
