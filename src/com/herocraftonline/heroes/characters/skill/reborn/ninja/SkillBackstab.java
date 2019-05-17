package com.herocraftonline.heroes.characters.skill.reborn.ninja;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.logging.Level;

public class SkillBackstab extends ActiveSkill implements Passive {

    private String backstabText;

    public SkillBackstab(Heroes plugin) {
        super(plugin, "Backstab");
        setDescription("When attacking a target from behind, you $1 an additional $2% damage. " +
                "While sneaking, your attacks are more precise, and $3 an additional $4% damage.");
        setUsage("/skill backstab");
        setIdentifiers("skill backstab");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE, SkillType.UNBINDABLE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroesListener(this), plugin);
    }

    public String getDescription(Hero hero) {

        double backstabChance = SkillConfigManager.getUseSetting(hero, this, "backstab-chance", -1.0, false);
        double backstabDamageModifier = SkillConfigManager.getScaledUseSettingDouble(hero, this, "backstab-bonus", false);

        String backstabString = "deal";
        if (backstabChance > -1)
            backstabString = "have a " + Util.decFormat.format(backstabChance) + "% chance to deal";

        double ambushChance = SkillConfigManager.getUseSetting(hero, this, "ambush-chance", -1.0, false);
        double ambushDamageModifier = SkillConfigManager.getScaledUseSettingDouble(hero, this, "ambush-bonus", false);

        String ambushString = "deal";
        if (ambushChance > -1)
            ambushString = "have a " + Util.decFormat.format(ambushChance) + "% chance to deal";

        String formattedBackstabDamageModifier = Util.decFormat.format(backstabDamageModifier * 100);
        String formattedAmbushDamageModifier = Util.decFormat.format(ambushDamageModifier * 100);

        return getDescription()
                .replace("$1", backstabString)
                .replace("$2", formattedBackstabDamageModifier)
                .replace("$3", ambushString)
                .replace("$4", formattedAmbushDamageModifier);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.USE_TEXT.node(), "");
        config.set("backstab-text", "");
        config.set("weapons", Util.swords);
        config.set("backstab-chance", -1.0);
        config.set("backstab-bonus", 0.05);
        config.set("backstab-bonus-increase-per-dexterity", 0.04125);
        config.set("ambush-chance", -1.0);
        config.set("ambush-bonus", 0.10);
        config.set("ambush-bonus-increase-per-dexterity", 0.06375);
        config.set("allow-vanilla-sneaking", true);
        return config;
    }

    public void init() {
        super.init();
        backstabText = SkillConfigManager.getRaw(this, "backstab-text", "");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        player.sendMessage(ChatColor.RED + "----------[ " + ChatColor.WHITE + "Backstab Damage " + ChatColor.RED + "]----------");

        List<String> weapons = SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords);

        double backstabDamageModifier = 1 +SkillConfigManager.getScaledUseSettingDouble(hero, this, "backstab-bonus", false);
        double ambushDamageModifier = 1 + SkillConfigManager.getScaledUseSettingDouble(hero, this, "ambush-bonus", false);

        double backstabDamage = 0;
        double ambushDamage = 0;
        for (String weaponName : weapons) {
            Material weapon = Material.getMaterial(weaponName);
            if (weapon == null) {
                Heroes.log(Level.WARNING, "SkillBackstab: " + weaponName + " is not a valid weapon material name.");
                continue;
            }

            int baseDamage = 0;
            if (plugin.getDamageManager().getHighestItemDamage(hero, weapon) == null) {
                Heroes.log(Level.WARNING, "SkillBackstab: " + weaponName + " has no damage set.");
            } else {
                baseDamage = plugin.getDamageManager().getHighestItemDamage(hero, weapon).intValue();
            }

            backstabDamage = baseDamage * backstabDamageModifier;
            ambushDamage = baseDamage * ambushDamageModifier;

            weaponName = weaponName.replace("_", " ");
            weaponName = WordUtils.capitalizeFully(weaponName);
            displayWeaponDamage(player, weaponName, backstabDamage, ambushDamage);
        }

        return SkillResult.NORMAL;
    }

    private void displayWeaponDamage(Player player, String weaponName, double backstabDamage, double ambushDamage) {
        player.sendMessage(ChatColor.GREEN + weaponName + ": "
                + ChatColor.WHITE + "Backstab: " + ChatColor.GRAY + Util.decFormat.format(backstabDamage)
                + ChatColor.WHITE + ", Sneaking Backstab: " + ChatColor.GRAY + Util.decFormat.format(ambushDamage));
    }

    @Override
    public void tryApplying(Hero hero) {

    }

    @Override
    public void apply(Hero hero) {

    }

    @Override
    public void unapply(Hero hero) {

    }

    public class SkillHeroesListener implements Listener {
        private final Skill skill;

        public SkillHeroesListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (!(event.getDamager() instanceof Hero) || !(event.getEntity() instanceof LivingEntity))
                return;

            Hero hero = (Hero) event.getDamager();
            if (!hero.canUseSkill(skill))
                return;

            Player player = hero.getPlayer();
            ItemStack item = NMSHandler.getInterface().getItemInMainHand(player.getInventory());

            if (!SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.swords).contains(item.getType().name()))
                return;

            Entity target = event.getEntity();
            double direction = target.getLocation().getDirection().dot(player.getLocation().getDirection());
            //player.sendMessage("Backstab Math Testing - Direction: " + direction);    Less than 0, facing front. Use for HerosCall?
            if (direction <= 0.0D)
                return;

            double chance = -1;
            double damageModifier = -1;

            // Sneak for ambush, nosneak for backstab.
            boolean allowVanillaSneaking = SkillConfigManager.getUseSetting(hero, skill, "allow-vanilla-sneaking", false);
            if (hero.hasEffectType(EffectType.SNEAK) || hero.hasEffectType(EffectType.INVIS) || hero.hasEffectType(EffectType.INVISIBILITY) || (allowVanillaSneaking && player.isSneaking())) {
                chance = SkillConfigManager.getUseSetting(hero, skill, "ambush-chance", -1.0, false);

                damageModifier = SkillConfigManager.getScaledUseSettingDouble(hero, skill, "ambush-bonus", false);
            } else {
                chance = SkillConfigManager.getUseSetting(hero, skill, "backstab-chance", -1.0, false);

                damageModifier = SkillConfigManager.getScaledUseSettingDouble(hero, skill, "backstab-bonus", false);
            }

            boolean backStab = false;
            if (chance < 0) {        // If below 1, backstab every time.
                backStab = true;
            } else if (Util.nextRand() < chance) {
                backStab = true;
            }

            if (backStab) {
                event.setDamage(event.getDamage() * damageModifier);
                target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.COLOURED_DUST, 0, 0, 0.2F, 0.0F, 0.2F, 0.0F, 30, 16);
//                target.getWorld().spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 0.5, 0), 30, 0.2, 0, 0.2, new Particle.DustOptions(Color.RED, 1));
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERDRAGON_HURT, 0.5F, 0.6F);
                if (backstabText != null && backstabText.length() > 0) {
                    if (target instanceof Monster)
                        broadcast(player.getLocation(), backstabText.replace("%hero%", player.getName()).replace("%target%", CustomNameManager.getName(target)));
                    else if (target instanceof Player)
                        broadcast(player.getLocation(), backstabText.replace("%hero%", player.getName()).replace("%target%", target.getName()));
                }
            }
        }
    }
}