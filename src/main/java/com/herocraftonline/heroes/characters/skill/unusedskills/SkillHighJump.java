package com.herocraftonline.heroes.characters.skill.unusedskills;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillHighJump extends ActiveSkill {

    private static final Set<Material> noJumpMaterials;
    static {
        noJumpMaterials = new HashSet<>();
        noJumpMaterials.add(Material.WATER);
        noJumpMaterials.add(Material.AIR);
        noJumpMaterials.add(Material.LAVA);
        noJumpMaterials.add(Material.ACACIA_LEAVES);
        noJumpMaterials.add(Material.SPRUCE_LEAVES);
        noJumpMaterials.add(Material.OAK_LEAVES);
        noJumpMaterials.add(Material.JUNGLE_LEAVES);
        noJumpMaterials.add(Material.DARK_OAK_LEAVES);
        noJumpMaterials.add(Material.BIRCH_LEAVES);
        noJumpMaterials.add(Material.SOUL_SAND);
    }
    
    public SkillHighJump(Heroes plugin) {
        super(plugin, "HighJump");
        setDescription("You jump into the air, higher!");
        setUsage("/skill highjump");
        setArgumentRange(0, 0);
        setIdentifiers("skill highjump");
        setTypes(SkillType.MOVEMENT_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL);
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("no-air-jump", true);
        return node;
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Material mat = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
        if ((SkillConfigManager.getUseSetting(hero, this, "no-air-jump", true) && noJumpMaterials.contains(mat)) || player.isInsideVehicle()) {
            player.sendMessage("You can't jump while mid-air or from inside a vehicle!");
            return SkillResult.FAIL;
        }
        float pitch = player.getEyeLocation().getPitch();
        int jumpForwards = 2;
        if (pitch > 45) {
            jumpForwards = 2;
        }
        if (pitch > 0) {
            pitch = -pitch;
        }
        float multiplier = (90f + pitch) / 50f;
        Vector v = player.getVelocity().setY(0.5).add(player.getLocation().getDirection().setY(0).normalize().multiply(multiplier * jumpForwards));
        player.setVelocity(v);
        player.setFallDistance(-8f);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_SILVERFISH_HURT , 10.0F, 1.0F);
        broadcastExecuteText(hero);
        
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
