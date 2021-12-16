package com.herocraftonline.heroes.characters.skill.remastered;

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
        setDescription("Leech the soul of your target, dealing $1 damage over $2 second(s). After expiring, the " +
                "effect will restore your health for $3% of the damage dealt.");
        setUsage("/skill soulleech");
        setIdentifiers("skill soulleech");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE,
                SkillType.DEBUFFING, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.DURATION, false);
        int period = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.PERIOD, false);

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE_TICK, false);
        double healMult = SkillConfigManager.getUseSettingDouble(hero, this, "heal-mult", false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage * ((double)duration / period)))
                .replace("$2", Util.decFormat.format(duration / 1000.0))
                .replace("$3", Util.decFormat.format(healMult * 100.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();

        config.set(SkillSetting.MAX_DISTANCE.node(), 8);
        config.set(SkillSetting.DAMAGE_TICK.node(), 14.0);
        config.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 0.0375);
        config.set(SkillSetting.DURATION.node(), 20000);
        config.set(SkillSetting.PERIOD.node(), 2000);
        config.set("heal-mult", 0.72);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is having their soul leeched by %hero%");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s soul is no longer being leeched.");
        config.set(SkillSetting.DELAY.node(), 1000);
        config.set("volume", 0.8F);
        config.set("pitch", 1.0F);

        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% is having their soul leeched by %hero%")
                .replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%target%'s soul is no longer being leeched.").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.DURATION, false);
        int period = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.PERIOD, false);

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE_TICK, false);
        double healMult = SkillConfigManager.getUseSettingDouble(hero, this, "heal-mult", false);

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

        public SoulLeechEffect(Skill skill, Player applier, long period, long duration, double tickDamage,
                               double healMult, float soundVolume, float soundPitch) {
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

            final LivingEntity entity = monster.getEntity();
            if (entity.isDead() || entity.getHealth() <= 0) {
                expire();
                return;
            }

            totalDamage += tickDamage;
            playSound(entity.getLocation());
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
            final Player player = hero.getPlayer();
            if (player.isDead() || player.getHealth() <= 0) {
                expire();
                return;
            }

            totalDamage += tickDamage;
            playSound(player.getLocation());
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
