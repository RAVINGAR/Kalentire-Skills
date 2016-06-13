package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillGreatCombustion extends TargettedSkill {

    public SkillGreatCombustion(Heroes plugin) {
        super(plugin, "GreatCombustion");
        setDescription("Unleash a mass of condensed flame on your target, dealing $1 damage and stunning for $2 seconds.");
        setUsage("/skill greatcombustion");
        setArgumentRange(0, 0);
        setIdentifiers("skill greatcombustion");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.DISABLING, SkillType.INTERRUPTING);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
        damage += hero.getAttributeValue(AttributeType.INTELLECT) * damageIncrease;

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 750, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 32, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        String formattedDamage = Util.decFormat.format(damage);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set(SkillSetting.DURATION.node(), 750);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 32);
        node.set(SkillSetting.DAMAGE.node(), 100);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.875);
        node.set(SkillSetting.DELAY.node(), 1500);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 750, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 32, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        plugin.getCharacterManager().getCharacter(target).addEffect(new StunEffect(this, player, duration));

        // These effect offsets might require adjustment due to switching from a Block to a Player for relative location
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 1.0, 0), Effect.LAVA_POP, 0, 0, 0, 0, 0, 1, 135, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.MOBSPAWNER_FLAMES, 0, 0, 0, 0, 0, 0, 8, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.EXPLOSION_LARGE, 0, 0, 0, 0, 0, 0, 3, 16);
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 6.0F, 1);

        return SkillResult.NORMAL;
    }
}
