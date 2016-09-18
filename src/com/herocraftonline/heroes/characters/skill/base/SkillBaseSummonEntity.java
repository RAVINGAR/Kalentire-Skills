package com.herocraftonline.heroes.characters.skill.base;

import java.util.HashSet;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
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

public abstract class SkillBaseSummonEntity extends ActiveSkill {

    public SkillBaseSummonEntity(Heroes plugin, String name) {
        super(plugin, name);
        setDescription("100% chance to spawn 1 entity, $2% for 2, and $3% for 3.");
        setUsage("/skill entity");
        setArgumentRange(0, 0);
        setIdentifiers("skill summonentity", "skill entity");
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

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        double chance2x = SkillConfigManager.getUseSetting(hero, this, "chance-2x", 0.2, false) + (int) SkillConfigManager.getUseSetting(hero, this, "chance-2x-per-level", 0.0, false) * hero.getSkillLevel(this);
        double chance3x = SkillConfigManager.getUseSetting(hero, this, "chance-3x", 0.1, false) + (int) SkillConfigManager.getUseSetting(hero, this, "chance-3x-per-level", 0.0, false) * hero.getSkillLevel(this);
        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 20, false);
        Block targetBlock = player.getTargetBlock((HashSet<Byte>)null, distance).getRelative(BlockFace.UP);
        summonEntity(hero, args, targetBlock);
        double chance = Util.nextRand();
        if (chance <= chance3x) {
            summonEntity(hero, args, targetBlock);
            summonEntity(hero, args, targetBlock);
        } else if (chance <= chance2x) {
            summonEntity(hero, args, targetBlock);
        }
        applySoundEffects(hero.getPlayer().getWorld(), hero.getPlayer());
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    protected Entity summonEntity(Hero hero, String args[], Block targetBlock) {
        return hero.getPlayer().getWorld().spawnEntity(targetBlock.getLocation(), getEntityType(targetBlock));
    }

    protected abstract EntityType getEntityType(Block targetBlock);

    protected void applySoundEffects(World world, Player player) { }
}
