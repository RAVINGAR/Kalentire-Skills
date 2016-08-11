package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class SkillMissileBarrier extends SkillBaseHomingMissile {

    public SkillMissileBarrier(Heroes plugin) {
        super(plugin, "MissileBarrier");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();



        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        return null;
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        return null;
    }

    @Override
    protected void onEntityHit(Hero hero, Entity entity, Vector hitOrigin, Vector hitForce) {

    }

    @Override
    protected void onEntityPassed(Hero hero, Entity entity, Vector hitOrigin, Vector hitForce) {

    }

    @Override
    protected void onBlockHit(Hero hero, Block block, Vector hitPosition, Vector hitForce, BlockFace hitFace) {

    }

    @Override
    protected void onBlockPassed(Hero hero, Block block, Vector hitPosition, Vector hitForce, BlockFace hitFace) {

    }

    @Override
    protected void renderMissilePath(World world, Vector start, Vector end, double radius) {

    }

    @Override
    protected void onExpire(Hero hero, Vector position, Vector velocity, boolean hitSomething) {

    }
}
