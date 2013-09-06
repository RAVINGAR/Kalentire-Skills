package com.herocraftonline.heroes.characters.skill.unusedskills;
/*
package com.herocraftonline.heroes.characters.skill.oldskills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillBloodlust extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillBloodlust(Heroes plugin) {
        super(plugin, "Bloodlust");
        setDescription("You sacrifice half of your health to boost your damage and armor!");
        setUsage("/skill bloodlust");
        setArgumentRange(0, 0);
        setIdentifiers(new String[] { "skill bloodlust" });
        setTypes(new SkillType[] { SkillType.BUFF, SkillType.SILENCABLE, SkillType.PHYSICAL });
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(30000));
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% is in bloodlust!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% is no longer in bloodlust!");
        return node;
    }

    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%hero% is in bloodlust!").replace("%hero%", "$1");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "%hero% is no longer in bloodlust!").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);
        hero.getPlayer().setHealth(hero.getPlayer().getHealth() / 2);
        BloodlustEffect beffect = new BloodlustEffect(this, duration, this.applyText, this.expireText);
        hero.addEffect(beffect);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public class BloodlustEffect extends ExpirableEffect {
        private String applyText;
        private String expireText;

        public BloodlustEffect(Skill skill, long duration, String applyText, String expireText) {
            super(skill, "Bloodlust", duration);
            this.applyText = applyText;
            this.expireText = expireText;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.PHYSICAL);
            addMobEffect(5, (int)(duration / 1000L * 20L), 1, false);
            addMobEffect(11, (int)(duration / 1000L * 20L), 1, false);
        }

        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), this.applyText, new Object[] { player.getDisplayName() });
        }

        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), this.expireText, new Object[] { player.getDisplayName() });
        }
    }
}
*/