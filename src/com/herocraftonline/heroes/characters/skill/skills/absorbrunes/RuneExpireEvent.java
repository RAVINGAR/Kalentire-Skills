package com.herocraftonline.heroes.characters.skill.skills.absorbrunes;

/*
 * Coded by: Delfofthebla - 5 / 11 / 2013
 * 
 * This is the "RuneExpiration" Event. It is meant to be triggered whenever a hero "applies" a 'Rune' skill, and then "expires" the Rune from his Rune list.
 * These events can realistically be listened for anything, but their primary function is to notify the Hosting Rune Skill skill that a Rune has been used and must be expired.
 * Letting the hosting skill know that a Rune has been expired allows it to alter an individual player's Rune list, which changes rapidly during combat.
 */

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.herocraftonline.heroes.characters.Hero;

public class RuneExpireEvent extends Event
{
	private static final HandlerList handlers = new HandlerList();

	// Rune Applier information
	private final Hero h;
	private final int runesToExpire;

	/**
	 * RuneApplicationEvent is triggered when a Rune has been "expired" from a RuneQueue List
	 * @param h - Hero to expire the Rune from
	 * @param runesToExpire - The amount of Runes that should be attempted to be expired.
	 */
	public RuneExpireEvent(Hero h, int runesToExpire)
	{
		this.h = h;
		this.runesToExpire = runesToExpire;
	}

	public Hero getHero()
	{
		return h;
	}

	public int getRunesToExpire()
	{
		return runesToExpire;
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
