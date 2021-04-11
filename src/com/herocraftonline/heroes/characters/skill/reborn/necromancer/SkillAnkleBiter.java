package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillAnkleBiter extends ActiveSkill {

    private final String minionEffectName = "AnkleBiter";
    private boolean disguiseApiLoaded;

    public SkillAnkleBiter(Heroes plugin) {
        super(plugin, "AnkleBiter");
        setDescription("Conjures a noxious minion to obey your commands. The minion has $1 HP and deals $2 damage per hit.");
        setUsage("/skill anklebiter");
        setArgumentRange(0, 0);
        setIdentifiers("skill anklebiter");
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

        double launchVelocity = SkillConfigManager.getUseSetting(hero, this, "launch-velocity", 2.0, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, "minion-duration", 10000, false);

        // Wolfs have the most reliable default AI for following and helping the player. We'll disguise it as something else later.
        Wolf minion = (Wolf) player.getWorld().spawnEntity(player.getEyeLocation(), EntityType.WOLF);
        minion.setOwner(player);

        final Monster monster = plugin.getCharacterManager().getMonster(minion);
        monster.setExperience(0);
        monster.addEffect(new AnkleBiterEffect(this, hero, duration));

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
            if (event.getDamage() <= 0 || !(event.getDamager() instanceof Wolf) || !(event.getEntity() instanceof LivingEntity))
                return;

            CharacterTemplate attackerCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getDamager());
            if (!attackerCT.hasEffect(minionEffectName))
                return;

            Hero summoner = ((AnkleBiterEffect) attackerCT.getEffect(minionEffectName)).getSummoner();

            CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            long duration = SkillConfigManager.getUseSetting(summoner, skill, "minion-on-hit-slow-duration", 2000, false);
            int amplifier = SkillConfigManager.getUseSetting(summoner, skill, "minion-on-hit-slow-amplifier", 2, false);

            defenderCT.addEffect(new SlowEffect(skill, summoner.getPlayer(), duration, amplifier));
        }
    }

    public class AnkleBiterEffect extends SummonEffect {
        public AnkleBiterEffect(Skill skill, Hero summoner, long duration) {
            super(skill, minionEffectName, duration, summoner, null);

            types.add(EffectType.DISEASE);
            types.add(EffectType.WATER_BREATHING);

            int speedAmplifier = SkillConfigManager.getUseSetting(summoner, skill, "minion-speed-amplifier", 2, false);

            addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, (int) (duration / 50), 0));
            addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (duration / 50), speedAmplifier));
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

            if (disguiseApiLoaded) {
                if (!DisguiseAPI.isDisguised(minion)) {
                    DisguiseAPI.undisguiseToAll(minion);
                }

                MobDisguise disguise = new MobDisguise(DisguiseType.getType(EntityType.ZOMBIE), false);
                disguise.setKeepDisguiseOnPlayerDeath(true);
                disguise.setEntity(minion);
                disguise.setCustomDisguiseName(true); // Is this the same? as disguise.setShowName(true) ?
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
