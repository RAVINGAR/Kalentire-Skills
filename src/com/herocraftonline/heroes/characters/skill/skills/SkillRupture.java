package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillRupture extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillRupture(Heroes plugin) {
        super(plugin, "Rupture");
        setDescription("Deal a mighty blow to your target, dealing $1 physical damage and causing them to be wounded for the next $2 seconds. Wounded targets will take $3 damage of bleeding damage for every block that they move during the duration.");
        setUsage("/skill rupture");
        setArgumentRange(0, 0);
        setIdentifiers("skill rupture");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DEBUFFING, SkillType.ABILITY_PROPERTY_BLEED, SkillType.DAMAGING, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(30), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(0.7), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);

        double damagePerDistance = SkillConfigManager.getUseSetting(hero, this, "damage-per-distance-moved", Double.valueOf(10), false);

        String formattedDamage = Util.decFormat.format(damage);
        String formatteddamagePerDistance = Util.decFormat.format(damagePerDistance);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration).replace("$3", formatteddamagePerDistance);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(4));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(40));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), Double.valueOf(1.0));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(7500));
        node.set(SkillSetting.PERIOD.node(), Integer.valueOf(500));
        node.set("damage-per-distance-moved", Double.valueOf(10));
        node.set("distance-per-damage", Double.valueOf(1.0));
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

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(30), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(0.7), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        // Damage the target
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        // Apply our effect
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);

        double damagePerDistance = SkillConfigManager.getUseSetting(hero, this, "damage-per-distance-moved", Double.valueOf(10), false);
        double distancePerDamage = SkillConfigManager.getUseSetting(hero, this, "distance-per-damage", Double.valueOf(2.0), false);

        plugin.getCharacterManager().getCharacter(target).addEffect(new RuptureBleedEffect(this, player, period, duration, damagePerDistance, distancePerDamage));

        player.getWorld().playSound(player.getLocation(), Sound.CHEST_OPEN, 1.2F, 0.4F);

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
            types.add(EffectType.PHYSICAL);
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

            int distance = (int) Math.ceil(getLastLoc().distance(location));

            if (distance > distancePerDamage) {
                Hero applierHero = plugin.getCharacterManager().getHero(getApplier());

                double damage = (distance / distancePerDamage) * damagePerDistance;

                // Damage the target
                addSpellTarget(hero.getEntity(), applierHero);
                damageEntity(hero.getEntity(), applier, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
            }
        }

        @Override
        public void tickMonster(Monster monster) {
            LivingEntity monsterLE = monster.getEntity();
            Location location = monsterLE.getLocation();

            int distance = (int) Math.ceil(getLastLoc().distance(location));

            if (distance > distancePerDamage) {
                Hero applierHero = plugin.getCharacterManager().getHero(getApplier());

                double damage = distance * damagePerDistance;

                // Damage the target
                addSpellTarget(monsterLE, applierHero);
                damageEntity(monsterLE, applier, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
            }
        }

        public Location getLastLoc() {
            return lastLoc;
        }

        public void setLastLoc(Location lastLoc) {
            this.lastLoc = lastLoc;
        }
    }
}
