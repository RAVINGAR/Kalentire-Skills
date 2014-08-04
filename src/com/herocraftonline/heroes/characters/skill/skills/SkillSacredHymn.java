package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

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

    @Override
    protected void doVisualEffects(World world, LivingEntity target) {
        // This is for Firework Effects
        VisualEffect fplayer = new VisualEffect();

        // this is our fireworks shit
        try {
            fplayer.playFirework(world, target.getLocation().add(0, 1.5, 0),
                    FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.BURST).withColor(Color.MAROON).withFade(Color.WHITE).build());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
