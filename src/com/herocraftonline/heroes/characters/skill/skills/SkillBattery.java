package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillBattery extends TargettedSkill {

    public SkillBattery(Heroes plugin) {
        super(plugin, "Battery");
        setDescription("You grant $1 of your mana to your target.");
        setUsage("/skill battery");
        setArgumentRange(0, 1);
        setTypes(SkillType.SILENCEABLE, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.MANA_DECREASING,
                SkillType.MANA_INCREASING, SkillType.ABILITY_PROPERTY_DARK);
        setIdentifiers("skill battery");
    }

    @Override
    public String getDescription(Hero hero) {
        int amount = SkillConfigManager.getUseSetting(hero, this, "transfer-amount", 20, false);

        return getDescription().replace("$1", amount + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set("transfer-amount", 150);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player) || player.equals(target))
            return SkillResult.INVALID_TARGET;

        Hero tHero = plugin.getCharacterManager().getHero((Player) target);

        int transferAmount = SkillConfigManager.getUseSetting(hero, this, "transfer-amount", 150, false);
        if (hero.getMana() > transferAmount) {
            broadcastExecuteText(hero, target);

            if (tHero.getMana() + transferAmount > tHero.getMaxMana()) {
                transferAmount = tHero.getMaxMana() - tHero.getMana();
            }

            HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(tHero, transferAmount, this);
            plugin.getServer().getPluginManager().callEvent(hrmEvent);
            if (hrmEvent.isCancelled()) {
                return SkillResult.CANCELLED;
            }

            int finalTransferAmount = hrmEvent.getDelta();

            hero.setMana(hero.getMana() - finalTransferAmount);
            tHero.setMana(tHero.getMana() + finalTransferAmount);

            player.getWorld().playEffect(player.getLocation(), Effect.EXTINGUISH, 3);
            player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_EXPERIENCE_ORB_PICKUP.value(), 0.5F, 1.0F);
            target.getWorld().spigot().playEffect(target.getLocation(), Effect.WITCH_MAGIC, 1, 1, 0F, 1F, 0F, 10F, 55, 10);
            return SkillResult.NORMAL;
        }
        else {
            Messaging.send(hero.getPlayer(), "You need at least $1 mana to transfer.", transferAmount);

            return new SkillResult(ResultType.LOW_MANA, false);
        }
    }
}
