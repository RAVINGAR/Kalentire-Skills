package com.herocraftonline.heroes.characters.skill.remastered.shadowknight;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillSoulLeech extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillSoulLeech(Heroes plugin) {
        super(plugin, "SoulLeech");
        setDescription("Leech the soul of your target, dealing $1 damage over $2 second(s). After expiring, the effect will restore your health for $3% of the damage dealt.");
        setUsage("/skill soulleech");
        setIdentifiers("skill soulleech");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.DEBUFFING, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE_TICK, false);
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
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is having their soul leeched by %hero%");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s soul is no longer being leeched.");
        node.set(SkillSetting.DELAY.node(), 1000);
        node.set("volume", 0.8F);
        node.set("pitch", 1.0F);

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% is having their soul leeched by %hero%").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s soul is no longer being leeched.").replace("%target%", "$1");
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

        float soundVolume = (float) SkillConfigManager.getUseSetting(hero, this, "volume", 0.8F, false);
        float soundPitch = (float) SkillConfigManager.getUseSetting(hero, this, "pitch", 1.0F, false);

        broadcastExecuteText(hero, target);

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        targetCT.addEffect(new SoulLeechEffect(this, player, period, duration, damage, healMult, soundVolume, soundPitch));

        return SkillResult.NORMAL;
    }

    public class SoulLeechEffect extends PeriodicDamageEffect {
        private final float soundVolume;
        private final float soundPitch;
        private double healMult;
        private double totalDamage = 0;

        public SoulLeechEffect(Skill skill, Player applier, long period, long duration, double tickDamage, double healMult, float soundVolume, float soundPitch) {
            super(skill, "SoulLeeched", applier, period, duration, tickDamage, applyText, expireText);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.DARK);
            types.add(EffectType.DISPELLABLE);

            this.healMult = healMult;
            this.soundVolume = soundVolume;
            this.soundPitch = soundPitch;
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
            playSound(monster.getEntity().getLocation());
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);

            totalDamage += tickDamage;
            playSound(hero.getPlayer().getLocation());
        }

        private void healApplier() {
            Hero hero = plugin.getCharacterManager().getHero(applier);

            HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(hero, totalDamage * healMult, skill);
            plugin.getServer().getPluginManager().callEvent(hrhEvent);
            if (!hrhEvent.isCancelled()) {
                hero.heal(hrhEvent.getDelta());
            }
        }

        private void playSound(Location location) {
            if (soundVolume > 0) {
                location.getWorld().playSound(location, Sound.ENTITY_BAT_DEATH, soundVolume, soundPitch);
            }
        }
    }
}
