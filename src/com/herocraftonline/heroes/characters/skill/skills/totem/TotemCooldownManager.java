package com.herocraftonline.heroes.characters.skill.skills.totem;

import com.herocraftonline.heroes.characters.Hero;

import java.util.HashMap;
import java.util.Map;

public class TotemCooldownManager {

    private static Map<Hero, Long> cooldowns = new HashMap<Hero, Long>();

    public static void addCooldown(Hero hero) {
        cooldowns.put(hero, System.currentTimeMillis());
    }

    public static boolean onCooldown(Hero hero) {
        if(cooldowns.containsKey(hero)) {
            if (cooldowns.get(hero) + hero.getCooldown("TotemCooldown") <= System.currentTimeMillis()) {
                hero.removeCooldown("TotemCooldown");
                return false;
            }
            return true;
        }
        return false;
    }

    public static void removeCooldown(Hero hero) {
        cooldowns.remove(hero);
    }

}
