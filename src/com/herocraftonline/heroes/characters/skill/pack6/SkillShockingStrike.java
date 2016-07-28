package com.herocraftonline.heroes.characters.skill.pack6;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

// import org.bukkit.Effect;

public class SkillShockingStrike extends TargettedSkill {

    public SkillShockingStrike(Heroes plugin) {
        super(plugin, "ShockingStrike");
        setDescription("Deliver a shocking strike to your target, dealing $1 physical damage and interrupting their casting.");
        setUsage("/skill shockingstrike");
        setArgumentRange(0, 0);
        setIdentifiers("skill shockingstrike");
        // Lightning used because no "electric" or "shocking" property, but it doesn't bolt.
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.ABILITY_PROPERTY_LIGHTNING, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.INTERRUPTING);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.6, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MAX_DISTANCE.node(), 3);
        node.set("weapons", Util.axes);
        node.set(SkillSetting.DAMAGE.node(), 30);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.7);
        node.set("lightning-volume", 0.0F);
        
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        Material item = player.getItemInHand().getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.axes).contains(item.name())) {
            Messaging.send(player, "You can't use Shocking Strike with that weapon!");
            return SkillResult.FAIL;
        }

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 30, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.7, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        float lightningVolume = (float) SkillConfigManager.getUseSetting(hero, this, "lightning-volume", 0.0F, false);
        
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        // Strike some lightning
        target.getWorld().spigot().strikeLightningEffect(target.getLocation(), true);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_THUNDER, lightningVolume, 1.0F);
        
        // We have actual lightning now, so the effect isn't needed.
        /* This is the new Particle API system for Spigot - the first few int = id, data, offsetX/Y/Z, speed, count, radius)
         * offset controls how spread out the particles are
         * id and data only work for two particles: ITEM_BREAK and TILE_BREAK
         * */
        /*player.getWorld().spigot().playEffect(target.getLocation().add(0, 1.0, 0), Effect.SNOW_SHOVEL, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 1.0, 0), Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);        
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_THUNDER, 0.7F, 1.0F);*/
        broadcastExecuteText(hero, target);

        return SkillResult.NORMAL;
    }
}