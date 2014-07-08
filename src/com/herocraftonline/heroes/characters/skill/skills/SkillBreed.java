package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class SkillBreed extends PassiveSkill {

    public SkillBreed(Heroes plugin) {
        super(plugin, "Breed");
        setDescription("You have gained the ability to breed animals.");
        setEffectTypes(EffectType.BENEFICIAL);
        Bukkit.getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double chance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE_PER_LEVEL, .001, false);
        int level = hero.getSkillLevel(this);
        if (level < 1)
            level = 1;

        String formattedChance = Util.decFormat.format(chance * level * 100.0);

        return getDescription().replace("$1", formattedChance);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set(SkillSetting.CHANCE_PER_LEVEL.node(), .001);
        node.set("allow-horse-mounting", false);
        node.set("allow-horse-breeding", false);

        return node;
    }

    public class SkillListener implements Listener {

        private Skill skill;

        SkillListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onPlayerEntityInteract(PlayerInteractEntityEvent event) {
            Player player = event.getPlayer();
            Hero hero = plugin.getCharacterManager().getHero(player);
            Material material = player.getItemInHand().getType();

            Entity targetEntity = event.getRightClicked();

            // Only deal with animals.
            if (!(targetEntity instanceof Animals))
                return;

            boolean isBreedAttempt = false;

            // Handle horse interaction
            if (targetEntity instanceof Horse) {
                switch (material) {
                    case WHEAT:
                    case HAY_BLOCK:
                    case SUGAR:
                    case BREAD:
                    case GOLDEN_APPLE:
                    case GOLDEN_CARROT:
                        isBreedAttempt = true;
                        break;
                    default:
                        break;
                }

                boolean canMountHorses = SkillConfigManager.getUseSetting(hero, skill, "allow-horse-mounting", false);
                if (!isBreedAttempt && !canMountHorses) {
                    player.sendMessage(ChatColor.GRAY + "Horse Mounting is Currently Disabled!");
                    event.setCancelled(true);
                    return;
                }

                if (isBreedAttempt) {
                    // If they are trying to breed the horse, check to make sure they are allowed to.
                    boolean canBreedHorses = SkillConfigManager.getUseSetting(hero, skill, "allow-horse-breeding", false);
                    if (!hero.canUseSkill(getName())) {
                        if (canBreedHorses) {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.GRAY + "You must be a farmer to do that!");
                            return;
                        }
                    }

                    if (!canBreedHorses) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.GRAY + "Horse breeding is currently disabled!");
                        return;
                    }

                    return;
                }
            }
            else if (targetEntity instanceof Sheep || targetEntity instanceof Cow) {
                if (material == Material.WHEAT)
                    isBreedAttempt = true;
            }
            else if (targetEntity instanceof Pig) {
                if (material == Material.CARROT)
                    isBreedAttempt = true;
            }
            else if (targetEntity instanceof Chicken) {
                switch (material) {
                    case MELON_SEEDS:
                    case PUMPKIN_SEEDS:
                    case NETHER_WARTS:
                        isBreedAttempt = true;
                        break;
                    default:
                        return;
                }
            }
            else if (targetEntity instanceof Ocelot) {
                if (material == Material.RAW_FISH)
                    isBreedAttempt = true;
            }
            else if (targetEntity instanceof Wolf) {
                if (material == Material.BONE) {
                    // We don't handle wolf taming events. ignore it.
                    return;
                }
            }

            // If we make it this far, they are trying to breed.
            if (isBreedAttempt && !hero.canUseSkill(getName())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.GRAY + "You must be a farmer to do that!");
            }
        }
    }
}
