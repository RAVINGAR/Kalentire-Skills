package com.herocraftonline.heroes.characters.skill.pack1.totem;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.skills.totem.SkillBaseTotem;
import com.herocraftonline.heroes.characters.skill.skills.totem.TotemEffect;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class Totem {

    private Location loc;
    private boolean fireOnNaturalRemove;
    private double range;
    private List<Block> blocks = new ArrayList<Block>();
    private EnderCrystal crystal;
    private TotemEffect effect = null;

    public Totem(SkillBaseTotem skill, Location loc, boolean fireOnNaturalRemove, double range) {
        setLocation(loc);
        setRange(range);
        setFireOnNaturalRemove(fireOnNaturalRemove);
    }

    public boolean canCreateTotem(Material material) {

        // Used to check if any block of the Totem is grounded, so it can't be in the air but it works on tall grass.
        boolean isGrounded = false;
        World world = loc.getWorld();

        blocks.add(world.getBlockAt(loc));
        blocks.add(world.getBlockAt(loc).getRelative(BlockFace.UP));
        blocks.add(world.getBlockAt(loc).getRelative(BlockFace.UP, 2));

        for(int i = 0; i < 3; i++) {

            Block block = blocks.get(i);


            if(!block.getType().equals(Material.AIR)) {
                return false;
            }

            if(i < 2) {
                if(!block.getRelative(BlockFace.DOWN).getType().equals(Material.AIR)) {
                    isGrounded = true;
                }

                if(SkillBaseTotem.totemExistsInRadius(blocks.get(0), 5)) {
                    return false;
                }
            }
        }
        
        return isGrounded;
    }
    
    public void createTotem(Material material) {

        World world = loc.getWorld();

        for(int i = 0; i < 2; i++) {
            blocks.get(i).setType(material);
        }
        crystal = (EnderCrystal) world.spawnEntity(blocks.get(1).getLocation().add(.5, .75, .5), EntityType.ENDER_CRYSTAL);
        return;
    }

    public void destroyTotem() {

        for(int i = 0; i < 2; i++) {

            Block block = blocks.get(i);

            Chunk chunk = block.getChunk();
            if(!chunk.isLoaded()) {
                chunk.load();
            }

            block.setType(Material.AIR);
        }
        
        crystal.remove();
        
        return;
    }

    public Location getLocation() {
        return loc;
    }

    public void setLocation(Location loc) {
        this.loc = loc;
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    public Block getBlock(int id) {
        return blocks.get(id);
    }

    public void setBlock(int id, Block block) {
        this.blocks.set(id, block);
    }

    public List<LivingEntity> getTargets(Hero hero) {

        double range = getRange();

        List<Entity> potentialTargets = crystal.getNearbyEntities(range, range, range);

        List<LivingEntity> targets = new ArrayList<LivingEntity>();
        for(Entity target : potentialTargets) {
            if(target instanceof LivingEntity)
                targets.add((LivingEntity) target);
        }

        return targets;
    }

    public EnderCrystal getCrystal() {
        return crystal;
    }

    public void setCrystal(EnderCrystal crystal) {
        this.crystal = crystal;
    }

    public boolean getFireOnNaturalRemove() {
        return fireOnNaturalRemove;
    }

    public void setFireOnNaturalRemove(boolean fireOnNaturalRemove) {
        this.fireOnNaturalRemove = fireOnNaturalRemove;
    }

    public TotemEffect getEffect() {
        return effect;
    }

    public void setEffect(TotemEffect effect) {
        this.effect = effect;
    }

}
