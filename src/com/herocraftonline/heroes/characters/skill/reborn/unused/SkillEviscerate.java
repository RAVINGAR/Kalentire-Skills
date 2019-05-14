package com.herocraftonline.heroes.characters.skill.reborn.unused;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillEviscerate extends TargettedSkill {

    public SkillEviscerate(Heroes plugin) {
        super(plugin, "Eviscerate");
        setDescription("You eviscerate your target, piercing through their armor and dealing $1 physical damage.");
        setUsage("/skill eviscerate");
        setArgumentRange(0, 0);
        setIdentifiers("skill eviscerate");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.ARMOR_PIERCING);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        String formattedDamage = Util.decFormat.format(damage);

        return getDescription()
                .replace("$1", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 4);
        config.set(SkillSetting.DAMAGE.node(), 100.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.0);
        config.set("weapons", Util.swords);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords).contains(item.name())) {
            player.sendMessage("You can't Eviscerate with that weapon!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRONGOLEM_HURT, 0.4F, 2.0F);
        //player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT.value(), 0.4F, 2.0F);

        target.getWorld().spigot().playEffect(target.getLocation().add(0, 1, 0), Effect.TILE_BREAK, 115, 3, 0.4F, 0.2F, 0.4F, 0.3F, 45, 16);
//        target.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0, 1, 0), 45, 0.4, 0.2, 0.4, 0.3, Bukkit.createBlockData(Material.NETHER_WART_BLOCK));

        return SkillResult.NORMAL;
    }
}
