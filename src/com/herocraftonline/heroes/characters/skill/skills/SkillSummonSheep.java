package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.DyeColor;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;

import java.util.HashSet;

public class SkillSummonSheep extends ActiveSkill {

    public SkillSummonSheep(Heroes plugin) {
        super(plugin, "SummonSheep");
        setDescription("100% chance to spawn 1 sheep, $2% for 2, and $3% for 3.");
        setUsage("/skill sheep");
        setArgumentRange(0, 0);
        setIdentifiers("skill summonsheep", "skill sheep");
        setTypes(SkillType.SUMMONING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int chance2x = (int) (SkillConfigManager.getUseSetting(hero, this, "chance-2x", 0.2, false) * 100 + SkillConfigManager.getUseSetting(hero, this, "chance-2x-per-level", 0.0, false) * hero.getLevel());
        int chance3x = (int) (SkillConfigManager.getUseSetting(hero, this, "chance-3x", 0.1, false) * 100 + SkillConfigManager.getUseSetting(hero, this, "chance-3x-per-level", 0.0, false) * hero.getLevel());
        return getDescription().replace("$2", chance2x + "").replace("$3", chance3x + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("chance-2x", 0.2);
        node.set("chance-3x", 0.1);
        node.set(SkillSetting.MAX_DISTANCE.node(), 20);
        node.set("chance-2x-per-level", 0.0);
        node.set("chance-3x-per-level", 0.0);
        return node;
    }

    @SuppressWarnings("deprecation")
	@Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        double chance2x = SkillConfigManager.getUseSetting(hero, this, "chance-2x", 0.2, false) + (int) SkillConfigManager.getUseSetting(hero, this, "chance-2x-per-level", 0.0, false) * hero.getSkillLevel(this);
        double chance3x = SkillConfigManager.getUseSetting(hero, this, "chance-3x", 0.1, false) + (int) SkillConfigManager.getUseSetting(hero, this, "chance-3x-per-level", 0.0, false) * hero.getSkillLevel(this);
        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 20, false);
        Block wTargetBlock = player.getTargetBlock((HashSet<Byte>)null, distance).getRelative(BlockFace.UP);
        Sheep sheep = (Sheep) player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.SHEEP);
        sheep.setColor(DyeColor.getByData((byte) Util.nextInt(DyeColor.values().length)));
        double chance = Util.nextRand();
        if (chance <= chance3x) {
            sheep = (Sheep) player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.SHEEP);
            sheep.setColor(DyeColor.getByData((byte) Util.nextInt(DyeColor.values().length)));
            sheep = (Sheep) player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.SHEEP);
            sheep.setColor(DyeColor.getByData((byte) Util.nextInt(DyeColor.values().length)));
        } else if (chance <= chance2x) {
            sheep = (Sheep) player.getWorld().spawnEntity(wTargetBlock.getLocation(), EntityType.SHEEP);
            sheep.setColor(DyeColor.getByData((byte) Util.nextInt(DyeColor.values().length)));
        }
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_SHEEP_AMBIENT , 0.8F, 1.0F); 
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }


}
