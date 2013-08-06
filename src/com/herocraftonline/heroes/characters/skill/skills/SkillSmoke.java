package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

public class SkillSmoke extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillSmoke(Heroes plugin) {
        super(plugin, "Smoke");
        setDescription("Vanish in a cloud of smoke! You will not be visible to other players for the next $1 seconds. Taking damage or using abilities will cause you to reappear.");
        setUsage("/skill smoke");
        setArgumentRange(0, 0);
        setIdentifiers("skill smoke");
        setNotes("Note: Interacting with anything removes the effect.");
        setNotes("Note: Taking damage removes the effect.");
        setNotes("Note: Using skills removes the effect.");
        setTypes(SkillType.ILLUSION, SkillType.PHYSICAL, SkillType.BUFF, SkillType.STEALTHY);

        //Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double duration = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4500, false) / 1000.0);

        return getDescription().replace("$1", duration + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 5500);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] Someone vanished in a cloud of smoke!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% has reappeared!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] Someone vanished in a cloud of smoke!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% has reappeared!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        Player player = hero.getPlayer();

        player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 4);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4500, false);
        hero.addEffect(new SmokeEffect(this, duration));

        return SkillResult.NORMAL;
    }

    public class SmokeEffect extends InvisibleEffect {

        public SmokeEffect(Skill skill, long duration) {
            super(skill, duration, null, null);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();

            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }
}
