package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.effects.common.SummonEffect;
import com.herocraftonline.heroes.characters.skill.*;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.*;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillNoxiousMinion extends ActiveSkill {

    private final String minionEffectName = "NoxiousMinion";
    private boolean disguiseApiLoaded;

    public SkillNoxiousMinion(Heroes plugin) {
        super(plugin, "NoxiousMinion");
        setDescription("Conjures a noxious minion to obey your commands. "
                + "The minion has $1 Health and deals $2 damage per hit.");
        setUsage("/skill noxiousminion");
        setArgumentRange(0, 0);
        setIdentifiers("skill noxiousminion");
        setTypes(SkillType.SUMMONING, SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_DISEASE, SkillType.SILENCEABLE);

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
        config.set("minion-attack-damage", 50.0);
        config.set("minion-on-hit-debuff-tick-damage", 15.0);
        config.set("minion-on-hit-debuff-period", 500);
        config.set("minion-on-hit-debuff-duration", 2000);
        config.set("minion-max-hp", 200.0);
        config.set("minion-speed-amplifier", 2);
        config.set("minion-duration", 10000);
        config.set("launch-velocity", 1.5);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double launchVelocity = SkillConfigManager.getUseSetting(hero, this, "launch-velocity", 1.5, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, "minion-duration", 10000, false);

        // Wolfs have the most reliable default AI for following and helping the player. We'll disguise it as something else later.
        Wolf minion = (Wolf) player.getWorld().spawnEntity(player.getEyeLocation(), EntityType.WOLF);
        minion.setOwner(player);

        final Monster monster = plugin.getCharacterManager().getMonster(minion);
        monster.setExperience(0);
        monster.addEffect(new NoxiousMinionEffect(this, hero, duration));

        minion.setVelocity(player.getLocation().getDirection().normalize().multiply(launchVelocity));
        minion.setFallDistance(-7F);

        return SkillResult.NORMAL;
    }

    private class SkillListener implements Listener {
        private final Skill skill;

        SkillListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageByEntityEvent event) {
            if (event.getDamage() == 0 || !(event.getDamager() instanceof Wolf) || !(event.getEntity() instanceof LivingEntity))
                return;

            CharacterTemplate attackerCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getDamager());
            if (!attackerCT.hasEffect(minionEffectName))
                return;

            Hero summoner = ((NoxiousMinionEffect) attackerCT.getEffect(minionEffectName)).getSummoner();

            CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            double tickDamage = SkillConfigManager.getUseSetting(summoner, skill, "minion-on-hit-debuff-tick-damage", 15.0, false);
            long period = SkillConfigManager.getUseSetting(summoner, skill, "minion-on-hit-debuff-period", 500, false);
            long duration = SkillConfigManager.getUseSetting(summoner, skill, "minion-on-hit-debuff-duration", 2000, false);

            defenderCT.addEffect(new NoxiousPoisonEffect(skill, summoner.getPlayer(), period, duration, tickDamage));
        }
    }

    private static class NoxiousPoisonEffect extends PeriodicDamageEffect {
        NoxiousPoisonEffect(Skill skill, Player applier, long period, long duration, double tickDamage) {
            super(skill, "NoxiousPoison", applier, period, duration, tickDamage, false, null, null);

            types.add(EffectType.POISON);
            addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (duration / 50), 0));
        }
    }

    private class NoxiousMinionEffect extends SummonEffect {
        NoxiousMinionEffect(Skill skill, Hero summoner, long duration) {
            super(skill, minionEffectName, duration, summoner, null);

            types.add(EffectType.POISON);
            //types.add(EffectType.WATER_BREATHING);

            int speedAmplifier = SkillConfigManager.getUseSetting(summoner, skill, "minion-speed-amplifier", 2, false);

            addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, (int) (duration / 50), 0));
            addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (duration / 50), speedAmplifier));
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            double maxHp = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-max-hp", 500.0, false);
            double hitDmg = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-attack-damage", 50.0, false);

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

                MobDisguise disguise = new MobDisguise(DisguiseType.getType(EntityType.CAVE_SPIDER), true);
//                PlayerDisguise disguise = new PlayerDisguise(applier);
                disguise.setKeepDisguiseOnPlayerDeath(true);
                disguise.setEntity(minion);
                disguise.setCustomDisguiseName(true); // Is this the same? as disguise.setShowName(true) ?
                disguise.setModifyBoundingBox(false);
                disguise.setReplaceSounds(true);
//                LivingWatcher watcher = disguise.getWatcher();
//                watcher.setCustomName(ChatColor.DARK_GREEN + applier.getName() + "'s Minion");
//                watcher.setArmor(applier.getInventory().getArmorContents().clone());
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
