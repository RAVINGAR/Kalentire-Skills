package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillPickPocket extends TargettedSkill {
    private String failMessage;
    private String noisySuccessMessage;

    public SkillPickPocket(Heroes plugin) {
        super(plugin, "PickPocket");
        setDescription("You attempt to steal an item from your target.");
        setUsage("/skill pickpocket");
        setArgumentRange(0, 0);
        setIdentifiers("skill pickpocket", "skill ppocket", "skill pickp");
        setTypes(SkillType.PHYSICAL, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("base-chance", 0.1);
        node.set("chance-per-level", 0.003);
        node.set("failure-message", "%hero% failed to steal from %target%!");
        node.set("noisy-success-message", "%hero% stole %target%s %item%!");
        node.set(Setting.USE_TEXT.node(), "");
        node.set("disallowed-items", Arrays.asList(""));
        node.set("always-steal-all", true);
        node.set("max-stolen", 64);
        return node;
    }

    @Override
    public void init() {
        super.init();
        failMessage = SkillConfigManager.getRaw(this, "failure-message", "%hero% failed to steal from %target%!").replace("%hero%", "$1").replace("%target%", "$2");
        noisySuccessMessage = SkillConfigManager.getRaw(this, "noisy-success-message", "%hero% stole %target%s %item%!").replace("%hero", "$1").replace("%target%", "$2").replace("%item%", "$3");
    }

    @SuppressWarnings("deprecation")
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        Player tPlayer = (Player) target;        

        double chance = SkillConfigManager.getUseSetting(hero, this, "base-chance", 0.1, false) + (SkillConfigManager.getUseSetting(hero, this, Setting.CHANCE_LEVEL, 0.02, false) * hero.getSkillLevel(this));

        if (Util.rand.nextDouble() >= chance) {
            if (Util.rand.nextDouble() >= chance) {
                broadcast(player.getLocation(), failMessage, player.getDisplayName(), tPlayer.getDisplayName());
            }
            Messaging.send(player, "You failed to steal anything from $1", tPlayer.getDisplayName());
            return SkillResult.NORMAL;
        }

        Inventory tInventory = tPlayer.getInventory();
        ItemStack[] items = tInventory.getContents();
        // Never steal items in the hotbar - We consider these 'active' and in the players hand.
        int slot = Util.rand.nextInt(27) + 9;
        Set<String> disallowed = new HashSet<String>(SkillConfigManager.getUseSetting(hero, this, "disallowed-items", new ArrayList<String>()));
        if (items[slot] == null || items[slot].getType() == Material.AIR || disallowed.contains(items[slot].getType().name())) {
            Messaging.send(player, "You failed to steal anything from $1", tPlayer.getDisplayName());
            return SkillResult.NORMAL;
        }
        // Lets make sure we don't have any setting limits.
        int stealAmount = items[slot].getAmount();
        if (!SkillConfigManager.getUseSetting(hero, this, "always-steal-all", true)) {
            int maxSteal = SkillConfigManager.getUseSetting(hero, this, "max-stolen", 64, false);
            if (stealAmount > maxSteal)
                stealAmount = maxSteal;

            stealAmount = Util.rand.nextInt(stealAmount) + 1;
        }
        
        if (stealAmount == items[slot].getAmount())
            tInventory.clear(slot);
        else {
            items[slot].setAmount(stealAmount);
            tInventory.setItem(slot, items[slot]);
        }
        
        tPlayer.updateInventory();
        Map<Integer, ItemStack> leftOvers = player.getInventory().addItem(items[slot]);
        if (leftOvers != null && !leftOvers.isEmpty()) {
            for (ItemStack is : leftOvers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), is);
            }
        }
        player.updateInventory();
        if (Math.random() >= chance)
            broadcast(player.getLocation(), noisySuccessMessage, player.getDisplayName(), tPlayer.getDisplayName(), items[slot].getType().name().replace("_", " ").toLowerCase());

        return SkillResult.NORMAL;
        //and that's all folks!
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }


}