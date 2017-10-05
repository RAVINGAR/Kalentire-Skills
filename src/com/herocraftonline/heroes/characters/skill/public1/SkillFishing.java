package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;


public class SkillFishing extends PassiveSkill {

    public SkillFishing(Heroes plugin) {
        super(plugin, "Fishing");
        this.setDescription("You have a $1% chance of getting a bonus fish!");
        this.setEffectTypes(EffectType.BENEFICIAL);
        this.setTypes(SkillType.KNOWLEDGE, SkillType.ABILITY_PROPERTY_EARTH, SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillPlayerListener(this), plugin);
    }


    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("chance-per-level", .001);
        node.set("leather-level", 5);
        node.set("enable-leather", false);
        return node;
    }

    public class SkillPlayerListener implements Listener {

        private final Skill skill;

        SkillPlayerListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerFish(PlayerFishEvent event) {
            if (event.isCancelled() || (event.getState() != State.CAUGHT_FISH) || !(event.getCaught() instanceof Item)) {
                return;
            }
            final Item getCaught = (Item) event.getCaught();
            final double chance = Util.nextRand();
            final Hero hero = SkillFishing.this.plugin.getCharacterManager().getHero(event.getPlayer());
            final Player player = hero.getPlayer();
            if (chance < (SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.CHANCE_LEVEL, .001, false) * hero.getHeroLevel(this.skill))) { //if the chance

                final int leatherlvl = SkillConfigManager.getUseSetting(hero, this.skill, "leather-level", 5, true);
                if ((hero.getLevel() >= leatherlvl) && SkillConfigManager.getUseSetting(hero, this.skill, "enable-leather", false)) { //if fishing leather is enabled and have the level

                    if (getCaught != null) { //If not null
                        switch (Util.nextInt(6)) {
                            case 0:
                                getCaught.setItemStack(new ItemStack(Material.LEATHER_BOOTS, 1));
                                player.sendMessage(ChatColor.GRAY + "You found leather boots!");
                                getCaught.getItemStack().setDurability((short) (Math.random() * 40));
                                break;
                            case 1:
                                getCaught.setItemStack(new ItemStack(Material.LEATHER_LEGGINGS, 1));
                                player.sendMessage(ChatColor.GRAY + "You found leather leggings!");
                                getCaught.getItemStack().setDurability((short) (Math.random() * 46));
                                break;
                            case 2:
                                getCaught.setItemStack(new ItemStack(Material.LEATHER_HELMET, 1));
                                player.sendMessage(ChatColor.GRAY + "You found a leather helmet!");
                                getCaught.getItemStack().setDurability((short) (Math.random() * 34));
                                break;
                            case 3:
                                getCaught.setItemStack(new ItemStack(Material.LEATHER_CHESTPLATE, 1));
                                player.sendMessage(ChatColor.GRAY + "You found a leather chestplate!");
                                getCaught.getItemStack().setDurability((short) (Math.random() * 49));
                                break;
                            case 4:
                                getCaught.setItemStack(new ItemStack(Material.GOLDEN_APPLE, 1));
                                player.sendMessage(ChatColor.GRAY + "You found a golden apple, woo!");
                                getCaught.getItemStack().setDurability((short) (Math.random() * 10));
                                break;
                            case 5:
                                getCaught.setItemStack(new ItemStack(Material.APPLE, 1));
                                player.sendMessage(ChatColor.GRAY + "You found an apple!");
                                getCaught.getItemStack().setDurability((short) (Math.random() * 29));
                                break;
                            case 6:
                                getCaught.setItemStack(new ItemStack(Material.RAW_FISH, 2));
                                player.sendMessage(ChatColor.GRAY + "You found 2 Fishes!");
                                break;
                            case 7:
                                getCaught.setItemStack(new ItemStack(Material.RAW_FISH, 1));
                                player.sendMessage(ChatColor.GRAY + "You found 1 Fish!");
                                break;
                        }
                    }
                } else {
                    switch (Util.nextInt(2)) {
                        case 0:
                            getCaught.setItemStack(new ItemStack(Material.RAW_FISH, 2));
                            player.sendMessage(ChatColor.GRAY + "You found 2 Fishes!");
                            break;
                        case 1:
                            getCaught.setItemStack(new ItemStack(Material.RAW_FISH, 1));
                            player.sendMessage(ChatColor.GRAY + "You found 1 Fish!");
                            break;
                    }
                }
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        final double chance = SkillConfigManager.getUseSetting(hero, this, "chance-per-level", .001, false);
        int level = hero.getHeroLevel(this);
        if (level < 1) {
            level = 1;
        }
        return this.getDescription().replace("$1", Util.stringDouble(chance * level * 100));
    }
}
