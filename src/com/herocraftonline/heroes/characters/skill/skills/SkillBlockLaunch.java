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
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

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
		Block block = hero.getPlayer().getTargetBlock(Util.transparentBlocks, 50);
		if (blockLaunch(block)) {
			broadcastExecuteText(hero);
			return SkillResult.NORMAL;
		} else {
			return SkillResult.INVALID_TARGET_NO_MSG;
		}
	}

	private boolean blockLaunch(Block block) {
		if (!Util.transparentBlocks.contains(block.getType()) && block.getRelative(BlockFace.UP).getType() == Material.AIR) {

			@SuppressWarnings("deprecation")// Bukkit can sukkit
			FallingBlock fb = block.getWorld().spawnFallingBlock(block.getLocation().add(0, 0.5, 0), block.getType(), block.getData());
			fb.setVelocity(new Vector(0, 1, 0));
			fb.setMetadata("block-pulse", new FixedMetadataValue(plugin, new Object()));

			return true;
		} else {
			return false;
		}
	}

	@EventHandler
	private void onFallingBlockFall(EntityChangeBlockEvent event) {
		if (event.getEntity() instanceof FallingBlock && event.getEntity().hasMetadata("block-pulse")) {
			event.setCancelled(true);
			event.getEntity().remove();
		}
	}
}
