package com.herocraftonline.heroes.characters.skill.skills;


import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillGroupHeal extends ActiveSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    public SkillGroupHeal(Heroes plugin) {
        super(plugin, "GroupHeal");
        setDescription("You restore $1 health to all nearby party members.");
        setUsage("/skill groupheal");
        setArgumentRange(0, 0);
        setIdentifiers("skill groupheal", "skill gheal");
        setTypes(SkillType.HEAL, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("heal-amount", 2);
        node.set(SkillSetting.RADIUS.node(), 5);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int healAmount = SkillConfigManager.getUseSetting(hero, this, "heal-amount", 2, false);
        if (hero.getParty() == null) {
            // Heal just the caster if he's not in a party
            HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(hero, healAmount, this, hero);
            plugin.getServer().getPluginManager().callEvent(hrhEvent);
            if (hrhEvent.isCancelled()) {
                Messaging.send(player, "Unable to heal the target at this time!");
                return SkillResult.CANCELLED;
            }

            hero.heal(hrhEvent.getAmount());
            //changed to hero.heal for bukkit events
        } else {
            int radiusSquared = (int) Math.pow(SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false), 2);
            Location heroLoc = player.getLocation();
            // Heal party members near the caster
            for (Hero partyHero : hero.getParty().getMembers()) {
                if (!player.getWorld().equals(partyHero.getPlayer().getWorld())) {
                    continue;
                }
                if (partyHero.getPlayer().getLocation().distanceSquared(heroLoc) <= radiusSquared) {
                    HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(partyHero, healAmount, this, hero);
                    plugin.getServer().getPluginManager().callEvent(hrhEvent);
                    if (hrhEvent.isCancelled()) {
                        Messaging.send(player, "Unable to heal the target at this time!");
                        return SkillResult.CANCELLED;
                    }

                    //old - partyHero.getPlayer().setHealth(partyHero.getPlayer().getHealth() + hrhEvent.getAmount());
                    partyHero.heal(hrhEvent.getAmount());

                }
            }
        }

        broadcastExecuteText(hero);
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), player.getLocation().add(0,1.5,0), FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BALL).withColor(Color.FUCHSIA).withFade(Color.WHITE).build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int heal = SkillConfigManager.getUseSetting(hero, this, "heal-amount", 2, false);
        return getDescription().replace("$1", heal + "");
    }
}
