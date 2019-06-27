package com.herocraftonline.heroes.characters.skill.reborn.unused;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillNoxiousMinion extends ActiveSkill {

    private final String minionEffectName = "NoxiousMinion";
    private boolean disguiseApiLoaded;

    public SkillNoxiousMinion(Heroes plugin) {
        super(plugin, "NoxiousMinion");
        setDescription("Conjures a noxious minion to obey your commands. "
                + "The minion has $1 Health and deals $2 damage per hit. $9");
        setUsage("/skill noxiousminion");
        setArgumentRange(0, 0);
        setIdentifiers("skill noxiousminion");
        setTypes(SkillType.SUMMONING, SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_DISEASE, SkillType.SILENCEABLE);

        disguiseApiLoaded = Bukkit.getServer().getPluginManager().isPluginEnabled("LibsDisguises");
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
        config.set("minion-attack-damage", 10.0);
        config.set("minion-attack-damage-per-level", 0.2);
        config.set("minion-max-hp", 125.0);
        config.set("minion-max-hp-per-level", 1.0);
        config.set("minion-on-hit-debuff-tick-damage", 10.0);
        config.set("minion-on-hit-debuff-period", 500);
        config.set("minion-on-hit-debuff-duration", 2000);
        config.set("minion-speed-amplifier", 1);
        config.set("minion-duration", 8000);
        config.set("launch-velocity", 1.5);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

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

    private class NoxiousPoisonEffect extends PeriodicDamageEffect {
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

            int speedAmplifier = SkillConfigManager.getUseSetting(summoner, skill, "minion-speed-amplifier", 2, false);

            addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, (int) (duration / 50), 0));
            addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (duration / 50), speedAmplifier));
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            double maxHp = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-max-hp", 125.0, false);
            maxHp += SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-max-hp-per-level", 1.0, false) * getSummoner().getHeroLevel(skill);

            double hitDmg = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-attack-damage", 10.0, false);
            hitDmg += SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-attack-damage-per-level", 0.2, false) * getSummoner().getHeroLevel(skill);

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
                disguise.setEntity(minion);
                disguise.setShowName(true);
                disguise.setModifyBoundingBox(false);
                disguise.setReplaceSounds(true);
                disguise.setKeepDisguiseOnPlayerDeath(true);
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
