package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

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
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class SkillTemporalEchoes extends ActiveSkill {

    private final String minionEffectName = "TemporalEchoe";
    private boolean disguiseApiLoaded;

    public SkillTemporalEchoes(Heroes plugin) {
        super(plugin, "TemporalEchoes");
        setDescription("Conjures up to $1 time doubles of yourself to assist you in battle for up to $2 seconds. "
                + "The due to the unstable state of their existance, they only have $3 health and deal $4 damage per hit.");
        setUsage("/skill temporalechoes");
        setArgumentRange(0, 0);
        setIdentifiers("skill temporalechoes");
        setTypes(SkillType.SUMMONING, SkillType.ABILITY_PROPERTY_TEMPORAL, SkillType.SILENCEABLE);

        if (Bukkit.getServer().getPluginManager().getPlugin("LibsDisguises") != null) {
            disguiseApiLoaded = true;
        }
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("echoes-summoned", 4);
        config.set("minion-attack-damage", 40.0);
        config.set("minion-max-hp", 100.0);
        config.set("minion-speed-amplifier", 1);
        config.set("minion-duration", 6000);
        config.set("launch-velocity", 1.2);
        config.set("min-launch-spread", -0.4);
        config.set("max-launch-spread", 0.4);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        long duration = SkillConfigManager.getUseSetting(hero, this, "minion-duration", 6000, false);
        double launchVelocity = SkillConfigManager.getUseSetting(hero, this, "launch-velocity", 1.2, false);
        int numEchoes = SkillConfigManager.getUseSetting(hero, this, "echoes-summoned", 4, false);

        final double randomMin = SkillConfigManager.getUseSetting(hero, this, "min-launch-spread", -0.4, false);
        final double randomMax = SkillConfigManager.getUseSetting(hero, this, "max-launch-spread", 0.4, false);

        for (int i = 0; i < numEchoes; i++) {
            // Wolfs have the most reliable default AI for following and helping the player. We'll disguise it as something else later.
            Wolf minion = (Wolf) player.getWorld().spawnEntity(player.getEyeLocation(), EntityType.WOLF);
            minion.setOwner(player);

            final Monster monster = plugin.getCharacterManager().getMonster(minion);
            monster.setExperience(0);
            monster.addEffect(new TemporalEchoesMinionEffect(this, hero, duration));

            Vector launchVector = player.getLocation().getDirection().normalize()
                    .add(new Vector(ThreadLocalRandom.current().nextDouble(randomMin, randomMax), 0, ThreadLocalRandom.current().nextDouble(randomMin, randomMax)))
                    .multiply(launchVelocity);

            minion.setVelocity(launchVector);
            minion.setFallDistance(-7F);
        }

        return SkillResult.NORMAL;
    }

    private class TemporalEchoesMinionEffect extends SummonEffect {
        TemporalEchoesMinionEffect(Skill skill, Hero summoner, long duration) {
            super(skill, minionEffectName, duration, summoner, null);

            types.add(EffectType.TEMPORAL);

            int speedAmplifier = SkillConfigManager.getUseSetting(summoner, skill, "minion-speed-amplifier", 1, false);

            addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, (int) (duration / 50), 0));
            addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (duration / 50), speedAmplifier));
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            double maxHp = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-max-hp", 100.0, false);
            double hitDmg = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-attack-damage", 40.0, false);

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

                PlayerDisguise disguise = new PlayerDisguise(applier);
                disguise.setKeepDisguiseOnPlayerDeath(true);
                disguise.setEntity(minion);
                disguise.setShowName(true);
                disguise.setModifyBoundingBox(false);
                disguise.setReplaceSounds(true);
                LivingWatcher watcher = disguise.getWatcher();
                PlayerInventory inventory = applier.getInventory();
                watcher.setArmor(inventory.getArmorContents().clone());
                watcher.setItemInMainHand(inventory.getItemInMainHand().clone());
                watcher.setItemInOffHand(inventory.getItemInOffHand().clone());
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
