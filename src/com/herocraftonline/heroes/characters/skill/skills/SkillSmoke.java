package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillSmoke extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillSmoke(Heroes plugin) {
        super(plugin, "Smoke");
        setDescription("You completely disappear from view.");
        setUsage("/skill smoke");
        setArgumentRange(0, 0);
        setIdentifiers("skill smoke");
        setNotes("Note: Taking damage removes the effect");
        setTypes(SkillType.ILLUSION, SkillType.BUFF, SkillType.COUNTER, SkillType.STEALTHY);
        
        
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 20000);
        node.set(Setting.APPLY_TEXT.node(), "You vanish in a cloud of smoke!");
        node.set(Setting.EXPIRE_TEXT.node(), "You reappeared!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "You vanish in a cloud of smoke!");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "You reappeared!");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 20000, false);
        Player player = hero.getPlayer();
        player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.SMOKE, 4);
        hero.addEffect(new InvisibleEffect(this, duration, applyText, expireText));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.EXPLODE , 0.8F, 1.0F); 
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
