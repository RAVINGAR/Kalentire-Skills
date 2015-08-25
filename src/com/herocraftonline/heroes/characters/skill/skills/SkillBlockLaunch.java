package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.Set;

public class SkillBlockLaunch extends ActiveSkill implements Listener {

	public SkillBlockLaunch(Heroes plugin) {
		super(plugin, "BlockLaunch");
		setDescription("Test for block pulse");
		setUsage("/skill blocklaunch");
		setIdentifiers("skill blocklaunch");
		setArgumentRange(0, 0);

		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		Block block = hero.getPlayer().getTargetBlock((Set<Material>) null, 50);
		if (blockLaunch(block)) {
			broadcastExecuteText(hero);
			return SkillResult.NORMAL;
		} else {
			return SkillResult.INVALID_TARGET_NO_MSG;
		}
	}

	private boolean blockLaunch(Block block) {
		if (block.getType() != Material.AIR && block.getRelative(BlockFace.UP).getType() == Material.AIR) {

			@SuppressWarnings("deprecation")// Bukkit can Sukkit
			FallingBlock fb = block.getWorld().spawnFallingBlock(block
					// Comment this out when testing fixes to client remove block thingy
					.getRelative(BlockFace.UP)
					.getLocation(), block.getType(), block.getData());

			fb.setDropItem(false);
			fb.setVelocity(new Vector(0, 0.4, 0));

			fb.setMetadata("block-pulse", new FixedMetadataValue(plugin, new Object()));

			return true;
		} else {
			return false;
		}
	}

	@EventHandler
	private void onFallingBlockFall(EntityChangeBlockEvent event) {
		if (event.getEntity().getType() == EntityType.FALLING_BLOCK && event.getEntity().hasMetadata("block-pulse")) {
			event.setCancelled(true);
		}
	}
}
