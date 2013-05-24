package com.herocraftonline.heroes.characters.skill.skills.absorbrunes;

/*
 * Coded by: Delfofthebla - 5 / 1 / 2013
 * 
 * This is the "RuneActivation" Event. It is meant to be triggered whenever a hero "activates" a 'Rune' skill.
 * These events can realistically be listened for anything, but their primary function is to notify the Hosting Rune Skill that a rune has been activated.
 * Letting the hosting skill know that a Rune has been activated allows it to alter an individual player's Rune list, which changes rapidly during combat.
 */

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.herocraftonline.heroes.characters.Hero;

public class RuneActivationEvent extends Event
{
	private static final HandlerList handlers = new HandlerList();

	// Rune Applier information
	private final Hero h;

	// Rune to be applied
	private final Rune rune;

	/**
	 * RuneActivation Event is triggered when a Rune has been activated by a player.
	 * @param h - The Hero that activated the Rune ability.
	 * @param rune - The specific Rune that was activated.
	 */
	public RuneActivationEvent(Hero h, Rune rune)
	{
		this.h = h;
		this.rune = rune;
	}

	public Hero getHero()
	{
		return h;
	}

	public Rune getRune()
	{
		return rune;
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
