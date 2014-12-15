package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillMurasame extends TargettedSkill {

    public SkillMurasame(Heroes plugin) {
        super(plugin, "Murasame");
        setDescription("Deal $1 physical damage and heal yourself for $2% of your total health. Costs $3% of your sword.");
        setUsage("/skill murasame");
        setArgumentRange(0, 0);
        setIdentifiers("skill murasame");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.MOVEMENT_SLOWING);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.6, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        int healingPercent = SkillConfigManager.getUseSetting(hero, this, "healing-percent", 25, false);

        int swordSacrificePercent = SkillConfigManager.getUseSetting(hero, this, "sword-sacrifice-percent", 10, false);

        return getDescription().replace("$1", damage + "").replace("$2", healingPercent + "").replace("$3", swordSacrificePercent + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.6);
        node.set("sword-sacrifice-percent", 10);
        node.set("healing-percent", 25);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!damageCheck(player, target)) {
            Messaging.send(player, "You can't damage that target!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Material item = player.getItemInHand().getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords).contains(item.name())) {
            Messaging.send(player, "You can't use Murasame with that weapon!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        short dura = player.getItemInHand().getDurability();
        short maxDura = player.getItemInHand().getType().getMaxDurability();
        short duraCost = (short) (maxDura * (SkillConfigManager.getUseSetting(hero, this, "sword-sacrifice-percent", 10, false) * 0.01));

        if(dura == (short)0) {
            player.getItemInHand().setDurability((short) (duraCost));
        }
        else if(maxDura - dura > duraCost) {
            player.getItemInHand().setDurability((short) (dura + duraCost));
        }
        else if(maxDura - dura == duraCost) {
            player.setItemInHand(null);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_BREAK, 0.5F, 1.0F);
        }
        else {
            Messaging.send(player, "Your Katana doesn't have enough durability to use Murasame!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.6, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        int healingPercent = SkillConfigManager.getUseSetting(hero, this, "healing-percent", 25, false);

        if (damage > 0) {
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC, false);
        }

        if (healingPercent > 0) {

            double healAmount = player.getMaxHealth() * (healingPercent / 100);
            
            HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(hero, healAmount, this, hero);
            Bukkit.getPluginManager().callEvent(hrhEvent);
            if(!hrhEvent.isCancelled()) {
                hero.heal(hrhEvent.getAmount());
            }
        }

        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.VILLAGER_THUNDERCLOUD, 0, 0, 0, 0, 0, 1, 150, 16);

        return SkillResult.NORMAL;
    }
}
