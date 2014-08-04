package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillBandage extends SkillBaseHeal {

    public SkillBandage(Heroes plugin) {
        super(plugin, "Bandage");
        setDescription("Bandage your target, restoring $1 of their health. You are only healed for $2 health from this ability. You cannot use this ability in combat.");
        setUsage("/skill bandage <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill bandage");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set(SkillSetting.HEALING.node(), 75);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 3.125);

        return node;
    }

    @Override
    protected void removeEffects(Hero hero) {

    }

    @Override
    protected void doVisualEffects(World world, LivingEntity target) {

    }
}
