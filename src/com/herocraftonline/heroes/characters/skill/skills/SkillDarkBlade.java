package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.WitheringEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillDarkBlade extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillDarkBlade(Heroes plugin) {
        super(plugin, "DarkBlade");
        setDescription("Strike your target with a blade of dark, dealing $1 physical damage, draining $2 of their mana, and giving it to you.");
        setUsage("/skill darkblade");
        setArgumentRange(0, 0);
        setIdentifiers("skill darkblade");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.SILENCEABLE, SkillType.DAMAGING,
                SkillType.AGGRESSIVE, SkillType.MANA_INCREASING, SkillType.MANA_DECREASING);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(98), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        int manaDrain = SkillConfigManager.getUseSetting(hero, this, "mana-drain", Integer.valueOf(100), false);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedManaDrain = Util.decFormat.format(manaDrain);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedManaDrain);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set(SkillSetting.DAMAGE.node(), 85);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.75);
        node.set("mana-drain", 100);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(98), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        broadcastExecuteText(hero, target);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);
        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        targetCT.addEffect(new WitheringEffect(this, player, 1000, 0, null, null));

        int manaDrain = SkillConfigManager.getUseSetting(hero, this, "mana-drain", Integer.valueOf(100), false);

        // If the target is a player, drain their mana
        if ((target instanceof Player)) {
            // Get the target hero
            Hero tHero = plugin.getCharacterManager().getHero((Player) target);

            if (tHero.getMana() > manaDrain) {
                int newMana = tHero.getMana() - manaDrain;
                tHero.setMana(newMana);
            }
            else {
                tHero.setMana(0);
            }

            if (tHero.isVerboseMana())
                Messaging.send(player, Messaging.createManaBar(tHero.getMana(), tHero.getMaxMana()));
        }

        HeroRegainManaEvent hrEvent = new HeroRegainManaEvent(hero, manaDrain, this);
        plugin.getServer().getPluginManager().callEvent(hrEvent);
        if (!hrEvent.isCancelled()) {
            hero.setMana(hrEvent.getAmount() + hero.getMana());

            if (hero.isVerboseMana())
                Messaging.send(player, Messaging.createManaBar(hero.getMana(), hero.getMaxMana()));
        }

        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(),
                                 target.getLocation(),
                                 FireworkEffect.builder().
                                               flicker(false).trail(false)
                                               .with(FireworkEffect.Type.BURST)
                                               .withColor(Color.BLACK)
                                               .withFade(Color.PURPLE)
                                               .build());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
    }
}