package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;

public class SkillArrowstorm extends ActiveSkill {

    private final Map<Hero, Integer> shootingPlayers = new HashMap<Hero, Integer>();

    public SkillArrowstorm(Heroes plugin) {
        super(plugin, "Arrowstorm");
        this.setDescription("Shoots between $1-$2 arrows at a rate of $3-$4 per second. CD: $5s M: $6");
        this.setUsage("/skill arrowstorm");
        this.setArgumentRange(0, 0);
        this.setIdentifiers(new String[]{"skill arrowstorm"});

        this.setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING);
    }

    @Override
    public String getDescription(Hero hero) {
        final int maxArrows = SkillConfigManager.getUseSetting(hero, this, "max-arrows", 30, false) + (SkillConfigManager.getUseSetting(hero, this, "arrows-per-level", 0, false) * hero.getHeroLevel(this));
        final int minArrows = SkillConfigManager.getUseSetting(hero, this, "min-arrows", 15, false) + (SkillConfigManager.getUseSetting(hero, this, "arrows-per-level", 0, false) * hero.getHeroLevel(this));
        final int maxRate = SkillConfigManager.getUseSetting(hero, this, "max-rate", 20, false);
        final int minRate = SkillConfigManager.getUseSetting(hero, this, "min-rate", 2, false);
        String description = this.getDescription().replace("$1", maxArrows + "").replace("$2", minArrows + "").replace("$3", maxRate + "").replace("$4", minRate + "");

        //COOLDOWN
        final int cooldown = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false) - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE, 0, false) * hero.getHeroLevel(this))) / 1000;
        if (cooldown > 0) {
            description += " CD:" + cooldown + "s";
        }

        //MANA
        final int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 10, false) - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE, 0, false) * hero.getHeroLevel(this));
        if (mana > 0) {
            description += " M:" + mana;
        }

        //HEALTH_COST
        final int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false) - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST_REDUCE, mana, true) * hero.getHeroLevel(this));
        if (healthCost > 0) {
            description += " HP:" + healthCost;
        }

        //STAMINA
        final int staminaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA, 0, false) - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA_REDUCE, 0, false) * hero.getHeroLevel(this));
        if (staminaCost > 0) {
            description += " FP:" + staminaCost;
        }

        //DELAY
        final int delay = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
            description += " W:" + delay + "s";
        }

        //EXP
        final int exp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXP.node(), 0, false);
        if (exp > 0) {
            description += " XP:" + exp;
        }
        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("max-arrows", 30);
        node.set("min-arrows", 15);
        node.set("min-rate", 2);
        node.set("max-rate", 20);
        node.set("arrows-per-level", 0.0);
        node.set(SkillSetting.COOLDOWN.node(), 1000);
        node.set(SkillSetting.MANA.node(), 10);
        return node;
    }

    @Override
    public SkillResult use(final Hero hero, String[] args) {
        if (this.shootingPlayers.containsKey(hero)) {
            this.plugin.getServer().getScheduler().cancelTask(this.shootingPlayers.get(hero));
            this.shootingPlayers.remove(hero);
            final long cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 1000, false);
            hero.setCooldown("Arrowstorm", System.currentTimeMillis() + cooldown);
            hero.getPlayer().sendMessage(ChatComponents.GENERIC_SKILL + hero.getPlayer().getName() + ChatColor.GRAY + " stopped shooting arrows prematurely.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        final Player player = hero.getPlayer();
        final PlayerInventory inv = player.getInventory();
        int minArrows = SkillConfigManager.getUseSetting(hero, this, "min-arrows", 15, false) + (SkillConfigManager.getUseSetting(hero, this, "arrows-per-level", 0, false) * hero.getHeroLevel(this));
        if (minArrows <= 0) {
            minArrows = 1;
        } else if (minArrows > 64) {
            minArrows = 64;
        }
        int maxArrows = SkillConfigManager.getUseSetting(hero, this, "max-arrows", 30, false) + (SkillConfigManager.getUseSetting(hero, this, "arrows-per-level", 0, false) * hero.getHeroLevel(this));
        if (maxArrows < minArrows) {
            maxArrows = minArrows;
        } else if (maxArrows > 64) {
            maxArrows = 64;
        }
        int minRate = SkillConfigManager.getUseSetting(hero, this, "min-rate", 2, false);
        if ((minRate != 1) | (minRate != 2) | (minRate != 4) | (minRate != 5) | (minRate != 10) | (minRate != 20)) {
            minRate = 2;
        }
        int maxRate = SkillConfigManager.getUseSetting(hero, this, "max-rate", 10, false);
        if ((maxRate != 1) | (maxRate != 2) | (maxRate != 4) | (maxRate != 5) | (maxRate != 10) | (maxRate != 20)) {
            maxRate = 10;
        }
        if (maxRate < minRate) {
            maxRate = minRate;
        }
        final int randRate = maxRate - minRate;
        final int randArrows = maxArrows - minArrows;


        final Map<Integer, ? extends ItemStack> arrowSlots = inv.all(Material.ARROW);

        int numArrows = 0;
        for (final Map.Entry<Integer, ? extends ItemStack> entry : arrowSlots.entrySet()) {
            numArrows += entry.getValue().getAmount();
        }

        final int preTotalArrows = (int) Math.rint(Math.random() * randArrows) + minArrows;
        if (numArrows > preTotalArrows) {
            numArrows = preTotalArrows;
        }
        if (numArrows < minArrows) {
            numArrows = 0;
        }
        if (numArrows == 0) {
            return new SkillResult(ResultType.MISSING_REAGENT, true, minArrows, "Arrows");
        }

        int removedArrows = 0;
        for (final Map.Entry<Integer, ? extends ItemStack> entry : arrowSlots.entrySet()) {
            final int amount = entry.getValue().getAmount();
            int remove = amount;
            if ((removedArrows + remove) > numArrows) {
                remove = numArrows - removedArrows;
            }
            removedArrows += remove;
            if (remove == amount) {
                inv.clear(entry.getKey());
            } else {
                inv.getItem(entry.getKey()).setAmount(amount - remove);
            }

            if (removedArrows >= numArrows) {
                break;
            }
        }
        player.updateInventory();


        this.broadcastExecuteText(hero);
        final long sleepTime = (long) Math.rint(Math.random() * randRate) + minRate;
        final Skill skill = this;
        final float rate = 20 / sleepTime;
        player.sendMessage(ChatColor.GRAY + "Casting " + ChatColor.WHITE + preTotalArrows + ChatColor.GRAY + " at a rate of " + ChatColor.WHITE + rate + ChatColor.GRAY + " per second");
        this.shootingPlayers.put(hero, this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                player.launchProjectile(Arrow.class);
            }
        }, 0L, sleepTime));
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    SkillArrowstorm.this.plugin.getServer().getScheduler().cancelTask(SkillArrowstorm.this.shootingPlayers.get(hero));
                    SkillArrowstorm.this.shootingPlayers.remove(hero);
                    hero.setCooldown("Arrowstorm", SkillConfigManager.getUseSetting(hero, skill, SkillSetting.COOLDOWN.node(), 1000, false) + System.currentTimeMillis());
                } catch (final Exception e) {

                }
            }
        }, preTotalArrows * sleepTime);
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                hero.setCooldown("Arrowstorm", System.currentTimeMillis());
            }
        }, 1L);
        return SkillResult.NORMAL;
    }

}
