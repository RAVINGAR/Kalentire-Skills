package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillSmoke extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillSmoke(Heroes plugin) {
        super(plugin, "Smoke");
        this.setDescription("You completely disappear from view.");
        this.setUsage("/skill smoke");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill smoke");
        this.setNotes("Note: Taking damage removes the effect");
        this.setTypes(SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.BUFFING, SkillType.DISABLE_COUNTERING, SkillType.STEALTHY);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 20000);
        node.set(SkillSetting.APPLY_TEXT.node(), "You vanish in a cloud of smoke!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "You reappeared!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "You vanish in a cloud of smoke!");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "You reappeared");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        this.broadcastExecuteText(hero);

        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        final Player player = hero.getPlayer();
        player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.SMOKE, 4);
        hero.addEffect(new InvisibleEffect(this, player, duration, this.applyText, this.expireText));

        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return this.getDescription();
    }
}
