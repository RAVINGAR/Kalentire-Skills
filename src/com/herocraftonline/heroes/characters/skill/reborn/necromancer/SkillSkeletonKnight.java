package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SummonEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillSkeletonKnight extends ActiveSkill {

    private final String minionEffectName = "SkeletonKnight";
    private final MythicMobs mythicMobs;

    public SkillSkeletonKnight(Heroes plugin) {
        super(plugin, "SkeletonKnight");
        setDescription("Conjures a Skeleton Knight to obey your commands. " +
                "He has $1 HP, deals $2 damage per hit, and lasts for up to $3 seconds. " +
                "Your knight will cleave nearby enemies on every hit at a $4% damage rate. $9");
        setUsage("/skill skeletonknight");
        setIdentifiers("skill skeletonknight");
        setArgumentRange(0, 0);
        setTypes(SkillType.SUMMONING, SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE);

        if (Bukkit.getServer().getPluginManager().getPlugin("MythicMobs") != null) {
            this.mythicMobs = MythicMobs.inst();
        } else {
            this.mythicMobs = null;
        }

        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        double maxHp = SkillConfigManager.getUseSetting(hero, this, "minion-max-hp", 400.0, false);
        maxHp += SkillConfigManager.getUseSetting(hero, this, "minion-max-hp-per-level", 4.0, false) * hero.getHeroLevel(this);

        double hitDmg = SkillConfigManager.getUseSetting(hero, this, "minion-attack-damage", 25.0, false);
        hitDmg += SkillConfigManager.getUseSetting(hero, this, "minion-attack-damage-per-level", 0.4, false) * hero.getHeroLevel(this);
        double cleaveDamageMultiplier = SkillConfigManager.getUseSetting(hero, this, "minion-cleave-damage-multiplier", 1.0, false);

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
                .replace("$4", Util.decFormat.format(cleaveDamageMultiplier))
                .replace("$9", speedText);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("mythic-mobs-mob-override-name", "SkeletonKnight");
        config.set("minion-pve-damage-mitigation", 0.5);
        config.set("maximum-allowed-minions", 3);
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

        int numSummons = SkillConfigManager.getUseSetting(hero, this, "number-minions-summoned", 2, false);
        int maxMinions = SkillConfigManager.getUseSetting(hero, this, "maximum-allowed-minions", 3, false);
        int minionCount = 0;

        long oldestMinionRemainingTime = Long.MAX_VALUE;
        Monster oldestMinion = null;
        for (Monster summon : hero.getSummons()) {
            if (summon.hasEffect(minionEffectName)) {
                long remainingTime = ((SkeletonKnightEffect) summon.getEffect(minionEffectName)).getRemainingTime();
                if (remainingTime < oldestMinionRemainingTime) {
                    oldestMinionRemainingTime = remainingTime;
                    oldestMinion = summon;
                }
                minionCount++;
            }
            if (minionCount >= maxMinions && oldestMinion != null) {
                player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You already have the maximum number of allowed minions. Your oldest minion was replaced.");
                oldestMinion.clearEffects();
            }
        }

        if (mythicMobs == null) {
            player.sendMessage("This skill does not work without mythic mobs. Contact a server admin to get this resolved!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);

        double launchVelocity = SkillConfigManager.getUseSetting(hero, this, "launch-velocity", 2.0, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, "minion-duration", 45000, false);

        String mythicMobMinionName = SkillConfigManager.getUseSetting(hero, this, "mythic-mobs-mob-override-name", "SkeletonKnight");
        ActiveMob mob = mythicMobs.getMobManager().spawnMob(mythicMobMinionName, player.getEyeLocation());
        mob.setOwner(player.getUniqueId());

        final Monster monster = plugin.getCharacterManager().getMonster(mob.getLivingEntity());
        monster.setExperience(0);
        monster.addEffect(new SkeletonKnightEffect(this, hero, duration));

        mob.getLivingEntity().setVelocity(player.getLocation().getDirection().normalize().multiply(launchVelocity));
        mob.getLivingEntity().setFallDistance(-7F);

        return SkillResult.NORMAL;
    }

    private class SkillListener implements Listener {
        private final Skill skill;

        SkillListener(Skill skill) {
            this.skill = skill;
        }

//        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
//        public void onEntityDamage(EntityDamageByEntityEvent event) {
//            if (event.getDamage() <= 0 || event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK)
//                return;
//            if (!(event.getDamager() instanceof LivingEntity) || !(event.getEntity() instanceof LivingEntity))
//                return;
//
//            // Handle Summon cleave attack
//            if (!(event.getDamager() instanceof Player)) {
//                Monster attackerM = plugin.getCharacterManager().getMonster((LivingEntity) event.getDamager());
//                if (attackerM.hasEffect(minionEffectName)) {
//                    SkeletonKnightEffect effect = (SkeletonKnightEffect) attackerM.getEffect(minionEffectName);
//                    handleMinionCleave(event, attackerM, effect);
//                }
//            }
//        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.getDamage() <= 0.0 || event.getSkill() == null || event.getEntity() instanceof Player || !(event.getEntity() instanceof LivingEntity))
                return;
            if (!event.getSkill().getTypes().contains(SkillType.AREA_OF_EFFECT))
                return;

            Monster defenderM = plugin.getCharacterManager().getMonster((LivingEntity) event.getEntity());
            if (defenderM.hasEffect(minionEffectName)) {
                SkeletonKnightEffect effect = (SkeletonKnightEffect) defenderM.getEffect(minionEffectName);
                event.setDamage(event.getDamage() * (1.0 - effect.getAoeDamageReductionPercent()));
            }
        }

        public void handleMinionCleave(EntityDamageByEntityEvent event, Monster summon, SkeletonKnightEffect effect) {
            Hero summoner = summon.getSummoner();
            LivingEntity summonLE = summon.getEntity();
            LivingEntity baseTarget = (LivingEntity) event.getEntity();
            double radius = effect.getCleaveRadius();
            double damage = event.getDamage() * effect.getCleaveDamageMultiplier();

            for (Entity entity : baseTarget.getNearbyEntities(radius, radius, radius)) {
                if (!(entity instanceof LivingEntity) || entity.equals(baseTarget))
                    continue;

                LivingEntity aoeTarget = (LivingEntity) entity;
                if (!damageCheck(summoner.getPlayer(), aoeTarget))
                    continue;

                addSpellTarget(aoeTarget, summoner);
                damageEntity(aoeTarget, summonLE, damage, EntityDamageEvent.DamageCause.CUSTOM, false); // If you change this to ENTITY_ATTACK you will get an infinite loop.
            }
        }
    }

    public class SkeletonKnightEffect extends SummonEffect {
        private double cleaveRadius;
        private double cleaveDamageMultiplier;
        private double aoeDamageReductionPercent;

        SkeletonKnightEffect(Skill skill, Hero summoner, long duration) {
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

            this.cleaveRadius = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-cleave-radius", 3.0, false);
            this.cleaveDamageMultiplier = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-cleave-damage-multiplier", 1.0, false);

            double maxHp = SkillConfigManager.getScaledUseSettingDouble(getSummoner(), skill, "minion-max-hp", 400.0, false);
            double hitDmg = SkillConfigManager.getScaledUseSettingDouble(getSummoner(), skill, "minion-attack-damage", 25.0, false);
            this.aoeDamageReductionPercent = SkillConfigManager.getScaledUseSettingDouble(getSummoner(), skill, "aoe-damage-reduction-percent", 0.5, false);

            LivingEntity minion = monster.getEntity();
            minion.setMaxHealth(maxHp);
            minion.setHealth(maxHp);
            minion.setCustomName(ChatColor.DARK_GREEN + applier.getName() + "'s Minion");
            minion.setCustomNameVisible(true);
            monster.setDamage(hitDmg);
        }

        public double getCleaveRadius() {
            return cleaveRadius;
        }

        public double getCleaveDamageMultiplier() {
            return cleaveDamageMultiplier;
        }

        public double getAoeDamageReductionPercent() {
            return aoeDamageReductionPercent;
        }
    }
}
