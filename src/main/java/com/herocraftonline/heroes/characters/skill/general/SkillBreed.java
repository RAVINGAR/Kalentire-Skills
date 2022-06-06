package com.herocraftonline.heroes.characters.skill.general;

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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;

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
        int level = hero.getHeroLevel(this);
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
        node.set("allow-horse-breeding", false);

        return node;
    }

    public class SkillListener implements Listener {

        private final Skill skill;

        SkillListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerBreedEvent(EntityBreedEvent event) {
            if(event.getBreeder() instanceof Player player) {
                Hero hero = plugin.getCharacterManager().getHero(player);
                if(hero.canUseSkill(getName())) {
                    if(event.getEntityType() == EntityType.HORSE && !SkillConfigManager.getUseSetting(hero, skill, "allow-horse-breeding", false)) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.GRAY + "You cannot breed horses!");
                    }
                }
                else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.GRAY + "You must be a farmer to do that!");
                }
            }
        }

    }
}
