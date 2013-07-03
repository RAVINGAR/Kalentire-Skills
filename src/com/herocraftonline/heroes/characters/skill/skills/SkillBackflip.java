package com.herocraftonline.heroes.characters.skill.skills;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillBackflip extends ActiveSkill {

	private static final Vector backwards = new Vector(-1, 1,-1);
    private static final Set<Material> nobackflipMaterials;
    static {
        nobackflipMaterials = new HashSet<Material>();
        nobackflipMaterials.add(Material.WATER);
        nobackflipMaterials.add(Material.AIR);
        nobackflipMaterials.add(Material.LAVA);
        nobackflipMaterials.add(Material.LEAVES);
        nobackflipMaterials.add(Material.SOUL_SAND);
    }
    
    public SkillBackflip(Heroes plugin) {
        super(plugin, "Backflip");
        setDescription("You backflip into the air");
        setUsage("/skill backflip");
        setArgumentRange(0, 0);
        setIdentifiers("skill backflip");
        setTypes(SkillType.MOVEMENT, SkillType.PHYSICAL);
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("no-air-backflip", true);
        return node;
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Material mat = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
        if ((SkillConfigManager.getUseSetting(hero, this, "no-air-backflip", true) && nobackflipMaterials.contains(mat)) || player.isInsideVehicle()) {
            Messaging.send(player, "You can't backflip while mid-air or from inside a vehicle!");
            return SkillResult.FAIL;
        }
        float pitch = player.getEyeLocation().getPitch();
        int backflipForwards = 1;
        if (pitch > 45) {
            backflipForwards = 1;
        }
        if (pitch > 0) {
            pitch = -pitch;
        }
        float multiplier = (90f + pitch) / 50f;
        Vector v = player.getVelocity().setY(1).add(player.getLocation().getDirection().setY(0).normalize().multiply(multiplier * backflipForwards)).multiply(backwards);
        player.setVelocity(v);
        player.setFallDistance(-8f);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.SKELETON_IDLE , 1.0F, 1.0F); 
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
