package com.herocraftonline.heroes.characters.skill.general;

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
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class SkillAstralReflection extends ActiveSkill {

    private final String minionEffectName = "AstralReflection";
    private boolean disguiseApiLoaded;

    public SkillAstralReflection(Heroes plugin) {
        super(plugin, "AstralReflection");
        setDescription(" Conjures a time double of yourself to assist you in battle until it dies or for $2 seconds. " +
                "If you use this skill while your double is alive, you can cause yourself and your double to switch places.");
        setUsage("/skill astralreflection");
        setArgumentRange(0, 0);
        setIdentifiers("skill astralreflection");
        setTypes(SkillType.SUMMONING, SkillType.ABILITY_PROPERTY_TEMPORAL, SkillType.SILENCEABLE);

        if (Bukkit.getServer().getPluginManager().getPlugin("LibsDisguises") != null) {
            disguiseApiLoaded = true;
        }
    }

    public String getDescription(Hero hero) {
        long duration = SkillConfigManager.getUseSetting(hero, this, "minion-duration", 6000, false);
        double maxHp = SkillConfigManager.getUseSetting(hero, this, "minion-max-hp", 100.0, false);
        double hitDmg = SkillConfigManager.getUseSetting(hero, this, "minion-attack-damage", 40.0, false);

        return getDescription()
                .replace("$2", Util.decFormat.format((double) duration / 1000))
                .replace("$3", Util.decFormat.format(maxHp))
                .replace("$4", Util.decFormat.format(hitDmg));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("minion-attack-damage", 40.0);
        config.set("minion-max-hp", 300.0);
        config.set("minion-speed-amplifier", 1);
        config.set("minion-duration", 60000);
        config.set("launch-velocity", 1.2);
        config.set("min-launch-spread", -0.4);
        config.set("max-launch-spread", 0.4);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Set<Monster> summons = hero.getSummons();
        Monster summon = null;
        for(Monster m : summons) {
            if(m.getEffect(minionEffectName) != null) {
                summon = m;
                break;
            }
        }

        if(summon == null) {
            summonDouble(hero);
        }
        else {
            switchPlaces(hero, summon);
        }


        return SkillResult.NORMAL;
    }

    private void switchPlaces(Hero hero, Monster summon) {
        Location hLoc = hero.getPlayer().getLocation();
        Location sLoc = summon.getEntity().getLocation();

        if(hLoc.getWorld().getName().equals(sLoc.getWorld().getName())) {
            World world = hLoc.getWorld();

            world.playSound(hLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 0.8F);
            world.playSound(sLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 0.8F);
            world.spawnParticle(Particle.WARPED_SPORE, hLoc, 30, 0.5, 0.5, 0.5, 0.2);
            world.spawnParticle(Particle.WARPED_SPORE, sLoc, 30, 0.5, 0.5, 0.5, 0.2);
            world.spawnParticle(Particle.CRIT_MAGIC, hLoc, 10, 0, 1, 0, 0.3);
            world.spawnParticle(Particle.CRIT_MAGIC, sLoc, 10, 0, 1, 0, 0.3);
            hero.getPlayer().teleport(sLoc);
            summon.getEntity().teleport(hLoc);
        }
    }

    private void summonDouble(Hero hero) {
        Player player = hero.getPlayer();

        long duration = SkillConfigManager.getUseSetting(hero, this, "minion-duration", 6000, false);
        double launchVelocity = SkillConfigManager.getUseSetting(hero, this, "launch-velocity", 1.2, false);

        final double randomMin = SkillConfigManager.getUseSetting(hero, this, "min-launch-spread", -0.4, false);
        final double randomMax = SkillConfigManager.getUseSetting(hero, this, "max-launch-spread", 0.4, false);
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

    private class TemporalEchoesMinionEffect extends SummonEffect {
        TemporalEchoesMinionEffect(Skill skill, Hero summoner, long duration) {
            super(skill, minionEffectName, duration, summoner, null);

            types.add(EffectType.TEMPORAL);

            int speedAmplifier = SkillConfigManager.getUseSetting(summoner, skill, "minion-speed-amplifier", 1, false);

            addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, (int) (duration / 50), 0));
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            double maxHp = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-max-hp", 100.0, false);
            double hitDmg = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-attack-damage", 40.0, false);

            LivingEntity minion = monster.getEntity();
            minion.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHp);
            minion.setHealth(maxHp);
            minion.setCustomName(applier.getName());
            minion.setCustomNameVisible(true);
            monster.setDamage(hitDmg);

            if (disguiseApiLoaded) {
                if (!DisguiseAPI.isDisguised(minion)) {
                    DisguiseAPI.undisguiseToAll(minion);
                }

                PlayerDisguise disguise = new PlayerDisguise(applier);
                disguise.setKeepDisguiseOnPlayerDeath(true);
                disguise.setEntity(minion);
                disguise.setCustomDisguiseName(true); // Is this the same? as disguise.setShowName(true) ?
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
