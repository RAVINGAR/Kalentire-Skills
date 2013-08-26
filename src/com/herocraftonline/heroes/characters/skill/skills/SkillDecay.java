package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
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

public class SkillDecay extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    private String applyText;
    private String expireText;

    public SkillDecay(Heroes plugin) {
        super(plugin, "Decay");
        setDescription("You disease your target dealing $1 dark damage over $2 seconds.");
        setUsage("/skill decay");
        setArgumentRange(0, 0);
        setIdentifiers("skill decay");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 17, false);
        double tickDamageIncrease = hero.getAttributeValue(AttributeType.INTELLECT) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.17, false);
        tickDamage += tickDamageIncrease;

        String formattedDamage = Util.decFormat.format(tickDamage * ((double) duration / (double) period));
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 7);
        node.set(SkillSetting.DURATION.node(), 20000);
        node.set(SkillSetting.PERIOD.node(), 2500);
        node.set(SkillSetting.DAMAGE_TICK.node(), Double.valueOf(17));
        node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.17));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDeonoter() + "%target%'s flesh has begun to rot!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDeonoter() + "%target% is no longer decaying alive!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDeonoter() + "%target%'s flesh has begun to rot!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDeonoter() + "%target% is no longer decaying alive!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, true);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 17, false);
        double tickDamageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 0.17, false);
        tickDamage += (tickDamageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        plugin.getCharacterManager().getCharacter(target).addEffect(new DecayEffect(this, duration, period, tickDamage, player));
        broadcastExecuteText(hero, target);

        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), 
            		target.getLocation().add(0,1.5,0), 
            		FireworkEffect.builder()
            		.flicker(true).trail(false)
            		.with(FireworkEffect.Type.BALL)
            		.withColor(Color.BLACK)
            		.withFade(Color.GRAY)
            		.build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
    }

    public class DecayEffect extends PeriodicDamageEffect {

        public DecayEffect(Skill skill, long duration, long period, double tickDamage, Player applier) {
            super(skill, "Decay", period, duration, tickDamage, applier);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.DISEASE);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), applyText, Messaging.getLivingEntityName(monster));
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), expireText, Messaging.getLivingEntityName(monster).toLowerCase());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }
}
