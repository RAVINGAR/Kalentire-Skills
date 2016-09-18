package com.herocraftonline.heroes.characters.skill.pack2;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillPiercingStrike extends TargettedSkill {

    public SkillPiercingStrike(Heroes plugin) {
        super(plugin, "PiercingStrike");
        setDescription("Extend your weapon and deliver a piercing strike, dealing $1% of your weapon damage to the target. This attack will pierce armor.");
        setUsage("/skill piercingstrike");
        setArgumentRange(0, 0);
        setIdentifiers("skill piercingstrike");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE);
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

        node.set(SkillSetting.MAX_DISTANCE.node(), 7);
        node.set("weapons", Util.shovels);
        node.set("damage-multiplier", 0.5);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.shovels).contains(item.name())) {
            Messaging.send(player, "You can't piercing strike with that weapon!");
            return SkillResult.FAIL;
        }

        double damage = plugin.getDamageManager().getHighestItemDamage(hero, item);
        damage *= SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 1.0, false);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC, false);

        player.getWorld().playSound(player.getLocation(), CompatSound.BLOCK_ANVIL_LAND.value(), 0.6F, 1.6F);
        broadcastExecuteText(hero, target);

        return SkillResult.NORMAL;
    }
}