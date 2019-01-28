package com.herocraftonline.heroes.characters.skill.pack4;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.nms.NMSHandler;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class SkillGift extends TargettedSkill {

    private String sendText;

    public SkillGift(Heroes plugin) {
        super(plugin, "Gift");
        setDescription("You teleport an item to your target.");
        setUsage("/skill gift <player> [amount]");
        setArgumentRange(0, 3);
        setIdentifiers("skill gift");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("max-amount", 64);
        node.set("send-text", "%hero% has sent you %amount% %item%");
        return node;
    }

    @Override
    public void init() {
        super.init();
        sendText = SkillConfigManager.getRaw(this, "send-text", "%hero% has sent you %amount% %item%");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        Player player = hero.getPlayer();
        Player reciever = (Player) target;
        ItemStack item = NMSHandler.getInterface().getItemInMainHand(player.getInventory());
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("You need to have an item in your hotbar to send!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        } else {
            item = item.clone();
        }
        
        int maxAmount = SkillConfigManager.getUseSetting(hero, this, "max-amount", 64, false);
        int amount;

        amount = item.getAmount();
        if (amount > maxAmount) {
            item.setAmount(maxAmount);
            amount = maxAmount;
        }

        if(args.length > 2) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("That's not an amount!");
                player.sendMessage(getUsage());
                return SkillResult.FAIL;
            }
            if (amount > maxAmount) {
                player.sendMessage("You can only send up to " + maxAmount + " at a time");
                return SkillResult.FAIL;
            }
            item.setAmount(amount);
            if (item.getAmount() > maxAmount)
                item.setAmount(maxAmount);
        }


        if(NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getAmount() < item.getAmount()) {
            player.sendMessage("You aren't holding enough to send that amount!");
            return new SkillResult(ResultType.MISSING_REAGENT, false);
        }

        player.getInventory().removeItem(item);
        Map<Integer, ItemStack> leftOvers = reciever.getInventory().addItem(item);
        reciever.sendMessage(sendText.replace("%hero%", player.getName()).replace("%amount%", "" + amount).replace("%item%", item.getType().name().toLowerCase().replace("_", " ")));
        if (!leftOvers.isEmpty()) {
            for (ItemStack leftOver : leftOvers.values()) {
                reciever.getWorld().dropItem(reciever.getLocation(), leftOver);
            }
            reciever.sendMessage("Some items fall at your feet!");
        }

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

}
