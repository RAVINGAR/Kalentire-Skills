package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SummonEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillSkeletonKnight extends ActiveSkill {

    private final String minionEffectName = "SkeletonKnight";
    private final MythicMobs mythicMobs;

    public SkillSkeletonKnight(Heroes plugin) {
        super(plugin, "SkeletonKnight");
        setDescription("Conjures a Skeleton Knight to obey your commands. " +
                "He has $1 HP, deals $2 damage per hit, and lasts for up to $3 seconds. " +
                "Your knight will cleave nearby monsters (but not players) on every hit and take $4 reduced damage from other monsters or summons. $9");
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
        config.set(SkillSetting.RADIUS.node(), 3.0);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int maxMinions = SkillConfigManager.getUseSetting(hero, this, "maximum-allowed-minions", 3, false);
        int minionCount = 0;
        for (Monster summon : hero.getSummons()) {
            if (summon.hasEffect(minionEffectName)) {
                minionCount++;
            }
            if (minionCount >= maxMinions) {
                player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You already have the maximum number of minions of this type!");
                return SkillResult.FAIL;
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

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageByEntityEvent event) {
            if (event.getDamage() <= 0 || event.getEntity() instanceof Player)
                return;
            if (!(event.getDamager() instanceof LivingEntity) || !(event.getEntity() instanceof LivingEntity))
                return;

            // Handle Mob vs Summon damage mitigation
            if (!(event.getDamager() instanceof Player)) {
                CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
                if (defenderCT.hasEffect(minionEffectName)) {
                    SkeletonKnightEffect effect = (SkeletonKnightEffect) defenderCT.getEffect(minionEffectName);
                    event.setDamage(event.getDamage() * (1.0 - effect.getPveDamageMitigation()));
                }
            }

            // Handle Summon vs Mob cleave attack
            CharacterTemplate attackerCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getDamager());
            if (attackerCT.hasEffect(minionEffectName)) {
                handleMinionCleave(event, (Monster) attackerCT);
            }
        }

        public void handleMinionCleave(EntityDamageByEntityEvent event, Monster attackerCT) {
            Hero summoner = attackerCT.getSummoner();
            LivingEntity target = (LivingEntity) event.getEntity();
            double radius = SkillConfigManager.getScaledUseSettingDouble(summoner, skill, SkillSetting.RADIUS, 3.0, false);

            for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
                if (!(entity instanceof LivingEntity) || entity.equals(event.getEntity()))
                    continue;

                LivingEntity aoeTarget = (LivingEntity) entity;
                if (aoeTarget instanceof Player)    // Only AoE mobs
                    continue;

                addSpellTarget(aoeTarget, summoner);
                damageEntity((LivingEntity) entity, summoner.getPlayer(), event.getDamage(), EntityDamageEvent.DamageCause.ENTITY_ATTACK);
            }
        }
    }

    public class SkeletonKnightEffect extends SummonEffect {
        private double pveDamageMitigation;

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

            this.pveDamageMitigation = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-pve-damage-mitigation", 0.5, false);

            double maxHp = SkillConfigManager.getScaledUseSettingDouble(getSummoner(), skill, "minion-max-hp", 400.0, false);
            double hitDmg = SkillConfigManager.getScaledUseSettingDouble(getSummoner(), skill, "minion-attack-damage", 25.0, false);

            LivingEntity minion = monster.getEntity();
            minion.setMaxHealth(maxHp);
            minion.setHealth(maxHp);
            minion.setCustomName(ChatColor.DARK_GREEN + applier.getName() + "'s Minion");
            minion.setCustomNameVisible(true);
            monster.setDamage(hitDmg);
        }

        public double getPveDamageMitigation() {
            return pveDamageMitigation;
        }
    }
}
