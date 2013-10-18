package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillIntervene extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillIntervene(Heroes plugin) {
        super(plugin, "Intervene");
        setDescription("Mark your target for Intervention for the next $1 seconds. While active, if you are within $2 blocks of your target when they are attacked, you will intervene the attack, taking $3% of the damage for them.");
        setUsage("/skill intervene <Target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill intervene");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.BUFFING, SkillType.NO_SELF_TARGETTING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(8000), false);
        double damageSplitPercent = SkillConfigManager.getUseSetting(hero, this, "damage-split-percent", Double.valueOf(0.50), false);

        int distanceRequired = SkillConfigManager.getUseSetting(hero, this, "distance-required-for-intervene", Integer.valueOf(5), false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDamageSplitPercent = Util.decFormat.format(damageSplitPercent * 100.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", distanceRequired + "").replace("$3", formattedDamageSplitPercent);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 7);
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(8000));
        node.set("damage-split-percent", Double.valueOf(0.5));
        node.set("distance-required-for-intervene", Integer.valueOf(5));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is intervening attacks against %target%");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is no longer intervening attacks against %target%.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% is intervening attacks against %target%").replace("%hero%", "$2").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% is no longer intervening attacks against %target%.").replace("%hero%", "$2").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;

        Hero tHero = plugin.getCharacterManager().getHero((Player) target);
        if (tHero.hasEffect("InterveneTarget")) {
            Messaging.send(player, "You cannot intervene that target right now!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero, target);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(8000), false);
        double damageSplitPercent = SkillConfigManager.getUseSetting(hero, this, "damage-split-percent", Double.valueOf(0.50), false);
        int distanceRequired = SkillConfigManager.getUseSetting(hero, this, "distance-required-for-intervene", Integer.valueOf(5), false);

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        targetHero.addEffect(new InterveneEffect(this, player, duration, damageSplitPercent, distanceRequired));

        player.getWorld().playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.getDamage() == 0)
                return;

            if (event.getEntity() instanceof Player && event.getDamager().getEntity() instanceof Player) {
                Player defenderPlayer = (Player) event.getEntity();
                Hero defenderHero = plugin.getCharacterManager().getHero(defenderPlayer);

                // Check if interveneing
                if (defenderHero.hasEffect("InterveneTarget")) {
                    InterveneEffect bgEffect = (InterveneEffect) defenderHero.getEffect("InterveneTarget");

                    LivingEntity damagerLE = event.getDamager().getEntity();
                    Player interveningPlayer = bgEffect.getApplier();

                    int distance = bgEffect.getDistanceRequired();
                    int distanceSquared = distance * distance;

                    if (interveningPlayer.getLocation().distanceSquared(defenderPlayer.getLocation()) > distanceSquared)
                        return;

                    if (!damageCheck(damagerLE, defenderPlayer) || !damageCheck(damagerLE, interveningPlayer))
                        return;

                    // Modify damage;
                    double damageSplitPercent = bgEffect.getDamageSplitPercent();

                    double targetDamageModifier = 1.0 - damageSplitPercent;
                    double targetDamage = event.getDamage() * targetDamageModifier;

                    double intervenerDamageModifier = 1.0 - targetDamageModifier;
                    double intervenerDamage = event.getDamage() * intervenerDamageModifier;

                    event.setDamage(targetDamage);

                    CharacterTemplate damagerCT = plugin.getCharacterManager().getCharacter(damagerLE);
                    addSpellTarget((LivingEntity) interveningPlayer, (Hero) damagerCT);

                    if (skill.isType(SkillType.ABILITY_PROPERTY_PHYSICAL) && !skill.isType(SkillType.ARMOR_PIERCING))
                        damageEntity((LivingEntity) interveningPlayer, (Player) damagerLE, intervenerDamage, DamageCause.ENTITY_ATTACK, false);
                    else
                        damageEntity((LivingEntity) interveningPlayer, (Player) damagerLE, intervenerDamage, DamageCause.MAGIC, false);

                    interveningPlayer.getWorld().playSound(interveningPlayer.getLocation(), Sound.HURT, 0.8F, 0.5F);
                }

            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getDamage() == 0)
                return;

            if (event.getEntity() instanceof Player && event.getDamager().getEntity() instanceof Player) {
                Player defenderPlayer = (Player) event.getEntity();
                Hero defenderHero = plugin.getCharacterManager().getHero(defenderPlayer);

                // Check if interveneing
                if (defenderHero.hasEffect("InterveneTarget")) {
                    InterveneEffect bgEffect = (InterveneEffect) defenderHero.getEffect("InterveneTarget");

                    LivingEntity damagerLE = event.getDamager().getEntity();
                    Player interveningPlayer = bgEffect.getApplier();

                    int distance = bgEffect.getDistanceRequired();
                    int distanceSquared = distance * distance;

                    if (interveningPlayer.getLocation().distanceSquared(defenderPlayer.getLocation()) > distanceSquared)
                        return;

                    if (!damageCheck(damagerLE, defenderPlayer) || !damageCheck(damagerLE, interveningPlayer))
                        return;

                    // Modify damage;
                    double damageSplitPercent = bgEffect.getDamageSplitPercent();

                    double targetDamageModifier = 1.0 - damageSplitPercent;
                    double targetDamage = event.getDamage() * targetDamageModifier;

                    double intervenerDamageModifier = 1.0 - targetDamageModifier;
                    double intervenerDamage = event.getDamage() * intervenerDamageModifier;

                    event.setDamage(targetDamage);

                    CharacterTemplate damagerCT = plugin.getCharacterManager().getCharacter(damagerLE);
                    addSpellTarget((LivingEntity) interveningPlayer, (Hero) damagerCT);
                    damageEntity((LivingEntity) interveningPlayer, (Player) damagerLE, intervenerDamage, DamageCause.ENTITY_ATTACK, false);

                    interveningPlayer.getWorld().playSound(interveningPlayer.getLocation(), Sound.HURT, 0.8F, 0.5F);
                }

            }
        }
    }

    public class InterveneEffect extends ExpirableEffect {

        private int distanceRequired;
        private double damageSplitPercent;

        public InterveneEffect(Skill skill, Player applier, long duration, double damageSplitPercent, int distanceRequired) {
            super(skill, "InterveneTarget", applier, duration, applyText, expireText);

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);

            this.damageSplitPercent = damageSplitPercent;
            this.distanceRequired = distanceRequired;
        }

        public double getDamageSplitPercent() {
            return damageSplitPercent;
        }

        public void setDamageSplitPercent(double damageSplitPercent) {
            this.damageSplitPercent = damageSplitPercent;
        }

        public int getDistanceRequired() {
            return distanceRequired;
        }

        public void setDistanceRequired(int distanceRequired) {
            this.distanceRequired = distanceRequired;
        }
    }
}
