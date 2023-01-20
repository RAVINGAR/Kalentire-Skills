package com.herocraftonline.heroes.characters.skill.general;

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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillBloodlust extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillBloodlust(final Heroes plugin) {
        super(plugin, "Bloodlust");
        setDescription("You sacrifice half of your health to boost your damage and armor!");
        setUsage("/skill bloodlust");
        setArgumentRange(0, 0);
        setIdentifiers(new String[]{"skill bloodlust"});
        setTypes(new SkillType[]{SkillType.BUFFING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_PHYSICAL});
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 30000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% is in bloodlust!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% is no longer in bloodlust!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%hero% is in bloodlust!").replace("%hero%", "$1").replace("$hero$", "$1");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "%hero% is no longer in bloodlust!").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);
        hero.getPlayer().setHealth(hero.getPlayer().getHealth() / 2);
        final BloodlustEffect beffect = new BloodlustEffect(this, hero.getPlayer(), duration, this.applyText, this.expireText);
        hero.addEffect(beffect);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(final Hero hero) {
        return getDescription();
    }

    public static class BloodlustEffect extends ExpirableEffect {
        private final String applyText;
        private final String expireText;

        public BloodlustEffect(final Skill skill, final Player applier, final long duration, final String applyText, final String expireText) {
            super(skill, "Bloodlust", applier, duration);
            this.applyText = applyText;
            this.expireText = expireText;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.PHYSICAL);
            addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, (int) (duration / 1000L * 20L), 1), false);
            addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, (int) (duration / 1000L * 20L), 1), false);
        }

        public void apply(final Hero hero) {
            super.apply(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), this.applyText, new Object[]{player.getName()});
        }

        public void remove(final Hero hero) {
            super.remove(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), this.expireText, new Object[]{player.getName()});
        }
    }
}
