package com.herocraftonline.heroes.characters.skill.skills;

import com.google.common.base.Predicate;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.NumberConversions;

import java.util.HashSet;
import java.util.Set;

public abstract class SkillBaseGroundEffect extends SkillBaseMassBlockEffector {

	public SkillBaseGroundEffect(Heroes plugin, String name) {
		super(plugin, name);
	}

	protected final void process(final Hero hero, final double radius, final int effectHeight, final long duration, final long period,
	                             final BlockProcessor blockProcessor, final TargetHandler targetHandler, final Predicate<BlockState> blockFilter) {
		final Location origin = hero.getPlayer().getLocation();
		final BlockRegion region = BlockRegion.bounds(origin.getBlock(), NumberConversions.ceil(radius));

		Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

			private Set<BlockState> tracked = new HashSet<>();

			@Override
			public void run() {

				//TODO Expand from center in a sort of mold of the landscape

				BlockState center = region.getCenter();
				spread(center);
			}

			private void spread(BlockState block) {
				if (validSpread(block)) {
					track(block);

					if (validTarget(block)) {

					}

					spread(region.getRelative(block, BlockFace.DOWN));
					spread(region.getRelative(block, BlockFace.NORTH));
					spread(region.getRelative(block, BlockFace.SOUTH));
					spread(region.getRelative(block, BlockFace.EAST));
					spread(region.getRelative(block, BlockFace.WEST));
					spread(region.getRelative(block, BlockFace.UP));
				}
			}

			private boolean validSpread(BlockState block) {
				return block != null && isTracked(block) && Util.transparentBlocks.contains(block.getType())
						&& blockFilter.apply(block) && origin.distanceSquared(block.getLocation().add(0.5, 0.5, 0.5)) <= NumberConversions.square(radius);
			}

			private boolean isTracked(BlockState block) {
				return !tracked.contains(block);
			}

			private void track(BlockState block) {
				tracked.add(block);
			}

			private boolean validTarget(BlockState block) {
				return !Util.transparentBlocks.contains(region.getRelative(block, BlockFace.DOWN));
			}
		});
	}

	public interface TargetHandler {
		void handle(Hero hero, LivingEntity target);
	}
}
