package com.herocraftonline.heroes.characters.skill.remastered.shared;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;

public class SkillTameWolf extends PassiveSkill {

    public SkillTameWolf(Heroes plugin) {
        super(plugin, "TameWolf");
        setDescription("You have the ability to tame wolves.");
        setUsage("/skill tamewolf <release|summon>");
        setIdentifiers("skill tamewolf");
        setArgumentRange(0, 1);
        setTypes(SkillType.SUMMONING, SkillType.KNOWLEDGE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        return this.getDescription();
    }

    class SkillEntityListener implements Listener {
        private final SkillTameWolf skill;

        SkillEntityListener(SkillTameWolf skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityTame(EntityTameEvent event) {
            if (!(event.getEntity() instanceof Wolf) || !( event.getOwner() instanceof Player))
                return;

            final Player player = (Player)  event.getOwner();
            final Hero hero = SkillTameWolf.this.plugin.getCharacterManager().getHero(player);
            if (!hero.canUseSkill(this.skill.getName())) {
                player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You don't have the ability to tame wolves!");
                event.setCancelled(true);
            }
        }
    }
}
