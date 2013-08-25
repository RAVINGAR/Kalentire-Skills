package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;


public class SkillFishing extends PassiveSkill {

    public SkillFishing(Heroes plugin) {
        super(plugin, "Fishing");
        setDescription("You have a $1% chance of getting a bonus fish!");
        setEffectTypes(EffectType.BENEFICIAL);
        setTypes(SkillType.KNOWLEDGE, SkillType.EARTH, SkillType.BUFF);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillPlayerListener(this), plugin);
    }


    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("chance-per-level", .001);
        node.set("leather-level", 5);
        node.set("enable-leather", false);
        return node;
    }

    public class SkillPlayerListener implements Listener {

        private Skill skill;

        SkillPlayerListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerFish(PlayerFishEvent event){
            if (event.isCancelled() || event.getState() != State.CAUGHT_FISH || !(event.getCaught() instanceof CraftItem)) {
                return;
            }
            CraftItem getCaught = (CraftItem) event.getCaught();
            double chance = Util.nextRand();
            Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            Player player = hero.getPlayer();
            if (chance < SkillConfigManager.getUseSetting(hero, skill, SkillSetting.CHANCE_LEVEL, .001, false) * hero.getSkillLevel(skill)){ //if the chance

                int leatherlvl = SkillConfigManager.getUseSetting(hero, skill, "leather-level", 5, true);
                if (hero.getLevel() >= leatherlvl && SkillConfigManager.getUseSetting(hero, skill, "enable-leather", false)){ //if fishing leather is enabled and have the level

                    if (getCaught != null){ //If not null
                        switch(Util.nextInt(8)){
                        case 0: 
                            getCaught.setItemStack(new ItemStack(Material.LEATHER_BOOTS, 1));
                            Messaging.send(player, "You found leather boots!");
                            getCaught.getItemStack().setDurability((short) (Math.random() * 40));
                            break;
                        case 1: 
                            getCaught.setItemStack(new ItemStack(Material.LEATHER_LEGGINGS, 1));
                            Messaging.send(player, "You found leather leggings!");
                            getCaught.getItemStack().setDurability((short) (Math.random() * 46));
                            break;
                        case 2: 
                            getCaught.setItemStack(new ItemStack(Material.LEATHER_HELMET, 1));
                            Messaging.send(player, "You found a leather helmet!");
                            getCaught.getItemStack().setDurability((short) (Math.random() * 34));
                            break;
                        case 3: 
                            getCaught.setItemStack(new ItemStack(Material.LEATHER_CHESTPLATE, 1));
                            Messaging.send(player, "You found a leather chestplate!");
                            getCaught.getItemStack().setDurability((short) (Math.random() * 49));
                            break;
                        case 4: 
                            getCaught.setItemStack(new ItemStack(Material.GOLDEN_APPLE, 1));
                            Messaging.send(player, "You found a golden apple, woo!");
                            getCaught.getItemStack().setDurability((short) (Math.random() * 10));
                            break;
                        case 5: 
                            getCaught.setItemStack(new ItemStack(Material.APPLE, 1));
                            Messaging.send(player, "You found an apple!");
                            getCaught.getItemStack().setDurability((short) (Math.random() * 29));
                            break;
                        case 6: 
                            getCaught.setItemStack(new ItemStack(Material.RAW_FISH, 2));
                            Messaging.send(player, "You found 2 Fish!");
                            break;
                        case 7: 
                            getCaught.setItemStack(new ItemStack(Material.RAW_FISH, 1));
                            Messaging.send(player, "You found 1 Fish!");
                            break;
                        }
                    }
                } else {
                    switch(Util.nextInt(2)){
                    case 0: 
                        getCaught.setItemStack(new ItemStack(Material.RAW_FISH, 2));
                        Messaging.send(player, "You found 2 Fishes!");
                        break;
                    case 1: 
                        getCaught.setItemStack(new ItemStack(Material.RAW_FISH, 1));
                        Messaging.send(player, "You found 1 Fish!");
                        break;
                    }
                }   
            }           
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double chance = SkillConfigManager.getUseSetting(hero, this, "chance-per-level", .001, false);
        int level = hero.getSkillLevel(this);
        if (level < 1)
            level = 1;
        return getDescription().replace("$1", Util.stringDouble(chance * level * 100));
    }
}