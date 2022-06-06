package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillPiercingStrike extends TargettedSkill {

    public SkillPiercingStrike(Heroes plugin) {
        super(plugin, "PiercingStrike");
        setDescription("Extend your weapon and deliver a piercing strike, dealing $1% of your weapon damage to the target. " +
                "This attack will pierce armor.");
        setUsage("/skill piercingstrike");
        setArgumentRange(0, 0);
        setIdentifiers("skill piercingstrike");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.ARMOR_PIERCING);
    }

    @Override
    public String getDescription(Hero hero) {
        double damageMultiplier = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 1.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damageMultiplier * 100.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MAX_DISTANCE.node(), 7);
        node.set("weapons", Util.shovels);
        node.set("damage-multiplier", 1.25);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.shovels).contains(item.name())) {
            player.sendMessage("You can't piercing strike with that weapon!");
            return SkillResult.FAIL;
        }

        double damage = plugin.getDamageManager().getFlatItemDamage(hero, item);
        damage *= SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 1.0, false);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK, false);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6F, 1.6F);
        broadcastExecuteText(hero, target);

        return SkillResult.NORMAL;
    }
}