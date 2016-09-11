package com.herocraftonline.heroes.characters.skill.unusedskills;

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
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillPoison extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    private String applyText;
    private String expireText;

    public SkillPoison(Heroes plugin) {
        super(plugin, "Poison");
        setDescription("You poison your target dealing $1 damage over $2 seconds.");
        setUsage("/skill poison");
        setArgumentRange(0, 0);
        setIdentifiers("skill poison");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_POISON, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 15000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 6, false);
        double tickDamageIncrease = hero.getAttributeValue(AttributeType.INTELLECT) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.075, false);
        tickDamage += tickDamageIncrease;

        String formattedDamage = Util.decFormat.format(tickDamage * ((double) duration / (double) period));
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
        node.set(SkillSetting.DURATION.node(), 15000);
        node.set(SkillSetting.PERIOD.node(), 1000);
        node.set(SkillSetting.DAMAGE_TICK.node(), 6);
        node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 0.075);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target%'s is poisoned!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% has recovered from the poison!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%target%'s is poisoned!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%target% has recovered from the poison!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 15000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, true);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 6, false);
        double tickDamageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 0.075, false);
        tickDamage += (tickDamageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        plugin.getCharacterManager().getCharacter(target).addEffect(new PoisonSkillEffect(this, player, period, duration, tickDamage));

        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0, 1.5, 0),
                                 FireworkEffect.builder().flicker(false).trail(false)
                                               .with(FireworkEffect.Type.BALL)
                                               .withColor(Color.OLIVE)
                                               .withFade(Color.LIME)
                                               .build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_ZOMBIE_INFECT.value(), 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class PoisonSkillEffect extends PeriodicDamageEffect {

        public PoisonSkillEffect(Skill skill, Player applier, long period, long duration, double tickDamage) {
            super(skill, "Poison", applier, period, duration, tickDamage);

            types.add(EffectType.POISON);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.DISPELLABLE);

            addMobEffect(19, (int) ((duration / 1000.0) * 20), 0, true);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, Messaging.getLivingEntityName(monster));
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }
    }
}
