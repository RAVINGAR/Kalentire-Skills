package com.herocraftonline.heroes.characters.skill.skills;

import java.util.List;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillBackstab extends ActiveSkill {

    private String useText;

    public SkillBackstab(Heroes plugin) {
        super(plugin, "Backstab");
        setDescription("When attacking a target from behind, you $1 an additional $2% damage. While sneaking, your attacks are more precise, and $3 an additional $4% damage.");
        setUsage("/skill backstab");
        setArgumentRange(0, 0);
        setIdentifiers("skill backstab");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE, SkillType.UNBINDABLE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroesListener(this), plugin);
    }

    public String getDescription(Hero hero) {

        // First get their agility
        double agilityModifier = hero.getAttributeValue(AttributeType.AGILITY) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.EFFECTIVENESS_INCREASE_PER_AGILITY, 0.02125D, false);

        double backstabChance = SkillConfigManager.getUseSetting(hero, this, "backstab-chance", -1D, false);
        double backstabBonus = 1 + SkillConfigManager.getUseSetting(hero, this, "backstab-bonus", 0.65D, false);

        // Modify backstab
        backstabBonus += agilityModifier;
        int backstabModifier = (int) backstabBonus * 100;

        String backstabString = "deal";
        if (backstabChance > -1)
            backstabString = "have a " + backstabChance + "% chance to deal";

        double ambushChance = SkillConfigManager.getUseSetting(hero, this, "ambush-chance", -1D, false);
        double ambushBonus = 1 + SkillConfigManager.getUseSetting(hero, this, "ambush-bonus", 1.325D, false);

        // Modify ambush
        ambushBonus += agilityModifier;
        int ambushModifier = (int) ambushBonus * 100;

        String ambushString = "deal";
        if (ambushChance > -1)
            backstabString = "have a " + ambushChance + "% chance to deal";

        return getDescription().replace("$1", backstabString + "").replace("$2", backstabModifier + "").replace("$3", ambushString + "").replace("$4", ambushModifier + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("weapons", Util.swords);
        node.set("backstab-bonus", Double.valueOf(0.85D));
        node.set("backstab-chance", Double.valueOf(-1D));
        node.set("ambush-bonus", Double.valueOf(1.8D));
        node.set("ambush-chance", Double.valueOf(-1D));
        node.set(SkillSetting.EFFECTIVENESS_INCREASE_PER_AGILITY.node(), Double.valueOf(0.02125D));
        node.set("allow-vanilla-sneaking", Boolean.valueOf(false));
        node.set(SkillSetting.USE_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% backstabbed %target%!");

        return node;
    }

    public void init() {
        super.init();
        useText = SkillConfigManager.getRaw(this, SkillSetting.USE_TEXT, ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% backstabbed %target%!").replace("%hero%", "$1").replace("%target%", "$2");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        Messaging.send(player, ChatColor.RED + "----------[ " + ChatColor.WHITE + "Backstab Damage " + ChatColor.RED + "]----------");

        List<String> weapons = SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords);

        double backstabBonus = 1 + SkillConfigManager.getUseSetting(hero, this, "backstab-bonus", 0.65D, false);
        double ambushBonus = 1 + SkillConfigManager.getUseSetting(hero, this, "ambush-bonus", 1.325D, false);

        // Modify the damage modifier based on agility.
        double agilityModifier = hero.getAttributeValue(AttributeType.AGILITY) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.EFFECTIVENESS_INCREASE_PER_AGILITY, 0.02125D, false);
        backstabBonus += agilityModifier;
        ambushBonus += agilityModifier;

        int backstabDamage = 0;
        int ambushDamage = 0;
        for (String weaponName : weapons) {
            Material weapon = Material.getMaterial(weaponName);
            int baseDamage = plugin.getDamageManager().getHighestItemDamage(weapon, player).intValue();

            backstabDamage = (int) (baseDamage * backstabBonus);
            ambushDamage = (int) (baseDamage * ambushBonus);

            weaponName = weaponName.replace("_", " ");
            weaponName = WordUtils.capitalizeFully(weaponName);
            displayWeaponDamage(player, weaponName, backstabDamage, ambushDamage);
        }

        return SkillResult.NORMAL;
    }

    private void displayWeaponDamage(Player player, String weaponName, int backstabDamage, int ambushDamage) {
        Messaging.send(player, ChatColor.GREEN + weaponName + ": "
                + ChatColor.WHITE + "Backstab: " + ChatColor.GRAY + backstabDamage
                + ChatColor.WHITE + ", Sneaking Backstab: " + ChatColor.GRAY + ambushDamage);
    }

    public class SkillHeroesListener implements Listener {
        private final Skill skill;

        public SkillHeroesListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (!(event.getDamager() instanceof Hero) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            Hero hero = (Hero) event.getDamager();
            Player player = hero.getPlayer();

            if (hero.canUseSkill(skill)) {
                ItemStack item = player.getItemInHand();

                if (!SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.swords).contains(item.getType().name())) {
                    return;
                }

                if (event.getEntity().getLocation().getDirection().dot(player.getLocation().getDirection()) <= 0.0D) {
                    return;
                }

                double chance = -1;
                double bonusDamage = -1;

                // Sneak for ambush, nosneak for backstab.
                boolean allowVanillaSneaking = SkillConfigManager.getUseSetting(hero, skill, "allow-vanilla-sneaking", false);
                if (hero.hasEffect("Sneak") || (allowVanillaSneaking && player.isSneaking())) {
                    chance = SkillConfigManager.getUseSetting(hero, skill, "ambush-chance", -1D, false);
                    bonusDamage = 1 + SkillConfigManager.getUseSetting(hero, skill, "ambush-bonus", 1.2D, false);
                }
                else {
                    chance = SkillConfigManager.getUseSetting(hero, skill, "backstab-chance", -1D, false);
                    bonusDamage = 1 + SkillConfigManager.getUseSetting(hero, skill, "backstab-bonus", 0.65D, false);
                }

                boolean backstabbed = true;
                if (chance < 0)		// If below 1, backstab every time.
                    event.setDamage((event.getDamage() * bonusDamage));
                else {
                    if (Util.nextRand() < chance)
                        event.setDamage((event.getDamage() * bonusDamage));
                    else
                        backstabbed = false;
                }

                if (backstabbed) {
                    Entity target = event.getEntity();
                    if (target instanceof Monster)
                        broadcast(player.getLocation(), useText, player.getDisplayName(), Messaging.getLivingEntityName((Monster) target));
                    else if (target instanceof Player)
                        broadcast(player.getLocation(), useText, player.getDisplayName(), ((Player) target).getDisplayName());
                }
            }
        }
    }
}