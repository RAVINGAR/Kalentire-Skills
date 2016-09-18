package com.herocraftonline.heroes.characters.skill.pack6;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillMurasame extends TargettedSkill {

    public SkillMurasame(Heroes plugin) {
        super(plugin, "Murasame");
        setDescription("Deal $1 magical damage that interrupts casting and heal yourself for $2% of your total health. Costs $3% of your sword.");
        setUsage("/skill murasame");
        setArgumentRange(0, 0);
        setIdentifiers("skill murasame");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.INTERRUPTING);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.6, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int healingPercent = SkillConfigManager.getUseSetting(hero, this, "healing-percent", 25, false);

        int swordSacrificePercent = SkillConfigManager.getUseSetting(hero, this, "sword-sacrifice-percent", 10, false);

        return getDescription().replace("$1", damage + "").replace("$2", healingPercent + "").replace("$3", swordSacrificePercent + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.6);
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

        Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords).contains(item.name())) {
            Messaging.send(player, "You can't use Murasame with that weapon!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int duraPercent = SkillConfigManager.getUseSetting(hero, this, "sword-sacrifice-percent", 10, false);
        if (duraPercent > 0) {
            short dura = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getDurability();
            short maxDura = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType().getMaxDurability();
            short duraCost = (short) (maxDura * (duraPercent * 0.01));

            if (dura == (short) 0) {
                NMSHandler.getInterface().getItemInMainHand(player.getInventory()).setDurability((short) (duraCost));
            } else if (maxDura - dura > duraCost) {
                NMSHandler.getInterface().getItemInMainHand(player.getInventory()).setDurability((short) (dura + duraCost));
            } else if (maxDura - dura == duraCost) {
                NMSHandler.getInterface().setItemInMainHand(player.getInventory(), null);
                player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_ITEM_BREAK.value(), 0.5F, 1.0F);
            } else {
                Messaging.send(player, "Your Katana doesn't have enough durability to use Murasame!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
        }

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.6, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        int healingPercent = SkillConfigManager.getUseSetting(hero, this, "healing-percent", 25, false);

        if (damage > 0) {
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC, false);
        }

        if (healingPercent > 0) {

            double healAmount = player.getMaxHealth() * (healingPercent / 100);

            // No selfheal nerf since we have a number preset
            HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(hero, healAmount, this);
            Bukkit.getPluginManager().callEvent(hrhEvent);
            if(!hrhEvent.isCancelled()) {
                hero.heal(hrhEvent.getAmount());
            }
        }

        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.VILLAGER_THUNDERCLOUD, 0, 0, 0, 0, 0, 1, 150, 16);

        return SkillResult.NORMAL;
    }
}