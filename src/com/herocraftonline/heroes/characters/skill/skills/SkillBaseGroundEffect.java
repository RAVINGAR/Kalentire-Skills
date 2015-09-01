package com.herocraftonline.heroes.characters.skill.skills;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.util.NumberConversions;

public abstract class SkillBaseGroundEffect extends SkillBaseMassBlockEffector {

	public SkillBaseGroundEffect(Heroes plugin, String name) {
		super(plugin, name);
	}

	@Override
	protected final void process(final Hero hero, final BlockRegion region, final BlockProcessor processor, final Predicate<BlockState> filter) {
		Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				BlockState center = region.getCenter();

				//TODO Expand from center in a sort of mold of the landscape
			}
		});
	}

	protected final void process(Hero hero, final double radius, BlockProcessor processor, Predicate<BlockState> filter) {
		final Location origin = hero.getPlayer().getLocation();

		BlockRegion region = BlockRegion.bounds(origin.getBlock(), NumberConversions.ceil(radius));
		filter = Predicates.and(new Predicate<BlockState>() {
			@Override
			public boolean apply(BlockState block) {
				return origin.distanceSquared(block.getLocation().add(0.5, 0.5, 0.5)) <= NumberConversions.square(radius);
			}
		}, filter);

		process(hero, region, processor, filter);
	}
}
