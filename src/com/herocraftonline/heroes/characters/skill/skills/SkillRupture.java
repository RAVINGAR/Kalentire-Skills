package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent;

public class SkillRupture extends TargettedSkill {

    private boolean ncpEnabled = false;

    private String applyText;
    private String expireText;

    public SkillRupture(Heroes plugin) {
        super(plugin, "Rupture");
        setDescription("Deal a mighty blow to your target, dealing $1 physical damage and causing them to be wounded for the next $2 seconds. Wounded targets will take $3 bleeding damage for every block that they move during the duration.");   // If a target is dealt more than $4 damage from bleeding, the effect will be removed, and the target will be slowed for $5 seconds.");
        setUsage("/skill rupture");
        setArgumentRange(0, 0);
        setIdentifiers("skill rupture");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DEBUFFING, SkillType.ABILITY_PROPERTY_BLEED, SkillType.DAMAGING, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(30), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.7, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);

        double damagePerDistance = SkillConfigManager.getUseSetting(hero, this, "damage-per-distance-moved", (double) 10, false);

        String formattedDamage = Util.decFormat.format(damage);
        String formatteddamagePerDistance = Util.decFormat.format(damagePerDistance);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration).replace("$3", formatteddamagePerDistance);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set(SkillSetting.DAMAGE.node(), 40);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.0);
        node.set(SkillSetting.DURATION.node(), 7500);
        node.set(SkillSetting.PERIOD.node(), 500);
        node.set("damage-per-distance-moved", (double) 10);
        node.set("distance-per-damage", 1.0);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% is wounded deeply!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% is no longer wounded.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%target% is wounded deeply!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%target% is no longer wounded.").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        if (ncpEnabled) {
            if (!player.isOp()) {
                NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this);
                hero.addEffect(ncpExemptEffect);
            }
        }
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(30), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.7, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        // Damage the target
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        // Apply our effect
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);

        double damagePerDistance = SkillConfigManager.getUseSetting(hero, this, "damage-per-distance-moved", (double) 10, false);
        double distancePerDamage = SkillConfigManager.getUseSetting(hero, this, "distance-per-damage", 2.0, false);

        plugin.getCharacterManager().getCharacter(target).addEffect(new RuptureBleedEffect(this, player, period, duration, damagePerDistance, distancePerDamage));

        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().playSound(player.getLocation(), Sound.CHEST_OPEN, 1.2F, 0.4F);

        if (ncpEnabled) {
            if (!player.isOp()) {
                if (hero.hasEffect("NCPExemptionEffect_FIGHT"))
                    hero.removeEffect(hero.getEffect("NCPExemptionEffect_FIGHT"));
            }
        }
        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        public SkillEntityListener() {}

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerTeleport(PlayerTeleportEvent event) {
            Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());

            if (hero.hasEffect("RuptureBleed")) {
                RuptureBleedEffect rbEffect = (RuptureBleedEffect) hero.getEffect("RuptureBleed");
                rbEffect.setLastLoc(event.getTo());
            }
        }
    }

    public class RuptureBleedEffect extends PeriodicExpirableEffect {

        private double damagePerDistance;
        private double distancePerDamage;
        private Location lastLoc;

        public RuptureBleedEffect(Skill skill, Player applier, long period, long duration, double damagePerDistance, double distancePerDamage) {
            super(skill, "RuptureBleed", applier, period, duration, applyText, expireText);

            types.add(EffectType.BLEED);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.DAMAGING);

            this.damagePerDistance = damagePerDistance;
            this.distancePerDamage = distancePerDamage;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            setLastLoc(hero.getPlayer().getLocation());
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            setLastLoc(monster.getEntity().getLocation());
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();
            Location location = player.getLocation();

            int distance = (int) Math.floor(getLastLoc().distance(location));

            if (distance > distancePerDamage) {
                Hero applierHero = plugin.getCharacterManager().getHero(getApplier());

                double damage = (distance / distancePerDamage) * damagePerDistance;

                // Damage the target
                addSpellTarget(hero.getEntity(), applierHero);
                damageEntity(hero.getEntity(), applier, damage, DamageCause.MAGIC, false);

                lastLoc = location;

                Messaging.send(player, ChatColor.DARK_RED + "Your reckless movements are causing your wounds to rupture!");
            }
        }

        @Override
        public void tickMonster(Monster monster) {
            LivingEntity monsterLE = monster.getEntity();
            Location location = monsterLE.getLocation();

            int distance = (int) Math.floor(getLastLoc().distance(location));

            if (distance > distancePerDamage) {
                Hero applierHero = plugin.getCharacterManager().getHero(getApplier());

                double damage = (distance / distancePerDamage) * damagePerDistance;

                // Damage the target
                addSpellTarget(monsterLE, applierHero);
                damageEntity(monsterLE, applier, damage, DamageCause.MAGIC, false);

                lastLoc = location;
            }
        }

        public Location getLastLoc() {
            return lastLoc;
        }

        public void setLastLoc(Location lastLoc) {
            this.lastLoc = lastLoc;
        }
    }

    private class NCPExemptionEffect extends Effect {

        public NCPExemptionEffect(Skill skill) {
            super(skill, "NCPExemptionEffect_FIGHT");
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.FIGHT);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.FIGHT);
        }
    }
}
