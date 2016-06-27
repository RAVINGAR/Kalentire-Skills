package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Util;

public class SkillEmpathy extends TargettedSkill {

    public SkillEmpathy(Heroes plugin) {
        super(plugin, "Empathy");
        setDescription("You deal up to $1 dark damage equal to $2% of your missing health and slow the target for $3 seconds.");
        setUsage("/skill empathy");
        setArgumentRange(0, 0);
        setIdentifiers("skill empathy");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DARK, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        double maxDamage = SkillConfigManager.getUseSetting(hero, this, "max-damage", 152, false);
        double maxDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "max-damage-increase-per-intellect", 1.0, false);
        maxDamage += maxDamageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        double modifier = SkillConfigManager.getUseSetting(hero, this, "damage-modifier", 0.5, false);
        double modifierIncrease = SkillConfigManager.getUseSetting(hero, this, "damage-modifier-increase-per-intellect", 0.0, false);
        modifier += (modifierIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int slowDuration = SkillConfigManager.getUseSetting(hero, this, "slow-duration", 4000, false);

        String formattedModifier = Util.decFormat.format(modifier * 100.0);
        String formattedSlowDuration = Util.decFormat.format(slowDuration / 1000.0);

        return getDescription().replace("$1", maxDamage + "").replace("$2", formattedModifier).replace("$3", formattedSlowDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set("max-damage", 152);
        node.set("max-damage-increase-per-intellect", 1.0);
        node.set("damage-modifier", 1.0);
        node.set("damage-modifier-increase-per-intellect", 0.0);
        node.set("slow-duration", 4000);
        node.set("slow-amplifier", 2);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (target.equals(player)) {
            return SkillResult.INVALID_TARGET;
        }

        double maxDamage = SkillConfigManager.getUseSetting(hero, this, "max-damage", 152, false);
        double maxDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "max-damage-increase-per-intellect", 1.0, false);
        maxDamage += maxDamageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        double modifier = SkillConfigManager.getUseSetting(hero, this, "damage-modifier", 1.0, false);
        double modifierIncrease = SkillConfigManager.getUseSetting(hero, this, "damage-modifier-increase-per-intellect", 0.0, false);
        modifier += modifierIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        double damage = (player.getMaxHealth() - player.getHealth()) * modifier;

        if (damage > maxDamage)
            damage = maxDamage;

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        int slowDuration = SkillConfigManager.getUseSetting(hero, this, "slow-duration", 4000, false);
        int amplifier = SkillConfigManager.getUseSetting(hero, this, "amplifier", 2, false);

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        targetCT.addEffect(new SlowEffect(this, player, slowDuration, amplifier, null, null));

        broadcastExecuteText(hero, target);
        
        target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.4, 0), Effect.WITCH_MAGIC, 0, 0, 0.3F, 0.3F, 0.3F, 0.5F, 45, 16);
        target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.4, 0), Effect.SPELL, 0, 0, 0.3F, 0.3F, 0.3F, 0.0F, 15, 16);

        return SkillResult.NORMAL;
    }

}