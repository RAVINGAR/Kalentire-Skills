package com.herocraftonline.heroes.characters.skill.reborn.defender;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.List;

public class SkillStoneSkin extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillStoneSkin(Heroes plugin) {
        super(plugin, "StoneSkin");
        setDescription("Your skin turns to stone for $1 second(s). While active, enemies receive $2 damage on physical attacks! " +
                "Additionally protects you $3% from $4. On expiry the stones explode off your skin causing $5% of received damage to enemies in $6 blocks$7.");
        setArgumentRange(0, 0);
        setUsage("/skill stoneskin");
        setIdentifiers("skill stoneskin", "skill sskin");
        setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.SILENCEABLE, SkillType.DEBUFFING, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        boolean useEnemyDamagePercent = SkillConfigManager.getUseSetting(hero,this,"use-enemy-damage-percent", true);
        double enemyDamagePercent = SkillConfigManager.getUseSettingDouble(hero, this, "enemy-damage-percent", false);
        double enemyDamage = SkillConfigManager.getUseSettingDouble(hero, this, "enemy-damage", false);
        double damageReduction = SkillConfigManager.getUseSettingDouble(hero, this, "damage-reduction", false);
        double endDamageModifier = SkillConfigManager.getUseSettingDouble(hero, this, "end-damage-modifier-based-on-received-damage", false);
        double endDamageRadius = SkillConfigManager.getUseSettingDouble(hero, this, "end-damage-radius", false);

        double hPower = SkillConfigManager.getUseSettingDouble(hero, this, "end-horizontal-knockback-power", false);
        double vPower = SkillConfigManager.getUseSettingDouble(hero, this, "end-vertical-knockback-power", false);

        boolean isKnockback = hPower > 0 || vPower > 0;

        boolean reduceMagicDamage = SkillConfigManager.getUseSetting(hero, this, "reduce-magic-damage",false);
        boolean resistLightning = SkillConfigManager.getUseSetting(hero, this, "resist-lightning",true);

        String damageSourcesString = String.format("physical%s attacks%s",
                reduceMagicDamage ? " and magical" : "",
                resistLightning ? ", and resists lightning." : ".");

        return getDescription().replace("$1", Util.decFormat.format(duration / 1000))
                .replace("$2", useEnemyDamagePercent ? Util.decFormat.format(enemyDamagePercent * 100) + "%" : Util.decFormat.format(enemyDamage))
                .replace("$3", Util.decFormat.format(damageReduction * 100))
                .replace("$4", damageSourcesString)
                .replace("$5", Util.decFormat.format(endDamageModifier * 100))
                .replace("$6", Util.decFormat.format(endDamageRadius))
                .replace("$7", isKnockback ? " with knockback" : "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();

        config.set("use-enemy-damage-percent", true);
        config.set("enemy-damage-percent", 0.1);
        config.set("enemy-damage", 5.0);
        config.set("damage-reduction", 0.05);
        config.set("reduce-magic-damage", false);
        config.set("resist-lightning", true);
        config.set("end-horizontal-knockback-power", 1.0);
        config.set("end-vertical-knockback-power", 0.2);
        config.set("end-damage-modifier-based-on-received-damage", 0.2);
        config.set("end-damage-radius", 3.0);
        config.set(SkillSetting.COOLDOWN.node(), 16000);
        config.set(SkillSetting.DURATION.node(), 8000);
        config.set(SkillSetting.USE_TEXT.node(), null); // remove entry
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s skin has turned to stone.");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s skin softens.");
        return config;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero%'s skin has turned to stone.")
                .replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero%'s skin softens.")
                .replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        double damageReduction = SkillConfigManager.getUseSetting(hero, this, "damage-reduction", 0.05, false);

        hero.addEffect(new StoneSkinEffect(this, player, duration, damageReduction, applyText, expireText));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_THUNDER, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        private Skill skill;
        public SkillHeroListener(Skill skill){
            this.skill = skill;
        }

