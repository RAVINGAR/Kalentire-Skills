package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.Skill;
import org.bukkit.command.CommandSender;

 public class SkillBaseUtility extends Skill {

	 private static Object throwException() {
		 throw new RuntimeException("Static utility skill constructed");
	 }

	 protected SkillBaseUtility() {
		 super((Heroes) throwException(), (String) throwException());
	 }

	 @Override
	 public final String getDescription(Hero hero) {
		 return (String) throwException();
	 }

	 @Override
	 public final boolean execute(CommandSender commandSender, String s, String[] strings) {
		 return (Boolean) throwException();
	 }

	 @Override
	 public final void init() {
		 throwException();
	 }
 }
