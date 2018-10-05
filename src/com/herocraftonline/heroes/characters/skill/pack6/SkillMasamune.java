package com.herocraftonline.heroes.characters.skill.pack6;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.effects.common.SpeedEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;

public class SkillMasamune extends ActiveSkill {

    public SkillMasamune(Heroes plugin) {
        super(plugin, "Masamune");
        // setDescription("Increase movement speed for $1 seconds and heal yourself for $2 every $3 seconds for $4 seconds. Costs $5% of your sword.");
        setDescription("Increase movement speed for $1 seconds. Costs $2% of your sword.");
        setUsage("/skill masamune");
        setArgumentRange(0, 0);
        setIdentifiers("skill masamune");
        setTypes(SkillType.HEALING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.MOVEMENT_INCREASING);
    }

    @Override
    public String getDescription(Hero hero) {

        int speedDuration = SkillConfigManager.getUseSetting(hero, this, "speed-duration", 30000, false);
        /*double healingPerTick = SkillConfigManager.getUseSetting(hero, this, "healing-per-tick", 20, false);
        healingPerTick += SkillConfigManager.getUseSetting(hero, this, "healing-per-intellect", 2, false);
        long healingPeriod = SkillConfigManager.getUseSetting(hero, this, "healing-period", 1000, false);
        long healingDuration = SkillConfigManager.getUseSetting(hero, this, "healing-duration", 10000, false);*/

        int swordSacrificePercent = SkillConfigManager.getUseSetting(hero, this, "sword-sacrifice-percent", 20, false);

        //return getDescription().replace("$1", (speedDuration / 1000) + "").replace("$2", healingPerTick + "").replace("$3", healingPeriod + "").replace("$4", healingDuration + "").replace("$5", swordSacrificePercent + "");
        return getDescription().replace("$1", (speedDuration / 1000) + "").replace("$2", swordSacrificePercent + "");

    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("sword-sacrifice-percent", 20);
        node.set("speed-duration", 30000);
        node.set("speed-amplitude", 2);
        /*node.set("healing-duration", 10000);
        node.set("healing-period", 1000);
        node.set("healing-per-tick", 20);
        node.set("healing-per-intellect", 2);*/

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords).contains(item.name())) {
            player.sendMessage("You can't use Masamune with that weapon!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int duraPercent = SkillConfigManager.getUseSetting(hero, this, "sword-sacrifice-percent", 20, false);
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
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5F, 1.0F);
            } else {
                player.sendMessage("Your Katana doesn't have enough durability to use Masamune!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
        }

        broadcastExecuteText(hero);

        long speedDuration = SkillConfigManager.getUseSetting(hero, this, "speed-duration", 30000, false);
        int speedAmplifier = SkillConfigManager.getUseSetting(hero, this, "speed-amplitude", 3, false);

        /*double healingPerTick = SkillConfigManager.getUseSetting(hero, this, "healing-per-tick", 20, false);
        healingPerTick += SkillConfigManager.getUseSetting(hero, this, "healing-per-intellect", 2, false);
        long healingPeriod = SkillConfigManager.getUseSetting(hero, this, "healing-period", 1000, false);
        long healingDuration = SkillConfigManager.getUseSetting(hero, this, "healing-duration", 10000, false);

        PeriodicHealEffect healEffect = new PeriodicHealEffect(this, "MasamuneHealEffect", player, healingPeriod, healingDuration, healingPerTick);*/
        SpeedEffect slowEffect = new SpeedEffect(this, "MasamuneSpeedEffect", player, speedDuration, speedAmplifier, null, null);

        // hero.addEffect(healEffect);
        hero.addEffect(slowEffect);        

        //player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.5, 0), org.bukkit.Effect.CLOUD, 0, 0, 0, 0, 0, 1, 150, 16);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.5, 0), 150, 0, 0, 0, 1);

        return SkillResult.NORMAL;
    }
}