        //FIXME: should I used monitor or just highest? As it possibly may cancel the event.
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.getDamage() == 0 || !(event.getEntity() instanceof Player))
                return;

            Hero defenderHero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (!defenderHero.hasEffect("StoneSkin"))
                return;

            // Harm attacker for physical attacks
            if (event.getSkill().isType(SkillType.ABILITY_PROPERTY_PHYSICAL)) {
                boolean useEnemyDamagePercent = SkillConfigManager.getUseSetting(defenderHero, skill,"use-enemy-damage-percent", true);
                double enemyDamagePercent = SkillConfigManager.getUseSettingDouble(defenderHero, skill, "enemy-damage-percent", false);
                double enemyDamage = SkillConfigManager.getUseSettingDouble(defenderHero, skill, "enemy-damage", false);

                double damage = useEnemyDamagePercent ? (enemyDamagePercent * event.getDamage()) : enemyDamage;
                if (damage > 0){
                    LivingEntity attacker = event.getDamager().getEntity();
                    plugin.getDamageManager().addSpellTarget(attacker, defenderHero, skill);
                    damageEntity(attacker, defenderHero.getEntity(), damage, false); //FIXME: use a damage cause or just custom damage cause?
                }
            }

            // Handle reduction of magic based damage.
            // Also Consider resisting lightning over the requirement to reduce magic damage
            boolean reduceMagicDamage = SkillConfigManager.getUseSetting(defenderHero, skill, "reduce-magic-damage",false);
            boolean resistLightning = SkillConfigManager.getUseSetting(defenderHero, skill, "resist-lightning",true);
            boolean isLightningSkill = event.getSkill().isType(SkillType.ABILITY_PROPERTY_LIGHTNING);
            boolean isMagicalSkill = event.getSkill().isType(SkillType.ABILITY_PROPERTY_MAGICAL);
            if (isLightningSkill && resistLightning){
                // Resist lightning damage
                event.setCancelled(true);
            } else {
                StoneSkinEffect stoneSkinEffect = (StoneSkinEffect) defenderHero.getEffect("StoneSkin");
                if (!isMagicalSkill || reduceMagicDamage) {
                    //Reduce non-magical skill damage (e.g. physical or projectile) or magical damage only when enabled
                    double damageFactor = 1.0 - stoneSkinEffect.getDamageReduction();
                    event.setDamage((event.getDamage() * damageFactor));
                }
                //Record damage
                if (event.getDamage() > 0) {
                    stoneSkinEffect.increaseDamageReceived(event.getDamage());
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event){
            if (event.getDamage() == 0 || !(event.getEntity() instanceof Player))
                return;

            Hero defenderHero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (!defenderHero.hasEffect("StoneSkin"))
                return;

            StoneSkinEffect stoneSkinEffect = (StoneSkinEffect) defenderHero.getEffect("StoneSkin");
            double damageFactor = 1.0 - stoneSkinEffect.getDamageReduction();
            event.setDamage((event.getDamage() * damageFactor));
            if (event.getDamage() > 0) {
                stoneSkinEffect.increaseDamageReceived(event.getDamage());
            }

            // Harm attacker for physical attacks
            boolean useEnemyDamagePercent = SkillConfigManager.getUseSetting(defenderHero, skill,"use-enemy-damage-percent", true);
            double enemyDamagePercent = SkillConfigManager.getUseSettingDouble(defenderHero, skill, "enemy-damage-percent", false);
            double enemyDamage = SkillConfigManager.getUseSettingDouble(defenderHero, skill, "enemy-damage", false);

            double damage = useEnemyDamagePercent ? (enemyDamagePercent * event.getDamage()) : enemyDamage;
            if (damage > 0){
                LivingEntity attacker = event.getDamager().getEntity();
                plugin.getDamageManager().addSpellTarget(attacker, defenderHero, skill);
                damageEntity(attacker, defenderHero.getEntity(), damage, false); //FIXME: use a damage cause or just custom damage cause?
            }
        }

    }

    public class StoneSkinEffect extends ExpirableEffect {
        private final double damageReduction;
        private double damageReceived;

        public StoneSkinEffect(Skill skill, Player applier, long duration, double damageReduction,
                               String applyText, String expireText) {
            super(skill, "StoneSkin", applier, duration, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.EARTH);
            types.add(EffectType.BENEFICIAL);

            this.damageReduction = damageReduction;
        }

        public double getDamageReduction() {
            return damageReduction;
        }

        public void increaseDamageReceived(double damage){
            this.damageReceived += damage;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            performKnockback(hero, damageReceived);
        }
    }

    private void performKnockback(Hero hero, double receivedDamage) {
        Player player = hero.getPlayer();

        //TODO add explosion particle?

        double hPower = SkillConfigManager.getUseSettingDouble(hero, this, "end-horizontal-knockback-power", false);
        double vPower = SkillConfigManager.getUseSettingDouble(hero, this, "end-vertical-knockback-power", false);
        double endDamagePercentPerReceivedDamage = SkillConfigManager.getUseSettingDouble(hero, this, "end-damage-modifier-based-on-received-damage", false);
        double radius = SkillConfigManager.getUseSettingDouble(hero, this, "end-damage-radius", false);

        double damage = endDamagePercentPerReceivedDamage * receivedDamage;

        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        //TODO: remove debug message
        hero.getPlayer().sendMessage("Received " + Util.decFormat.format(receivedDamage)
                + " damage during stoneskin and applied " + Util.decFormat.format(damage)
                + " to " + Util.decFormat.format(entities.size()) + " enemies.");
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity))
                continue;
            if (!damageCheck(player, (LivingEntity) entity))
                continue;

            final LivingEntity target = (LivingEntity) entity;
            if (damage > 0) {
                plugin.getDamageManager().addSpellTarget(target, hero, this);
                damageEntity(target, player, damage);
            }

            // Do our knockback
            Location playerLoc = player.getLocation();
            Location targetLoc = target.getLocation();

            double xDir = targetLoc.getX() - playerLoc.getX();
            double zDir = targetLoc.getZ() - playerLoc.getZ();
            double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

            xDir = xDir / magnitude * hPower;
            zDir = zDir / magnitude * hPower;

            final Vector velocity = new Vector(xDir, vPower, zDir);
            target.setVelocity(velocity);
        }
    }
}
