package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
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
        setTypes(SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.BUFFING, SkillType.STEALTHY);

        //Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(3000), false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), Integer.valueOf(5500));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "Someone vanished in a cloud of smoke!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% has reappeared!");
        node.set(SkillSetting.REAGENT.node(), Integer.valueOf(289));
        node.set(SkillSetting.REAGENT_COST.node(), Integer.valueOf(1));

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "Someone vanished in a cloud of smoke!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% has reappeared!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        Player player = hero.getPlayer();

        player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 4);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4500, false);
        hero.addEffect(new InvisibleEffect(this, player, duration, applyText, expireText));

        return SkillResult.NORMAL;
    }
}
