package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

public class SkillTest extends ActiveSkill {

    public SkillTest(Heroes plugin) {
        super(plugin, "Test");
        setDescription("I am become death");
        setUsage("/skill test");
        setArgumentRange(0, 0);
        setIdentifiers("skill test", "skill testes");
        setTypes(SkillType.SUMMONING, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MAX_DISTANCE.node(), 20);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 20, false);
        Block wTargetBlock = player.getTargetBlock(null, distance).getRelative(BlockFace.UP);
        player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.FALLING_BLOCK);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENDERMAN_STARE , 0.8F, 1.0F);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }


}
