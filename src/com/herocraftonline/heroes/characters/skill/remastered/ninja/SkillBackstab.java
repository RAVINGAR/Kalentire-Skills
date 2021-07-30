package com.herocraftonline.heroes.characters.skill.remastered.ninja;

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
        setDescription("When attacking a target from behind, you $1 an additional $2% damage. While sneaking, your attacks are more precise, and $3 an additional $4% damage.");
        setUsage("/skill backstab");
        setArgumentRange(0, 0);
        setIdentifiers("skill backstab");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE, SkillType.UNBINDABLE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroesListener(this), plugin);
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
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        player.sendMessage(ChatColor.RED + "----------[ " + ChatColor.WHITE + "Backstab Damage " + ChatColor.RED + "]----------");

        List<String> weapons = SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords);

        double backstabDamageModifier = SkillConfigManager.getUseSetting(hero, this, "backstab-bonus", 0.85, false);
        double backstabDamageModifierIncrease = SkillConfigManager.getUseSetting(hero, this, "backstab-bonus-increase-per-dexterity", 0.85, false);
        backstabDamageModifier += 1 + (hero.getAttributeValue(AttributeType.DEXTERITY) * backstabDamageModifierIncrease);

        double ambushDamageModifier = SkillConfigManager.getUseSetting(hero, this, "ambush-bonus", 0.85, false);
        double ambushDamageModifierIncrease = SkillConfigManager.getUseSetting(hero, this, "ambush-bonus-increase-per-dexterity", 0.85, false);
        ambushDamageModifier += 1 + (hero.getAttributeValue(AttributeType.DEXTERITY) * ambushDamageModifierIncrease);

        double backstabDamage = 0;
        double ambushDamage = 0;
        for (String weaponName : weapons) {
            Material weapon = Material.getMaterial(weaponName);
            if (weapon == null){
                Heroes.log(Level.WARNING, "SkillBackstab: " + weaponName + " is not a valid weapon material name.");
                continue;
            }

            int baseDamage = 0;
            if (plugin.getDamageManager().getHighestItemDamage(hero, weapon) == null){
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
            LivingEntity target = (LivingEntity) event.getEntity();

            if (hero.canUseSkill(skill)) {
                ItemStack item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()); // 1.9: Main hand does left click attacks, so main hand only

                if (!SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.swords).contains(item.getType().name())) {
                    return;
                }

                if (target.getLocation().getDirection().dot(player.getLocation().getDirection()) <= 0.0D) {
                    return;
                }

                double chance = -1;
                double damageModifier = -1;

                // Sneak for ambush, nosneak for backstab.
                boolean allowVanillaSneaking = SkillConfigManager.getUseSetting(hero, skill, "allow-vanilla-sneaking", false);
                if (hero.hasEffect("Sneak") || (allowVanillaSneaking && player.isSneaking())) {
                    chance = SkillConfigManager.getUseSetting(hero, skill, "ambush-chance", -1.0, false);

                    damageModifier = SkillConfigManager.getUseSetting(hero, skill, "ambush-bonus", 0.85, false);
                    double damageModifierIncrease = SkillConfigManager.getUseSetting(hero, skill, "ambush-bonus-increase-per-dexterity", 0.85, false);
                    damageModifier += 1 + (hero.getAttributeValue(AttributeType.DEXTERITY) * damageModifierIncrease);
                }
                else {
                    chance = SkillConfigManager.getUseSetting(hero, skill, "backstab-chance", -1.0, false);

                    damageModifier = SkillConfigManager.getUseSetting(hero, skill, "backstab-bonus", 0.85, false);
                    double damageModifierIncrease = SkillConfigManager.getUseSetting(hero, skill, "backstab-bonus-increase-per-dexterity", 0.85, false);
                    damageModifier += 1 + (hero.getAttributeValue(AttributeType.DEXTERITY) * damageModifierIncrease);
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
                    //target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.COLOURED_DUST, 0, 0, 0.2F, 0.0F, 0.2F, 0.0F, 30, 16);
                    target.getWorld().spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 0.5, 0), 30, 0.2, 0, 0.2, new Particle.DustOptions(Color.RED, 1));
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 1.0F, 0.6F);
//                    if (target instanceof Monster)
//                        broadcast(player.getLocation(), backstabText.replace("%hero%", player.getName()).replace("%target%", CustomNameManager.getName(target)));
//                    else if (target instanceof Player)
//                        broadcast(player.getLocation(), backstabText.replace("%hero%", player.getName()).replace("%target%", target.getName()));

                    if (target instanceof Monster || target instanceof Player) { // Check doesn't seem necessary, only seems to not run for some creatures such as Animals.
                        String targetName = target instanceof Player ? target.getName() : CustomNameManager.getName(target);
                        broadcast(player.getLocation(), backstabText.replace("%hero%", player.getName())
                                .replace("%target%", targetName));
                    }
                }
            }
        }
    }
}