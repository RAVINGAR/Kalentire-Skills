package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SkillPickPocket extends TargettedSkill {

    private String failMessage;
    private String noisySuccessMessage;

    public SkillPickPocket(Heroes plugin) {
        super(plugin, "PickPocket");
        this.setDescription("You attempt to steal an item from your target.");
        this.setUsage("/skill pickpocket");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill pickpocket", "skill ppocket", "skill pickp");
        this.setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("base-chance", 0.1);
        node.set("chance-per-level", 0.003);
        node.set("failure-message", "%hero% failed to steal from %target%!");
        node.set("noisy-success-message", "%hero% stole %target%s %item%!");
        node.set(SkillSetting.USE_TEXT.node(), "");
        node.set("disallowed-items", Arrays.asList(""));
        node.set("always-steal-all", true);
        node.set("max-stolen", 64);
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.failMessage = SkillConfigManager.getRaw(this, "failure-message", "%hero% failed to steal from %target%!").replace("%hero%", "$1").replace("%target%", "$2");
        this.noisySuccessMessage = SkillConfigManager.getRaw(this, "noisy-success-message", "%hero% stole %target%s %item%!").replace("%hero", "$1").replace("%target%", "$2").replace("%item%", "$3");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        final Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        final Player tPlayer = (Player) target;

        final double chance = SkillConfigManager.getUseSetting(hero, this, "base-chance", 0.1, false) + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE_LEVEL, 0.02, false) * hero.getLevel(this));

        if (Util.nextRand() >= chance) {
            if (Util.nextRand() >= chance) {
                this.broadcast(player.getLocation(), this.failMessage, player.getDisplayName(), tPlayer.getDisplayName());
            }
            player.sendMessage(ChatColor.GRAY + "You failed to steal anything from " + tPlayer.getDisplayName());
            return SkillResult.NORMAL;
        }

        final Inventory tInventory = tPlayer.getInventory();
        final ItemStack[] items = tInventory.getContents();
        // Never steal items in the hotbar - We consider these 'active' and in the players hand.
        final int slot = Util.nextInt(27) + 9;
        final Set<String> disallowed = new HashSet<String>(SkillConfigManager.getUseSetting(hero, this, "disallowed-items", new ArrayList<String>()));
        if ((items[slot] == null) || (items[slot].getType() == Material.AIR) || disallowed.contains(items[slot].getType().name())) {
            player.sendMessage(ChatColor.GRAY + "You failed to steal anything from " + tPlayer.getDisplayName());
            return SkillResult.NORMAL;
        }
        // Lets make sure we don't have any setting limits.
        int stealAmount = items[slot].getAmount();
        if (!SkillConfigManager.getUseSetting(hero, this, "always-steal-all", true)) {
            final int maxSteal = SkillConfigManager.getUseSetting(hero, this, "max-stolen", 64, false);
            if (stealAmount > maxSteal) {
                stealAmount = maxSteal;
            }

            stealAmount = Util.nextInt(stealAmount) + 1;
        }

        if (stealAmount == items[slot].getAmount()) {
            tInventory.clear(slot);
        } else {
            items[slot].setAmount(stealAmount);
            tInventory.setItem(slot, items[slot]);
        }

        tPlayer.updateInventory();
        final Map<Integer, ItemStack> leftOvers = player.getInventory().addItem(items[slot]);
        if ((leftOvers != null) && !leftOvers.isEmpty()) {
            for (final ItemStack is : leftOvers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), is);
            }
        }
        player.updateInventory();
        if (Math.random() >= chance) {
            this.broadcast(player.getLocation(), this.noisySuccessMessage, player.getDisplayName(), tPlayer.getDisplayName(), items[slot].getType().name().replace("_", " ").toLowerCase());
        }

        return SkillResult.NORMAL;
        //and that's all folks!
    }

    @Override
    public String getDescription(Hero hero) {
        return this.getDescription();
    }


}
