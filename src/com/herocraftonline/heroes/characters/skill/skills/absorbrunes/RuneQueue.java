package com.herocraftonline.heroes.characters.skill.skills.absorbrunes;

/*
 * Coded by: Delfofthebla - 5 / 1 / 2013
 * 
 * This is the RuneQueue class. It is an implementation of the "Queue" data structure, and used to keep track of "Rune lists"
 * All data structure functionality is located within this class and NOTHING ELSE.
 * Any desired effects from popping or pushing should be done outside of this class. This class is nothing more than a data structure--keep it that way.
 * 
 * If you are unfamiliar with the Queue data structure, you can read about it here: http://en.wikipedia.org/wiki/Queue_%28abstract_data_type%29
 * 
 * New runes are pushed to the back, and old ones are popped off the front. Nice and easy.
 * 
 * Dependent Classes:
 * Rune.java					// The actual Rune object. It is a key component of the RuneQueue Data Structure and keeps track
 * 								//		of Rune names and the next Runes in queue.
 */

public class RuneQueue
{
	private int capacity;
	private final int size;

	private Rune head;
	private Rune current;

	public RuneQueue()
	{
		capacity = 0;		// List is empty from the start
		size = 3;			// Change the maximum size of the list here
		head = null;		// List is always created with a null head.
	}

	public Rune getHead()
	{
		return head;
	}

	public int getCapacity()
	{
		return capacity;
	}

	public int getSize()
	{
		return size;
	}

	// Check to see if the queue is full.
	public boolean isFull()
	{
		if (capacity == size)
			return true;
		else
			return false;
	}

	// Check to see if the queue is empty.
	public boolean isEmpty()
	{
		if (capacity == 0 || head == null)
			return true;
		else
			return false;
	}

	// Pop a Rune of the queue
	public void pop()
	{
		// Ensure that the list isn't empty before attempting to pop anything off. 
		if (!isEmpty())
		{
			// Check to see if this is the final rune
			if (head.next == null)
			{
				// if so, set head to null
				head = null;
				capacity--;
			}
			else
			{
				// Clear current object variable from memory and reduce the capacity		
				head = head.next;
				capacity--;

				// DEBUG
				//Bukkit.broadcastMessage("Current rune is now " + head.name + ".");
			}
		}
	}

	// Push a Rune onto the list
	public void push(Rune rune)
	{
		if (isEmpty())				// First check if the queue is empty, and set the head.
		{
			// The list is empty, create a starting point.
			head = rune;
			capacity++;
		}
		else if (isFull())			// Now check if it is full. We need to know if we should pop first
		{
			// The list is full, we need to pop before we can push.

			pop();

			setCurrentToLast();

			current.next = rune;
			capacity++;
		}
		else
		// Otherwise behave normally, push to the end.
		{
			setCurrentToLast();

			// Normal push here
			current.next = rune;
			capacity++;
		}

		// Clear current from memory
		current = null;
	}

	// Set the current rune to the last one in the list
	public void setCurrentToLast()
	{
		// Set current to the head
		if (head != null)
			current = head;
		else
			return;

		// Iterate through the queue until we hit the end
		while (current.next != null)
		{
			current = current.next;
		}
	}

	// Build a string array with the list of names in the queue
	public String[] getRuneNameList()
	{
		// Return null if we don't have any runes. Bad idea? Maybe change this later...
		if (isEmpty())
			return null;

		// Create the list
		String[] list = new String[capacity];

		current = head;

		// Iterate through all runes and save their names
		for (int i = 0; i < capacity; i++)
		{
			list[i] = current.name;
			current = current.next;
		}

		// Clear current from memory
		current = null;

		// Return the list
		return list;
	}

	// Build a string array with the list of colored names in the queue
	public String[] getColoredRuneNameList()
	{
		// Return null if we don't have any runes. Bad idea? Maybe change this later...
		if (isEmpty())
			return null;

		// Create the list
		String[] list = new String[capacity];

		current = head;

		// Iterate through all runes and save their names
		for (int i = 0; i < capacity; i++)
		{
			list[i] = current.chatColor + current.name;
			current = current.next;
		}

		// Clear current from memory
		current = null;

		// Return the list
		return list;
	}
}
