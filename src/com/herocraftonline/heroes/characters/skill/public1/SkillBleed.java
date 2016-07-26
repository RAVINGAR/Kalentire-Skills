package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillBleed extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillBleed(Heroes plugin) {
        super(plugin, "Bleed");
        this.setDescription("You cause your target to bleed, dealing $1 damage over $1 seconds.");
        this.setUsage("/skill bleed <target>");
        this.setArgumentRange(0, 1);
        this.setTypes(SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
        this.setIdentifiers("skill bleed");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.PERIOD.node(), 2000);
        node.set(SkillSetting.DAMAGE_TICK.node(), 1);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% is bleeding!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has stopped bleeding!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%target% is bleeding!").replace("%target%", "$1");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "%target% has stopped bleeding!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        final Player player = hero.getPlayer();
        if (target.equals(player) || hero.getSummons().contains(target)) {
            return SkillResult.INVALID_TARGET;
        }

        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, true);
        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 1, false);
        tickDamage += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0, false) * hero.getSkillLevel(this));
        final BleedSkillEffect bEffect = new BleedSkillEffect(this, duration, period, tickDamage, player);
        this.plugin.getCharacterManager().getCharacter(target).addEffect(bEffect);
        this.broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public class BleedSkillEffect extends PeriodicDamageEffect {

        public BleedSkillEffect(Skill skill, long duration, long period, double tickDamage, Player applier) {
            super(skill, "Bleed", applier, period, duration, tickDamage);
            this.types.add(EffectType.BLEED);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), SkillBleed.this.applyText, player.getDisplayName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            this.broadcast(monster.getEntity().getLocation(), SkillBleed.this.expireText, CustomNameManager.getName(monster).toLowerCase());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), SkillBleed.this.expireText, player.getDisplayName());
        }
    }

    @Override
    public String getDescription(Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        damage += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0, false) * hero.getSkillLevel(this));
        damage = (damage * duration) / period;
        return this.getDescription().replace("$1", damage + "").replace("$2", (duration / 1000) + "");
    }
}
