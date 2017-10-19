package com.herocraftonline.heroes.characters.skill.unusedskills;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Messaging;

public class SkillConsume extends ActiveSkill {

    public SkillConsume(Heroes plugin) {
        super(plugin, "Consume");
        setDescription("You consume an item for mana.");
        setUsage("/skill consume <item>");
        setArgumentRange(1, 1);
        setIdentifiers("skill consume");
        setTypes(SkillType.ITEM_DESTRUCTION, SkillType.MANA_INCREASING);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        String root = "BONE";
        node.set(root + "." + SkillSetting.LEVEL.node(), 1);
        node.set(root + "." + SkillSetting.MANA.node(), 20);
        node.set(root + "." + SkillSetting.AMOUNT.node(), 1);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        if (hero.getMana() >= hero.getMaxMana()) {
            Messaging.send(player, "Your mana is already full!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        List<String> keys = SkillConfigManager.getUseSettingKeys(hero, this);
        if (keys == null || keys.isEmpty())
            return SkillResult.FAIL;

        for (String key : keys) {
            if (key.toUpperCase().equals(args[0].toUpperCase())) {
                Material mat = Material.matchMaterial(key);
                if (mat == null)
                    throw new IllegalArgumentException("Invalid Configuration for Skill Consume: " + key + " is not a valid Material");

                int amount = SkillConfigManager.getUseSetting(hero, this, key + "." + SkillSetting.AMOUNT, 1, true);
                if (amount < 1)
                    throw new IllegalArgumentException("Invalid Configuration for Skill Consume: " + key + " has invalid amount defined");

                int level = SkillConfigManager.getUseSetting(hero, this, key + "." + SkillSetting.LEVEL, 1, true);
                if (hero.getLevel(this) < level) {
                    return new SkillResult(ResultType.LOW_LEVEL, true, level);
                }

                ItemStack reagent = new ItemStack(mat, amount);
                if (!hasReagentCost(player, reagent)) {
                    String reagentName = reagent.getType().name().toLowerCase().replace("_", " ");
                    return new SkillResult(ResultType.MISSING_REAGENT, true, reagent.getAmount(), reagentName);
                }

                
                int mana = SkillConfigManager.getUseSetting(hero, this, key + "." + SkillSetting.MANA, 20, false);
                HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, mana, this);
                plugin.getServer().getPluginManager().callEvent(hrmEvent);
                if (hrmEvent.isCancelled())
                    return SkillResult.CANCELLED;

                player.getInventory().removeItem(reagent);
                player.updateInventory();
                hero.setMana(hrmEvent.getDelta() + hero.getMana());
                if (hero.isVerboseMana()) {
                    Messaging.send(player, ChatComponents.Bars.mana(hero.getMana(), hero.getMaxMana(), false));
                } else {
                    Messaging.send(player, "You regain " + mana + " mana");
                }

                broadcastExecuteText(hero);
                return SkillResult.NORMAL;
            }
        }

        Messaging.send(player, "You can't consume that item!");
        return SkillResult.FAIL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
