package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterDamageManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.ArmorUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillDualWield extends PassiveSkill {

    private static String hitCountEffectName = "DualWield-HitCount";
    private static String cooldownEffectName = "DualWield-CooldownEffect";

    private NMSHandler nmsHandler = NMSHandler.getInterface();

    public SkillDualWield(Heroes plugin) {
        super(plugin, "DualWield");
        setDescription("You are able to wield two weapons! After every $1 attack(s) with your main-hand weapon, " +
                "you will follow up with an additional attack with your offhand weapon, dealing $2% of its normal damage.");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        int attacksPerSwing = SkillConfigManager.getUseSetting(hero, this, "attacks-per-offhand-swing", 3, false);
        double damageEffectiveness = SkillConfigManager.getUseSetting(hero, this, "damage-effectiveness", 1.0, false);

        return getDescription()
                .replace("$1", attacksPerSwing + "")
                .replace("$2", Util.decFormat.format(damageEffectiveness * 100));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("minimum-duration-inbetween-hits", 500);
        config.set("max-duration-inbetween-hits", 5000);
        config.set("attacks-per-offhand-swing", 3);
        config.set("damage-effectiveness", 1.0);
        config.set("attack-delay-ticks", 5);
        return config;
    }

    public class SkillHeroListener implements Listener {
        private Skill skill;

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (!(event.getDamager() instanceof Hero) || !(event.getEntity() instanceof LivingEntity))
                return;

            Hero hero = (Hero) event.getDamager();
            if (!hero.canUseSkill(skill))
                return;

            if (hero.hasEffect(cooldownEffectName))
                return;

            Player player = hero.getPlayer();
            PlayerInventory playerInv = player.getInventory();
            ItemStack mainHand = playerInv.getItemInMainHand();
            ItemStack offHand = playerInv.getItemInOffHand();
            if (mainHand.getType() == Material.AIR || !Util.weapons.contains(mainHand.getType().name()) || offHand.getType() == Material.AIR || !Util.weapons.contains(offHand.getType().name()))
                return;

            DualWieldHitCountEffect effect = null;
            if (!hero.hasEffect(hitCountEffectName)) {
                long followupHitTimer = SkillConfigManager.getUseSetting(hero, skill, "max-duration-inbetween-hits", 5000, false);
                effect = new DualWieldHitCountEffect(skill, player, followupHitTimer);
                hero.addEffect(effect);
            } else {
                effect = (DualWieldHitCountEffect) hero.getEffect(hitCountEffectName);
            }

            /* NOTES FROM @RAVINGAR
            The WeaponDamageEvent will have a damage value that is the combination of mainhand and offhand attributes
            This skill now works through considering those attributes. It will first calculate the individual
            attributes/damage for the mainhand and use that as the new damage event. Then it will use the offhand damage
            for the offhand attacks.

            The main issue is that WeaponDamageEvent is called AFTER mitigation is already applied. So therefore we
            have to apply mitigation again.
             */
            CharacterDamageManager manager = plugin.getDamageManager();

            double mitigation = event.getDamage() - event.getRawDamage();

            event.setDamage(manager.getItemStackDamage(hero, mainHand, EquipmentSlot.HAND) - mitigation);

            effect.addHit(hero, (LivingEntity) event.getEntity(), offHand);
            int cooldownDuration = SkillConfigManager.getUseSetting(hero, skill, "minimum-duration-inbetween-hits", 500, false);
            hero.addEffect(new CooldownEffect(skill, player, cooldownDuration));
        }
    }

    // Effect required for implementing an internal cooldown on healing
    private class CooldownEffect extends ExpirableEffect {
        public CooldownEffect(Skill skill, Player applier, long duration) {
            super(skill, cooldownEffectName, applier, duration);
        }
    }

    private class DualWieldHitCountEffect extends ExpirableEffect {
        private int currentHitCount;
        private int requiredHits;
        private double damageEffectiveness;
        private int delayTicks;

        public DualWieldHitCountEffect(Skill skill, Player applier, long duration) {
            super(skill, hitCountEffectName, applier, duration);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            this.damageEffectiveness = SkillConfigManager.getUseSetting(hero, skill, "damage-effectiveness", 1.0, false);
            this.requiredHits = SkillConfigManager.getUseSetting(hero, skill, "attacks-per-offhand-swing", 3, false);
            this.delayTicks = SkillConfigManager.getUseSetting(hero, skill, "attack-delay-ticks", 5, false);
        }

        public boolean addHit(Hero hero, LivingEntity target, ItemStack offhand) {
            currentHitCount++;
            if (currentHitCount >= requiredHits) {
                triggerOffHandAttack(hero, target, offhand);
                hero.removeEffect(this);
                return true;
            }
            return false;
        }

        public void triggerOffHandAttack(Hero hero, LivingEntity target, ItemStack offHand) {
            double damage = plugin.getDamageManager().getItemStackDamage(hero, offHand, EquipmentSlot.OFF_HAND) * damageEffectiveness;
            if (damage <= 0)
                return;

            if (delayTicks <= 0) {
                doAttack(target, hero, damage);
            } else {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (target.isDead() || target.getHealth() <= 0)
                            return;

                        doAttack(target, hero, damage);
                    }
                }.runTaskLater(plugin, delayTicks);
            }
        }

        public void doAttack(LivingEntity target, Hero hero, double damage) {
            if (!damageCheck(applier, target))
                return;

            addSpellTarget(target, hero);
            float knockback = Heroes.properties.getCustomKnockback(hero.getCurrentOffhandItem());
            damageEntity(target, applier, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, knockback);
            applier.swingOffHand();
        }
    }
}