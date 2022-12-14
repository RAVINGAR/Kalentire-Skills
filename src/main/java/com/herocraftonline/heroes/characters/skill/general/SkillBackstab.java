package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class SkillBackstab extends ActiveSkill implements Passive, Listenable {

    private final Listener listener;
    private String backstabText;

    private List<Material> validWeapons;

    public SkillBackstab(Heroes plugin) {
        super(plugin, "Backstab");
        setDescription("When attacking a target from behind, you $1 an additional $2% damage. While sneaking, your attacks are more precise, and $3 an additional $4% damage.");
        setUsage("/skill backstab");
        setArgumentRange(0, 0);
        setIdentifiers("skill backstab");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE, SkillType.UNBINDABLE);
        validWeapons = new ArrayList<>();
        listener = new SkillHeroesListener(this);
    }

    public String getDescription(Hero hero) {
        double backstabChance = SkillConfigManager.getUseSetting(hero, this, "backstab-chance", -1.0, false);
        double backstabDamageModifier = SkillConfigManager.getUseSetting(hero, this, "backstab-bonus", 0.85, false);
        double backstabDamageModifierIncrease = SkillConfigManager.getUseSetting(hero, this, "backstab-bonus-increase-per-dexterity", 0.85, false);
        backstabDamageModifier += hero.getAttributeValue(AttributeType.DEXTERITY) * backstabDamageModifierIncrease;

        String backstabString = "deal";
        if (backstabChance > -1)
            backstabString = "have a " + Util.decFormat.format(backstabChance * 100) + "% chance to deal";

        double ambushChance = SkillConfigManager.getUseSetting(hero, this, "ambush-chance", -1.0, false);
        double ambushDamageModifier = SkillConfigManager.getUseSetting(hero, this, "ambush-bonus", 0.85, false);
        double ambushDamageModifierIncrease = SkillConfigManager.getUseSetting(hero, this, "ambush-bonus-increase-per-dexterity", 0.85, false);
        ambushDamageModifier += hero.getAttributeValue(AttributeType.DEXTERITY) * ambushDamageModifierIncrease;

        String ambushString = "deal";
        if (ambushChance > -1)
            ambushString = "have a " + Util.decFormat.format(ambushChance * 100) + "% chance to deal";

        return getDescription()
                .replace("$1", backstabString)
                .replace("$2", Util.decFormat.format(backstabDamageModifier * 100))
                .replace("$3", ambushString)
                .replace("$4", Util.decFormat.format(ambushDamageModifier * 100));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.USE_TEXT.node(), "");
        node.set("backstab-text", "");
        node.set("weapons", Util.swords);
        node.set("backstab-chance", (double) -1);
        node.set("backstab-bonus", 0.05);
        node.set("backstab-bonus-increase-per-dexterity", 0.04125);
        node.set("ambush-chance", (double) -1);
        node.set("ambush-bonus", 0.10);
        node.set("ambush-bonus-increase-per-dexterity", 0.06375);
        node.set("allow-vanilla-sneaking", false);

        return node;
    }

    public void init() {
        super.init();
        backstabText = SkillConfigManager.getRaw(this, "backstab-text", "");

        SkillConfigManager.getUseSetting(null, this, "weapons", Util.swords).forEach(str -> {
            Material material = Material.getMaterial(str.toUpperCase());
            if(material != null) {
                validWeapons.add(material);
            }
            else {
                Heroes.log(Level.WARNING, "SkillBackstab: " + str + " is not a valid weapon material name.");
            }
        });
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        player.sendMessage(ChatColor.RED + "----------[ " + ChatColor.WHITE + "Backstab Damage " + ChatColor.RED + "]----------");

        final double backstabDamageModifier = SkillConfigManager.getUseSetting(hero, this, "backstab-bonus", 0.85, false)
                + ((hero.getAttributeValue(AttributeType.DEXTERITY) * SkillConfigManager.getUseSetting(hero, this, "backstab-bonus-increase-per-dexterity", 0.05, false)));

        final double ambushDamageModifier = SkillConfigManager.getUseSetting(hero, this, "ambush-bonus", 0.85, false)
                + ((hero.getAttributeValue(AttributeType.DEXTERITY) * SkillConfigManager.getUseSetting(hero, this, "ambush-bonus-increase-per-dexterity", 0.05, false)));


        validWeapons.forEach(weapon -> {
            double baseDamage = plugin.getDamageManager().getDefaultClassDamage(hero, weapon);
            displayWeaponDamage(player,
                    WordUtils.capitalizeFully(weapon.getKey().getKey().replaceAll("_", " ")),
                    baseDamage * backstabDamageModifier,
                    baseDamage * ambushDamageModifier);
        });

        return SkillResult.NORMAL;
    }

    private void displayWeaponDamage(Player player, String weaponName, double backstabDamage, double ambushDamage) {
        player.sendMessage(ChatColor.GREEN + weaponName + ": "
                + ChatColor.WHITE + "Backstab: " + ChatColor.GRAY + Util.decFormat.format(backstabDamage)
                + ChatColor.WHITE + ", Sneaking Backstab: " + ChatColor.GRAY + Util.decFormat.format(ambushDamage));
    }

    @Override
    public void tryApplying(Hero hero) {
        // blank just so we implement Passive, and appear as a skill that does passive effects (though we dont need an effect to do this)
        // After all our only use as a active is to show bonus attack damage for weapons
    }

    @Override
    public void apply(Hero hero) {
        // blank just so we implement Passive, and appear as a skill that does passive effects (though we dont need an effect to do this)
        // After all our only use as a active is to show bonus attack damage for weapons
    }

    @Override
    public void unapply(Hero hero) {
        // blank just so we implement Passive, and appear as a skill that does passive effects (though we dont need an effect to do this)
        // After all our only use as a active is to show bonus attack damage for weapons
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    public class SkillHeroesListener implements Listener {
        private final Skill skill;

        public SkillHeroesListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (!(event.getAttacker() instanceof Hero hero)) {
                return;
            }

            Player player = hero.getPlayer();
            LivingEntity target = event.getDefender().getEntity();

            if (hero.canUseSkill(skill)) {
                ItemStack item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()); // 1.9: Main hand does left click attacks, so main hand only

                if (!validWeapons.contains(item.getType())) {
                    return;
                }

                if (target.getLocation().getDirection().dot(player.getLocation().getDirection()) <= 0.0D) {
                    return;
                }

                double chance;
                double damageModifier;

                // Sneak for ambush, nosneak for backstab.
                boolean allowVanillaSneaking = SkillConfigManager.getUseSetting(hero, skill, "allow-vanilla-sneaking", false);
                if (hero.hasEffect("Sneak") || (allowVanillaSneaking && player.isSneaking())) {
                    chance = SkillConfigManager.getUseSetting(hero, skill, "ambush-chance", -1.0, false);
                    damageModifier = SkillConfigManager.getUseSetting(hero, skill, "ambush-bonus", 0.85, false)
                        + (hero.getAttributeValue(AttributeType.DEXTERITY) * SkillConfigManager.getUseSetting(hero, skill, "ambush-bonus-increase-per-dexterity", 0.05, false));
                }
                else {
                    chance = SkillConfigManager.getUseSetting(hero, skill, "backstab-chance", -1.0, false);
                    damageModifier = SkillConfigManager.getUseSetting(hero, skill, "backstab-bonus", 0.85, false)
                        + (hero.getAttributeValue(AttributeType.DEXTERITY) * SkillConfigManager.getUseSetting(hero, skill, "backstab-bonus-increase-per-dexterity", 0.85, false));
                }

                if (chance < 0 || Util.nextRand() < chance) {
                    event.setDamage((event.getDamage() * damageModifier));
                    target.getWorld().spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 0.5, 0), 30, 0.2, 0, 0.2, new Particle.DustOptions(Color.RED, 1));
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 0.8F, 0.6F);
                    String targetName = target instanceof Player ? target.getName() : CustomNameManager.getName(target);
                    broadcast(player.getLocation(), backstabText.replace("%hero%", player.getName())
                            .replace("%target%", targetName));
                }
            }
        }
    }
}