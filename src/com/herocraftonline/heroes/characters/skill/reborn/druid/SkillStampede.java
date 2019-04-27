package com.herocraftonline.heroes.characters.skill.reborn.druid;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.effects.common.SummonEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SkillStampede extends ActiveSkill {

    private final String minionEffectName = "Stampede";
    private boolean disguiseApiLoaded;
    private final EntityType animalArray[] = {
//            EntityType.COW,
//            EntityType.PIG,
//            EntityType.LLAMA,
//            EntityType.SHEEP,
            EntityType.WOLF
//            EntityType.OCELOT
    };

    public SkillStampede(Heroes plugin) {
        super(plugin, "Stampede");
        setDescription("Call animals that obey your commands. The minion has $1 HP and deals $2 damage per hit.");
        setUsage("/skill stampede");
        setArgumentRange(0, 0);
        setIdentifiers("skill stampede");
        setTypes(SkillType.SUMMONING, SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE);

        if (Bukkit.getServer().getPluginManager().getPlugin("LibsDisguises") != null) {
            disguiseApiLoaded = true;
        }
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("minion-attack-damage", 25.0);
        config.set("minion-on-hit-slow-duration", 2000);
        config.set("minion-on-hit-slow-amplifier", 2);
        config.set("minion-max-hp", 100.0);
        config.set("minion-duration", 6000);
        config.set("minion-speed-amplifier", 2);
        config.set("launch-velocity", 2.0);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double launchVelocity = SkillConfigManager.getUseSetting(hero, this, "launch-velocity", 0.8, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, "minion-duration", 10000, false);
        int numAnimals = SkillConfigManager.getUseSetting(hero, this, "animal-summoned", 3, false);

        final double randomMin = SkillConfigManager.getUseSetting(hero, this, "min-launch-spread", -0.4, false);
        final double randomMax = SkillConfigManager.getUseSetting(hero, this, "max-launch-spread", 0.4, false);

        // Wolfs have the most reliable default AI for following and helping the player. We'll disguise it as something else later.
        for (int i = 0; i < numAnimals; i++) {
            Wolf minion = (Wolf) player.getWorld().spawnEntity(player.getEyeLocation(), EntityType.WOLF);
            minion.setOwner(player);

            final Monster monster = plugin.getCharacterManager().getMonster(minion);
            monster.setExperience(0);
            monster.addEffect(new StampedeEffect(this, hero, duration));

            Vector launchVector = player.getLocation().getDirection().normalize()
                    .add(new Vector(ThreadLocalRandom.current().nextDouble(randomMin, randomMax), 0, ThreadLocalRandom.current().nextDouble(randomMin, randomMax)))
                    .multiply(launchVelocity);

            minion.setVelocity(launchVector);
            minion.setFallDistance(-7F);
        }

        return SkillResult.NORMAL;
    }

    private class SkillListener implements Listener {
        private final Skill skill;

        SkillListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageByEntityEvent event) {
            if (event.getDamage() <= 0 || !(event.getDamager() instanceof Wolf) || !(event.getEntity() instanceof LivingEntity))
                return;

            CharacterTemplate attackerCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getDamager());
            if (!attackerCT.hasEffect(minionEffectName))
                return;

            Hero summoner = ((StampedeEffect) attackerCT.getEffect(minionEffectName)).getSummoner();

            CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            long duration = SkillConfigManager.getUseSetting(summoner, skill, "minion-on-hit-slow-duration", 2000, false);
            int amplifier = SkillConfigManager.getUseSetting(summoner, skill, "minion-on-hit-slow-amplifier", 2, false);

            defenderCT.addEffect(new SlowEffect(skill, summoner.getPlayer(), duration, amplifier));
        }
    }

    public class StampedeEffect extends SummonEffect {
        public StampedeEffect(Skill skill, Hero summoner, long duration) {
            super(skill, minionEffectName, duration, summoner, null);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            double maxHp = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-max-hp", 200.0, false);
            double hitDmg = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-attack-damage", 25.0, false);

            LivingEntity minion = monster.getEntity();
            minion.setMaxHealth(maxHp);
            minion.setHealth(maxHp);
            minion.setCustomName(ChatColor.DARK_GREEN + applier.getName() + "'s Minion");
            minion.setCustomNameVisible(true);
            monster.setDamage(hitDmg);

            // TODO: switch minion instanceof to disguiseType
            if (disguiseApiLoaded && !(minion instanceof Wolf)) {
                if (!DisguiseAPI.isDisguised(minion)) {
                    DisguiseAPI.undisguiseToAll(minion);
                }

                Random rand = new Random();
                int randomNum = rand.nextInt(animalArray.length);
                MobDisguise disguise = new MobDisguise(DisguiseType.getType(animalArray[randomNum]), true);
                disguise.setKeepDisguiseOnPlayerDeath(true);
                disguise.setEntity(minion);
                disguise.setShowName(true);
                disguise.setModifyBoundingBox(false);
                disguise.setReplaceSounds(true);
                disguise.setHearSelfDisguise(true);
                disguise.setHideHeldItemFromSelf(true);
                disguise.setHideArmorFromSelf(true);
                disguise.startDisguise();
            }
        }

        @Override
        public void removeFromMonster(Monster monster) {
            LivingEntity minion = monster.getEntity();

            if (disguiseApiLoaded && !(minion instanceof Wolf)) {
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
