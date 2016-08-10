package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashSet;

public class SkillSummonPig extends ActiveSkill {

    public SkillSummonPig(Heroes plugin) {
        super(plugin, "SummonPig");
        this.setDescription("100% chance to spawn 1 pig, $1% for 2, and $2% for 3.");
        this.setUsage("/skill pig");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill summonpig", "skill pig");
        this.setTypes(SkillType.SUMMONING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_EARTH);
    }

    @Override
    public String getDescription(Hero hero) {
        final int chance2x = (int) ((SkillConfigManager.getUseSetting(hero, this, "chance-2x", 0.2, false) * 100) + (SkillConfigManager.getUseSetting(hero, this, "chance-2x-per-level", 0.0, false) * hero.getLevel()));
        final int chance3x = (int) ((SkillConfigManager.getUseSetting(hero, this, "chance-3x", 0.1, false) * 100) + (SkillConfigManager.getUseSetting(hero, this, "chance-3x-per-level", 0.0, false) * hero.getLevel()));
        return this.getDescription().replace("$2", chance2x + "").replace("$3", chance3x + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("chance-2x", 0.2);
        node.set("chance-3x", 0.1);
        node.set(SkillSetting.MAX_DISTANCE.node(), 20);
        node.set("chance-2x-per-level", 0.0);
        node.set("chance-3x-per-level", 0.0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();
        this.broadcastExecuteText(hero);
        final double chance2x = SkillConfigManager.getUseSetting(hero, this, "chance-2x", 0.2, false) + ((int) SkillConfigManager.getUseSetting(hero, this, "chance-2x-per-level", 0.0, false) * hero.getSkillLevel(this));
        final double chance3x = SkillConfigManager.getUseSetting(hero, this, "chance-3x", 0.1, false) + ((int) SkillConfigManager.getUseSetting(hero, this, "chance-3x-per-level", 0.0, false) * hero.getSkillLevel(this));
        final int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 20, false) + ((int) SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE, 0.0, false) * hero.getSkillLevel(this));
        final Block wTargetBlock = player.getTargetBlock((HashSet<Byte>) null, distance).getRelative(BlockFace.UP);
        player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.PIG);
        final double chance = Util.nextRand();
        if (chance <= chance3x) {
            player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.PIG);
            player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.PIG);
        } else if (chance <= chance2x) {
            player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.PIG);
        }
        this.broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }


}
