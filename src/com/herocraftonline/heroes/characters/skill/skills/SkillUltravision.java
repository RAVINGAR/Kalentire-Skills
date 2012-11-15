package com.herocraftonline.heroes.characters.skill.skills;

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
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillUltravision extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillUltravision(Heroes plugin) {
        super(plugin, "Ultravision");
        setDescription("You gain the ability to see in darkness for $1 seconds.");
        setUsage("/skill ultravision");
        setArgumentRange(0, 0);
        setIdentifiers("skill excavate");
        setTypes(SkillType.BUFF, SkillType.KNOWLEDGE, SkillType.SILENCABLE);
    }
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 0);
        node.set(Setting.DURATION_INCREASE.node(), 100);
        node.set("apply-text", "%hero% gains Ultravision!");
        node.set("expire-text", "%hero% loses Ultravision!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, "apply-text", "%hero% gains Ultravision!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, "expire-text", "%hero% loses Ultravision!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 0, false);
        duration += (SkillConfigManager.getUseSetting(hero, this, Setting.DURATION_INCREASE, 100, false) * hero.getSkillLevel(this));
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
        if (multiplier > 20) {
            multiplier = 20;
        }
        hero.addEffect(new NightvisionEffect(this, duration, multiplier));

        return SkillResult.NORMAL;
    }

    public class NightvisionEffect extends ExpirableEffect {

        public NightvisionEffect(Skill skill, long duration, int amplifier) {
            super(skill, "Ultravision", duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            addMobEffect(3, (int) (duration / 1000) * 20, amplifier, false);
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


    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 0, false);
        duration += (SkillConfigManager.getUseSetting(hero, this, Setting.DURATION_INCREASE, 100, false) * hero.getSkillLevel(this));
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
