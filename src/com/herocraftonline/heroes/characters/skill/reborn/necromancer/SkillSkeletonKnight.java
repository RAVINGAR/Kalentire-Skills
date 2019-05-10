package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SummonEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillSkeletonKnight extends ActiveSkill {

    private final String minionEffectName = "SkeletonKnight";
    private boolean disguiseApiLoaded;

    public SkillSkeletonKnight(Heroes plugin) {
        super(plugin, "SkeletonKnight");
        setDescription("Conjures a Skeleton Knight to obey your commands. " +
                "The minion has $1 HP deals $2 damage per hit, and lasts for up to $3 seconds. $9");
        setUsage("/skill skeletonknight");
        setIdentifiers("skill skeletonknight");
        setArgumentRange(0, 0);
        setTypes(SkillType.SUMMONING, SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE);

        if (Bukkit.getServer().getPluginManager().getPlugin("LibsDisguises") != null) {
            disguiseApiLoaded = true;
        }
    }

    public String getDescription(Hero hero) {
        double maxHp = SkillConfigManager.getUseSetting(hero, this, "minion-max-hp", 400.0, false);
        maxHp += SkillConfigManager.getUseSetting(hero, this, "minion-max-hp-per-level", 4.0, false) * hero.getHeroLevel(this);

        double hitDmg = SkillConfigManager.getUseSetting(hero, this, "minion-attack-damage", 25.0, false);
        hitDmg += SkillConfigManager.getUseSetting(hero, this, "minion-attack-damage-per-level", 0.4, false) * hero.getHeroLevel(this);

        long duration = SkillConfigManager.getUseSetting(hero, this, "minion-duration", 45000, false);

        String speedText = "";
        int speedAmplifier = SkillConfigManager.getUseSetting(hero, this, "minion-speed-amplifier", -1, false);
        if (speedAmplifier > 2) {
            speedText = "This is an extremely fast minion.";
        } else if (speedAmplifier > 1) {
            speedText = "This is a very fast minion.";
        } else if (speedAmplifier > 0) {
            speedText = "This is a fast minion.";
        } else {
            speedText = "This minion does not move very fast.";
        }

        return getDescription()
                .replace("$1", Util.decFormat.format(maxHp))
                .replace("$2", Util.decFormat.format(hitDmg))
                .replace("$3", Util.decFormat.format(duration / 1000.0))
                .replace("$9", speedText);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("minion-attack-damage", 25.0);
        config.set("minion-attack-damage-per-level", 0.4);
        config.set("minion-max-hp", 400.0);
        config.set("minion-max-hp-per-level", 4.0);
        config.set("minion-duration", 60000);
        config.set("minion-speed-amplifier", -1);
        config.set("launch-velocity", 2.0);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        double launchVelocity = SkillConfigManager.getUseSetting(hero, this, "launch-velocity", 2.0, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, "minion-duration", 45000, false);

        // Wolfs have the most reliable default AI for following and helping the player. We'll disguise it as something else later.
        Wolf minion = (Wolf) player.getWorld().spawnEntity(player.getEyeLocation(), EntityType.WOLF);
        minion.setOwner(player);

        final Monster monster = plugin.getCharacterManager().getMonster(minion);
        monster.setExperience(0);
        monster.addEffect(new SkeletonKnightEffect(this, hero, duration));

        minion.setVelocity(player.getLocation().getDirection().normalize().multiply(launchVelocity));
        minion.setFallDistance(-7F);

        return SkillResult.NORMAL;
    }

    public class SkeletonKnightEffect extends SummonEffect {
        public SkeletonKnightEffect(Skill skill, Hero summoner, long duration) {
            super(skill, minionEffectName, duration, summoner, null);

            types.add(EffectType.DISEASE);
            types.add(EffectType.WATER_BREATHING);

            addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, (int) (duration / 50), 0));

            int speedAmplifier = SkillConfigManager.getUseSetting(summoner, skill, "minion-speed-amplifier", -1, false);
            if (speedAmplifier > -1) {
                addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (duration / 50), speedAmplifier));
            }
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            double maxHp = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-max-hp", 400.0, false);
            maxHp += SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-max-hp-per-level", 4.0, false) * getSummoner().getHeroLevel(skill);

            double hitDmg = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-attack-damage", 25.0, false);
            hitDmg += SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-attack-damage-per-level", 0.4, false) * getSummoner().getHeroLevel(skill);

            LivingEntity minion = monster.getEntity();
            minion.setMaxHealth(maxHp);
            minion.setHealth(maxHp);
            minion.setCustomName(ChatColor.DARK_GREEN + applier.getName() + "'s Minion");
            minion.setCustomNameVisible(true);
            monster.setDamage(hitDmg);

            if (disguiseApiLoaded) {
                if (!DisguiseAPI.isDisguised(minion)) {
                    DisguiseAPI.undisguiseToAll(minion);
                }

                MobDisguise disguise = new MobDisguise(DisguiseType.getType(EntityType.SKELETON), false);
                disguise.setEntity(minion);
                disguise.setShowName(true);
                disguise.setModifyBoundingBox(false);
                disguise.setReplaceSounds(true);
                disguise.setKeepDisguiseOnPlayerDeath(true);
                LivingWatcher watcher = disguise.getWatcher();
                ItemStack[] fakeInventory = new ItemStack[]{
                        new ItemStack(Material.IRON_BOOTS, 1),
                        new ItemStack(Material.IRON_LEGGINGS, 1),
                        new ItemStack(Material.IRON_CHESTPLATE, 1),
                        new ItemStack(Material.AIR, 1)
                };
                watcher.setArmor(fakeInventory);
                watcher.setItemInMainHand(new ItemStack(Material.IRON_SWORD, 1));
                watcher.setItemInOffHand(new ItemStack(Material.SHIELD, 1));
                disguise.startDisguise();
            }
        }

        @Override
        public void removeFromMonster(Monster monster) {
            LivingEntity minion = monster.getEntity();

            if (disguiseApiLoaded) {
                if (DisguiseAPI.isDisguised(minion)) {
                    Disguise disguise = DisguiseAPI.getDisguise(minion);
                    disguise.stopDisguise();
                    disguise.removeDisguise();
                }
            }

            // Execute this last since it will cleanup the minion
            super.removeFromMonster(monster);
        }
    }
}
