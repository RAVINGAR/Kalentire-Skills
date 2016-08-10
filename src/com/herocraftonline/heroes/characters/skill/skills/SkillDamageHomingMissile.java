package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.nms.physics.RayCastFlag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.EnumSet;

import static com.herocraftonline.heroes.characters.skill.SkillType.*;
import static com.herocraftonline.heroes.characters.skill.SkillType.SILENCEABLE;
import static com.herocraftonline.heroes.characters.skill.SkillType.UNINTERRUPTIBLE;

public class SkillDamageHomingMissile extends SkillBaseHomingMissile {

    public SkillDamageHomingMissile(Heroes plugin) {
        super(plugin, "DamageHomingMissile");
        setDescription("Damage stuff with homing missile");
        setUsage("/skill DamageHomingMissile");
        setIdentifiers("skill " + getName());
        setTypes(DAMAGING, NO_SELF_TARGETTING, UNINTERRUPTIBLE, SILENCEABLE);
        setArgumentRange(0, 0);
    }

    @Override
    public String getDescription(Hero hero) {
        return super.getDescription();
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity livingEntity, String[] strings) {

        broadcastExecuteText(hero);
        Player player = hero.getPlayer();

        super.fireHomingMissile(hero, true,
                () -> livingEntity.getLocation().toVector(), player.getEyeLocation().toVector(), 600,
                player.getEyeLocation().getDirection().multiply(0.2), 0.2, 4, 0.25,
                entity -> entity == livingEntity, block -> false,
                EnumSet.of(RayCastFlag.BLOCK_HIGH_DETAIL, RayCastFlag.BLOCK_HIT_FLUID_SOURCE, RayCastFlag.ENTITY_HIT_SPECTATORS));

        return SkillResult.NORMAL;
    }

    @Override
    protected void onEntityHit(Hero hero, Entity entity, Vector hitOrigin, Vector hitForce) {
        
    }

    @Override
    protected void onEntityInvolved(Hero hero, Entity entity, Vector hitOrigin, Vector hitForce) {

    }

    @Override
    protected void onBlockHit(Hero hero, Block block, Vector hitPosition, Vector hitForce, BlockFace hitFace) {

    }

    @Override
    protected void onBlockPassed(Hero hero, Block block, Vector hitPosition, Vector hitForce, BlockFace hitFace) {

    }

    @Override
    protected void renderMissilePath(World world, Vector start, Vector end) {

    }
}
