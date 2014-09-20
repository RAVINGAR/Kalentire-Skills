package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillSoulLeech extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    private String applyText;
    private String expireText;

    public SkillSoulLeech(Heroes plugin) {
        super(plugin, "SoulLeech");
        setDescription("Leech the soul of your target, dealing $1 damage over $2 seconds. After expiring, the effect will restore your health for $3% of the damage dealt.");
        setUsage("/skill soulleech");
        setArgumentRange(0, 0);
        setIdentifiers("skill soulleech");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.DEBUFFING, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 14.0, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 0.0375, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", 0.72, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDamage = Util.decFormat.format(damage * (duration / period));
        String formattedHeal = Util.decFormat.format(healMult * 100.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration).replace("$3", formattedHeal);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8);
        node.set(SkillSetting.DAMAGE_TICK.node(), 14);
        node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 0.0375);
        node.set(SkillSetting.DURATION.node(), 20000);
        node.set(SkillSetting.PERIOD.node(), 2000);
        node.set("heal-mult", 0.72);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% is having their soul leeched by %hero%");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target%'s soul is no longer being leeched.");
        node.set(SkillSetting.DELAY.node(), 1000);

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%target% is having their soul leeched by %hero%").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%target%'s soul is no longer being leeched.").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 14.0, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 0.0375, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", 0.72, false);

        broadcastExecuteText(hero, target);

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        targetCT.addEffect(new SoulLeechEffect(this, player, period, duration, damage, healMult));

        // this is our fireworks shit
        try {
            fplayer.playFirework(target.getWorld(),
                                 target.getLocation(),
                                 FireworkEffect.builder()
                                               .flicker(false).trail(true)
                                               .with(FireworkEffect.Type.BURST)
                                               .withColor(Color.GREEN)
                                               .withFade(Color.PURPLE)
                                               .build());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
    }

    public class SoulLeechEffect extends PeriodicDamageEffect {

        private double healMult;
        private double totalDamage = 0;

        public SoulLeechEffect(Skill skill, Player applier, long period, long duration, double tickDamage, double healMult) {
            super(skill, "SoulLeeched", applier, period, duration, tickDamage, applyText, expireText);

            types.add(EffectType.HARMFUL);
            types.add(EffectType.DARK);
            types.add(EffectType.DISPELLABLE);

            this.healMult = healMult;
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);

            healApplier();
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            healApplier();
        }

        @Override
        public void tickMonster(Monster monster) {
            super.tickMonster(monster);

            totalDamage += tickDamage;
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);

            totalDamage += tickDamage;
        }

        private void healApplier() {
            Hero hero = plugin.getCharacterManager().getHero(applier);

            HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(hero, totalDamage * healMult, skill);
            plugin.getServer().getPluginManager().callEvent(hrhEvent);
            if (!hrhEvent.isCancelled()) {
                hero.heal(hrhEvent.getAmount());
            }
        }
    }
}
