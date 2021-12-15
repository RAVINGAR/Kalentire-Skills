package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.characters.CharacterDamageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.inventory.ItemStack;


public class SkillCleave extends TargettedSkill {

    public SkillCleave(Heroes plugin) {
        super(plugin, "Cleave");
        setDescription("You cleave your target and nearby enemies for $1% weapon damage.");
        setUsage("/skill cleave");
        setArgumentRange(0, 0);
        setIdentifiers("skill cleave");
        setTypes(SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_PHYSICAL);
    }

    @Override
    public String getDescription(Hero hero) {
        double damageMultiplier = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 1.0, false);

        String formattedDamageMultiplier = Util.decFormat.format(damageMultiplier * 100.0);

        return getDescription().replace("$1", formattedDamageMultiplier);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set("weapons", Util.swords);
        node.set(SkillSetting.RADIUS.node(), 3);
        node.set("damage-multiplier", 0.75);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        ItemStack item = player.getInventory().getItemInMainHand();
        Material itemType = item.getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords).contains(itemType.toString())) {
            player.sendMessage("You can't cleave with that weapon!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero, target);

        CharacterDamageManager manager = plugin.getDamageManager();
        double damage = manager.getHighestItemDamage(hero, itemType);
        damage *= SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 1.0, false);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3, false);
        for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity)) {
                continue;
            }

            addSpellTarget(entity, hero);
            damageEntity((LivingEntity) entity, player, damage, DamageCause.ENTITY_ATTACK);
            //entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 0.5, 0), org.bukkit.Effect.TILE_BREAK, 115, 3, 0.3F, 0.2F, 0.3F, 0.5F, 5, 16);
            entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, entity.getLocation().add(0, 0.5, 0), 5, 0.3, 0.2, 0.3, 0.5, Bukkit.createBlockData(Material.NETHER_WART_BLOCK));
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.0F);
        //target.getWorld().spigot().playEffect(target.getLocation(), org.bukkit.Effect.CRIT, 0, 0, 1.5F, 1.0F, 1.5F, 0.4F, 45, 16);
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 0.5, 0), 45, 1.5, 1.5, 1.5, 0.4);
        //target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.TILE_BREAK, 115, 3, 0.3F, 0.2F, 0.3F, 0.5F, 45, 16);
        target.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0, 0.5, 0), 45, 0.3, 0.2, 0.3, 0.5, Bukkit.createBlockData(Material.NETHER_WART_BLOCK));

        return SkillResult.NORMAL;
    }
}
