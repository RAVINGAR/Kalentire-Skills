package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.NMSHandler;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SkillDisenchant extends ActiveSkill {

    public SkillDisenchant(Heroes plugin) {
        super(plugin, "Disenchant");
        this.setDescription("You are able to disenchant items, returning them to normal.");
        this.setArgumentRange(0, 0);
        this.setTypes(SkillType.KNOWLEDGE, SkillType.ITEM_MODIFYING);
        this.setIdentifiers("skill disenchant", "skill disench");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();
        final ItemStack item = NMSHandler.getInterface().getItemInMainHand(player.getInventory());
        if ((item == null) || (item.getType() == Material.AIR)) {
            player.sendMessage(ChatColor.GRAY + "You must have an item to disenchant!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        final List<Enchantment> enchants = new ArrayList<>(item.getEnchantments().keySet());
        if (enchants.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "That item has no enchantments!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        for (final Enchantment enchant : enchants) {
            item.removeEnchantment(enchant);
        }
        player.updateInventory();
        this.broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return this.getDescription();
    }

}
