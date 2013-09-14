package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.DisarmEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillBladeGrasp extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillBladeGrasp(Heroes plugin) {
        super(plugin, "Bladegrasp");
        setDescription("Focus on your target for the next $1 seconds. While focused, you will grasp your opponent's weapon by the blade and disarm them for $2 seconds. Grasping a blade causes you to take $3% increased damage from the attack.");
        setUsage("/skill bladegrasp");
        setArgumentRange(0, 0);
        setIdentifiers("skill bladegrasp");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int focusDuration = SkillConfigManager.getUseSetting(hero, this, "focus-duration", Integer.valueOf(3000), false);
        int disarmDuration = SkillConfigManager.getUseSetting(hero, this, "disarm-duration", Integer.valueOf(3000), false);
        double damageIncreasePercent = SkillConfigManager.getUseSetting(hero, this, "damage-increase-percent", Double.valueOf(0.20), false);

        String formattedFocusDuration = Util.decFormat.format(focusDuration / 1000.0);
        String formattedDisarmDuration = Util.decFormat.format(disarmDuration / 1000.0);
        String formattedDamageIncreasePercent = Util.decFormat.format(damageIncreasePercent * 100.0);

        return getDescription().replace("$1", formattedFocusDuration).replace("$2", formattedDisarmDuration).replace("$3", formattedDamageIncreasePercent);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 7);
        node.set("focus-duration", Integer.valueOf(4000));
        node.set("disarm-duration", Integer.valueOf(4000));
        node.set("damage-increase-percent", Double.valueOf(0.2));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is focusing on %target%");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is no longer focusing on %target%.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% is focusing on %target%").replace("%hero%", "$2").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% is no longer focusing on %target%.").replace("%hero%", "$2").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;

        broadcastExecuteText(hero, target);

        int focusDuration = SkillConfigManager.getUseSetting(hero, this, "focus-duration", Integer.valueOf(3000), false);
        int disarmDuration = SkillConfigManager.getUseSetting(hero, this, "disarm-duration", Integer.valueOf(3000), false);
        double damageIncreasePercent = SkillConfigManager.getUseSetting(hero, this, "damage-increase-percent", Double.valueOf(0.20), false);

        hero.addEffect(new BladeGraspEffect(this, player, focusDuration, (Player) target, disarmDuration, damageIncreasePercent));

        player.getWorld().playSound(player.getLocation(), Sound.EXPLODE, 0.7F, 2.0F);

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.getDamage() == 0 || !(event instanceof EntityDamageByEntityEvent)) {
                return;
            }

            EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;
            Entity defender = edbe.getEntity();
            Entity attacker = edbe.getDamager();
            if (defender instanceof Player && attacker instanceof Player) {
                Player defenderPlayer = (Player) defender;
                Hero defenderHero = plugin.getCharacterManager().getHero(defenderPlayer);

                // Check if bladegrasping
                if (defenderHero.hasEffect("BladeGrasping")) {
                    Player damagerPlayer = (Player) attacker;
                    Hero damagerHero = plugin.getCharacterManager().getHero(damagerPlayer);

                    BladeGraspEffect bgEffect = (BladeGraspEffect) defenderHero.getEffect("BladeGrasping");

                    // Compare attacker to bladegrasp target
                    if (bgEffect.getFocusedTarget().equals(damagerPlayer)) {
                        Material heldItem = damagerPlayer.getItemInHand().getType();

                        // Disarm checks
                        if (!Util.isWeapon(heldItem) && !Util.isAwkwardWeapon(heldItem)) {
                            return;
                        }
                        if (damagerHero.hasEffectType(EffectType.DISARM)) {
                            return;
                        }

                        // Disarm attacker
                        long disarmDuration = bgEffect.getDisarmDuration();
                        damagerHero.addEffect(new DisarmEffect(skill, defenderPlayer, disarmDuration));

                        // Modify damage;
                        double damageModifier = 1 + bgEffect.getDamageIncreasePercent();
                        event.setDamage(event.getDamage() * damageModifier);

                        // Remove bladegrasp as it has ran it's course.
                        defenderHero.removeEffect(bgEffect);

                        damagerPlayer.getWorld().playEffect(damagerPlayer.getLocation(), Effect.EXTINGUISH, 3);
                        damagerPlayer.getWorld().playSound(damagerPlayer.getLocation(), Sound.ITEM_BREAK, 0.8F, 1.0F);

                        defenderPlayer.getWorld().playSound(defenderPlayer.getLocation(), Sound.HURT, 0.8F, 0.5F);
                    }
                }

            }
        }
    }

    public class BladeGraspEffect extends ExpirableEffect {

        private Player focusedTarget;
        private long disarmDuration;
        private double damageIncreasePercent;

        public BladeGraspEffect(Skill skill, Player applier, long duration, Player focusedTarget, long disarmDuration, double damageIncreasePercent) {
            super(skill, "BladeGrasping", applier, duration, applyText, expireText);

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);

            this.focusedTarget = focusedTarget;
            this.disarmDuration = disarmDuration;
            this.damageIncreasePercent = damageIncreasePercent;
        }

        public double getDamageIncreasePercent() {
            return damageIncreasePercent;
        }

        public void setDamageIncreasePercent(double damageIncreasePercent) {
            this.damageIncreasePercent = damageIncreasePercent;
        }

        public Player getFocusedTarget() {
            return focusedTarget;
        }

        public void setFocusedTarget(Player focusedTarget) {
            this.focusedTarget = focusedTarget;
        }

        public long getDisarmDuration() {
            return disarmDuration;
        }

        public void setDisarmDuration(long disarmDuration) {
            this.disarmDuration = disarmDuration;
        }
    }
}
