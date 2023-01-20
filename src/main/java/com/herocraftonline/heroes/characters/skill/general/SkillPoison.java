package com.herocraftonline.heroes.characters.skill.general;

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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillPoison extends TargettedSkill {

    private String expireText;

    public SkillPoison(final Heroes plugin) {
        super(plugin, "Poison");
        this.setDescription("You poison your target dealing $1 damage over $2 second(s).");
        this.setUsage("/skill poison <target>");
        this.setArgumentRange(0, 1);
        this.setIdentifiers("skill poison");
        this.setTypes(SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000); // in milliseconds
        node.set(SkillSetting.PERIOD.node(), 2000); // in milliseconds
        node.set("tick-damage", 1);
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has recovered from the poison!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has recovered from the poison!").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final Player player = hero.getPlayer();

        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, true);
        final double tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        this.plugin.getCharacterManager().getCharacter(target).addEffect(new PoisonSkillEffect(this, period, duration, tickDamage, player));
        this.broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        final double period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        final double damage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        return this.getDescription().replace("$1", ((damage * duration) / period) + "").replace("$2", (duration / 1000) + "");
    }

    public class PoisonSkillEffect extends PeriodicDamageEffect {

        public PoisonSkillEffect(final Skill skill, final long period, final long duration, final double tickDamage, final Player applier) {
            super(skill, "Poison", applier, period, duration, tickDamage);
            this.types.add(EffectType.POISON);
            this.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (20 * duration / 1000), 0), true);
        }

        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
        }

        @Override
        public void removeFromMonster(final Monster monster) {
            super.removeFromMonster(monster);
            this.broadcast(monster.getEntity().getLocation(), SkillPoison.this.expireText, CustomNameManager.getName(monster).toLowerCase());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), SkillPoison.this.expireText, player.getDisplayName());
        }
    }
}
