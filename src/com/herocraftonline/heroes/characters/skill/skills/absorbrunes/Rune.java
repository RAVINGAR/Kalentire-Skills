package com.herocraftonline.heroes.characters.skill.skills.absorbrunes;

/*
 * Coded by: Delfofthebla - 5 / 1 / 2013
 * 
 * This is the Rune Node that is a component of the RuneQueue data structure.
 * 
 * As with the RuneQueue class, this should be used for data structure functionality and NOTHING ELSE.
 * Any desired effects from specific Runes should be done outside of this class.
 * This class is nothing more than a data structure component--keep it that way.
 * 
 * Dependent Classes:
 * RuneQueue.java				// The actual data structure. It is a key component of the Rune Node.
 * 								//		Without the RuneQueue this class has no usage or purpose.
 */

public class Rune
{
	// The next Rune in the queue
	public Rune next;

	// the name of the current Rune
	public String name;
	public int manaCost;
	public String chatColor;

	// All Runes must be instantiated with a variables set.
	public Rune(String name, int manaCost, String chatColor)
	{
		this.name = name;
		this.manaCost = manaCost;
		this.chatColor = chatColor;
	}
}
