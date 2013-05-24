package com.herocraftonline.heroes.characters.skill.skills.absorbrunes;

/*
 * Coded by: Delfofthebla - 5 / 11 / 2013
 * 
 * This is the "RuneApplication" Event. It is meant to be triggered whenever a hero "applies" a 'Rune' skill to a target.
 * These events can realistically be listened for anything, but their primary function is to notify individual "Rune" skills
 * 		that a Rune is ready to be applied.
 * 
 * Each individual Rune skill will listen for this event, and thus, know when a Rune is ready to be applied.
 * This allows it to actually perform the desired skill effect at the correct time.
 */

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.herocraftonline.heroes.characters.Hero;

public class RuneApplicationEvent extends Event
{
	private static final HandlerList handlers = new HandlerList();

	// Rune Applier information
	private final Hero h;

	// Rune Target information
	private final Entity target;

	// Rune to be applied
	private final RuneQueue runeList;

	/**
	 * RuneApplication Event is triggered when a rune is being applied to a specific target.
	 * @param h - Hero that is applying the Rune.
	 * @param target - Target entity to apply the Rune to.
	 * @param runeList - Runelist to grab a rune from.
	 */
	public RuneApplicationEvent(Hero h, Entity target, RuneQueue runeList)
	{
		this.h = h;
		this.target = target;
		this.runeList = runeList;
	}

	public Hero getHero()
	{
		return h;
	}

	public RuneQueue getRuneList()
	{
		return runeList;
	}

	public Entity getTarget()
	{
		return target;
	}

	@Override
	public HandlerList getHandlers()
	{
		return handlers;
	}

	public static HandlerList getHandlerList()
	{
		return handlers;
	}
}
