package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

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

public class SkillDivineStun extends TargettedSkill {

    public SkillDivineStun(Heroes plugin) {
        super(plugin, "DivineStun");
        setDescription("You stun your target for $1 seconds, preventing them from using skills or moving and dealing $2 damage");
        setUsage("/skill divinestun");
        setArgumentRange(0, 0);
        setIdentifiers("skill divinestun");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.SILENCEABLE, SkillType.DISABLING, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.INTERRUPTING);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 30, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.7, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 1500, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 30, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 6);
        node.set(SkillSetting.DURATION.node(), 1500);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 30);
        node.set(SkillSetting.DAMAGE.node(), 31);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.7);
        node.set(SkillSetting.DELAY.node(), 500);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 1500, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 30, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 30, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.7, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        plugin.getCharacterManager().getCharacter(target).addEffect(new StunEffect(this, player, duration));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENDERMAN_TELEPORT, 0.5F, 1.0F);

        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.SPELL, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 1.1, 0), org.bukkit.Effect.SPELL, 0, 0, 0, 0, 0, 1, 25, 16);
        return SkillResult.NORMAL;
    }
}
