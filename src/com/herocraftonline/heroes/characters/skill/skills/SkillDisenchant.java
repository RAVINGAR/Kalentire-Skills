package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillDisenchant extends ActiveSkill {

    public SkillDisenchant(Heroes plugin) {
        super(plugin, "Disenchant");
        setDescription("You are able to disenchant items, returning them to normal.");
        setArgumentRange(0, 0);
        setTypes(SkillType.KNOWLEDGE, SkillType.ITEM);
        setIdentifiers("skill disenchant", "skill disench");
    }

    @SuppressWarnings("deprecation")
    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        ItemStack item = player.getItemInHand();
        if (item == null || item.getType() == Material.AIR) {
            Messaging.send(player, "You must have an item to disenchant!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        List<Enchantment> enchants = new ArrayList<Enchantment>(item.getEnchantments().keySet());
        if (enchants.isEmpty()) {
            Messaging.send(player, "That item has no enchantments!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        for (Enchantment enchant : enchants) {
            item.removeEnchantment(enchant);
        }
        player.updateInventory();
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

}
