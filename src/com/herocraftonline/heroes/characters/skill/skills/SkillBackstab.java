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

    private String backstabText;

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

        double backstabChance = SkillConfigManager.getUseSetting(hero, this, "backstab-chance", Double.valueOf(-1.0), false);
        double backstabDamageModifier = SkillConfigManager.getUseSetting(hero, this, "backstab-bonus", Double.valueOf(0.85), false);
        double backstabDamageModifierIncrease = SkillConfigManager.getUseSetting(hero, this, "backstab-bonus-increase-per-agility", Double.valueOf(0.85), false);
        backstabDamageModifier += hero.getAttributeValue(AttributeType.AGILITY) * backstabDamageModifierIncrease;

        String backstabString = "deal";
        if (backstabChance > -1)
            backstabString = "have a " + Util.decFormat.format(backstabChance) + "% chance to deal";

        double ambushChance = SkillConfigManager.getUseSetting(hero, this, "ambush-chance", Double.valueOf(-1.0), false);
        double ambushDamageModifier = SkillConfigManager.getUseSetting(hero, this, "ambush-bonus", Double.valueOf(0.85), false);
        double ambushDamageModifierIncrease = SkillConfigManager.getUseSetting(hero, this, "ambush-bonus-increase-per-agility", Double.valueOf(0.85), false);
        ambushDamageModifier += hero.getAttributeValue(AttributeType.AGILITY) * ambushDamageModifierIncrease;

        String ambushString = "deal";
        if (ambushChance > -1)
            backstabString = "have a " + Util.decFormat.format(ambushChance) + "% chance to deal";

        String formattedBackstabDamageModifier = Util.decFormat.format(backstabDamageModifier * 100);
        String formattedAmbushDamageModifier = Util.decFormat.format(ambushDamageModifier * 100);

        return getDescription().replace("$1", backstabString + "").replace("$2", formattedBackstabDamageModifier).replace("$3", ambushString + "").replace("$4", formattedAmbushDamageModifier);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.USE_TEXT.node(), "");
        node.set("backstab-text", "");
        node.set("weapons", Util.swords);
        node.set("backstab-chance", Double.valueOf(-1));
        node.set("backstab-bonus", Double.valueOf(0.05));
        node.set("backstab-bonus-increase-per-agility", Double.valueOf(0.04125));
        node.set("ambush-chance", Double.valueOf(-1));
        node.set("ambush-bonus", Double.valueOf(0.10));
        node.set("ambush-bonus-increase-per-agility", Double.valueOf(0.06375));
        node.set("allow-vanilla-sneaking", Boolean.valueOf(false));

        return node;
    }

    public void init() {
        super.init();
        backstabText = SkillConfigManager.getRaw(this, "backstab-text", "").replace("%hero%", "$1").replace("%target%", "$2");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        Messaging.send(player, ChatColor.RED + "----------[ " + ChatColor.WHITE + "Backstab Damage " + ChatColor.RED + "]----------");

        List<String> weapons = SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords);

        double backstabDamageModifier = SkillConfigManager.getUseSetting(hero, this, "backstab-bonus", Double.valueOf(0.85), false);
        double backstabDamageModifierIncrease = SkillConfigManager.getUseSetting(hero, this, "backstab-bonus-increase-per-agility", Double.valueOf(0.85), false);
        backstabDamageModifier += 1 + (hero.getAttributeValue(AttributeType.AGILITY) * backstabDamageModifierIncrease);

        double ambushDamageModifier = SkillConfigManager.getUseSetting(hero, this, "ambush-bonus", Double.valueOf(0.85), false);
        double ambushDamageModifierIncrease = SkillConfigManager.getUseSetting(hero, this, "ambush-bonus-increase-per-agility", Double.valueOf(0.85), false);
        ambushDamageModifier += 1 + (hero.getAttributeValue(AttributeType.AGILITY) * ambushDamageModifierIncrease);

        double backstabDamage = 0;
        double ambushDamage = 0;
        for (String weaponName : weapons) {
            Material weapon = Material.getMaterial(weaponName);
            int baseDamage = plugin.getDamageManager().getHighestItemDamage(hero, weapon).intValue();

            backstabDamage = baseDamage * backstabDamageModifier;
            ambushDamage = baseDamage * ambushDamageModifier;

            weaponName = weaponName.replace("_", " ");
            weaponName = WordUtils.capitalizeFully(weaponName);
            displayWeaponDamage(player, weaponName, backstabDamage, ambushDamage);
        }

        return SkillResult.NORMAL;
    }

    private void displayWeaponDamage(Player player, String weaponName, double backstabDamage, double ambushDamage) {
        Messaging.send(player, ChatColor.GREEN + weaponName + ": "
                + ChatColor.WHITE + "Backstab: " + ChatColor.GRAY + Util.decFormat.format(backstabDamage)
                + ChatColor.WHITE + ", Sneaking Backstab: " + ChatColor.GRAY + Util.decFormat.format(ambushDamage));
    }

    public class SkillHeroesListener implements Listener {
        private final Skill skill;

        public SkillHeroesListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
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
                double damageModifier = -1;

                // Sneak for ambush, nosneak for backstab.
                boolean allowVanillaSneaking = SkillConfigManager.getUseSetting(hero, skill, "allow-vanilla-sneaking", false);
                if (hero.hasEffect("Sneak") || (allowVanillaSneaking && player.isSneaking())) {
                    chance = SkillConfigManager.getUseSetting(hero, skill, "ambush-chance", Double.valueOf(-1.0), false);

                    damageModifier = SkillConfigManager.getUseSetting(hero, skill, "ambush-bonus", Double.valueOf(0.85), false);
                    double damageModifierIncrease = SkillConfigManager.getUseSetting(hero, skill, "ambush-bonus-increase-per-agility", Double.valueOf(0.85), false);
                    damageModifier += 1 + (hero.getAttributeValue(AttributeType.AGILITY) * damageModifierIncrease);
                }
                else {
                    chance = SkillConfigManager.getUseSetting(hero, skill, "backstab-chance", Double.valueOf(-1.0), false);

                    damageModifier = SkillConfigManager.getUseSetting(hero, skill, "backstab-bonus", Double.valueOf(0.85), false);
                    double damageModifierIncrease = SkillConfigManager.getUseSetting(hero, skill, "backstab-bonus-increase-per-agility", Double.valueOf(0.85), false);
                    damageModifier += 1 + (hero.getAttributeValue(AttributeType.AGILITY) * damageModifierIncrease);
                }

                boolean backstabbed = true;
                if (chance < 0)		// If below 1, backstab every time.
                    event.setDamage((event.getDamage() * damageModifier));
                else {
                    if (Util.nextRand() < chance)
                        event.setDamage((event.getDamage() * damageModifier));
                    else
                        backstabbed = false;
                }

                if (backstabbed) {
                    Entity target = event.getEntity();
                    if (target instanceof Monster)
                        broadcast(player.getLocation(), backstabText, player.getDisplayName(), Messaging.getLivingEntityName((Monster) target));
                    else if (target instanceof Player)
                        broadcast(player.getLocation(), backstabText, player.getDisplayName(), ((Player) target).getDisplayName());
                }
            }
        }
    }
}