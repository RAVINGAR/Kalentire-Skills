package com.herocraftonline.heroes.characters.skill.reborn.defender;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

public class SkillTaunt extends TargettedSkill {

    private String applyText;
    private String expireText;
    private String tauntText;

    public SkillTaunt(Heroes plugin) {
        super(plugin, "Taunt");
        setDescription("Taunt your targetted enemy into attacking. " +
                "Taunted enemies deal $2% less damage to all targets other than you for $3 second(s).");
        setArgumentRange(0, 0);
        setUsage("/skill taunt");
        setIdentifiers("skill taunt");
        setTypes(SkillType.DEBUFFING, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.INTERRUPTING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        double maxDistance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 5.0, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 180000, false);
        double damageReduction = SkillConfigManager.getUseSetting(hero, this, "damage-reduction", 0.85, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDamageModifier = Util.decFormat.format((1 - damageReduction) * 100.0);

        return getDescription().replace("$1", maxDistance + "")
                .replace("$2", formattedDamageModifier).replace("$3", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("damage-reduction", 0.85);
        node.set(SkillSetting.MAX_DISTANCE.node(), 5.0);
        node.set(SkillSetting.COOLDOWN.node(), 18000);
        node.set(SkillSetting.DURATION.node(), 6000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% was taunted by %hero%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer taunted!");
        node.set("taunt-message-speed", 1000);
        node.set("taunt-text", "%hero% is taunting you!");

        return node;
    }

    public void init() {
        super.init();

        setUseText(getUseText());
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% was taunted by %hero%!")
                .replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% is no longer taunted!")
                .replace("%target%", "$1").replace("%hero%", "$2");
        tauntText = SkillConfigManager.getRaw(this, "taunt-text", "%hero% is taunting you!");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] strings) {
        if (target == null)
            return SkillResult.INVALID_TARGET_NO_MSG;

        Player player = hero.getPlayer();
        if (!damageCheck(player, target)) {
            player.sendMessage("You can't damage that target!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 180000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, "taunt-message-speed", 1000, false);
        double damageModifier = SkillConfigManager.getUseSetting(hero, this, "damage-reduction", 0.85, false);

        broadcastExecuteText(hero, target);

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        target.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, target.getLocation().add(0, 1, 0), 35, 1.0F, 0.5F, 1.0F, 0.2);
        targetCT.addEffect(new TauntEffect(this, player, period, duration, damageModifier));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CHICKEN_STEP, 0.8F, 0.5F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CHICKEN_HURT, 0.8F, 1.25F);

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            CharacterTemplate damager = event.getDamager();
            if (damager.hasEffect("Taunted")) {
                TauntEffect effect = (TauntEffect) damager.getEffect("Taunted");
                Player applier = effect.getApplier();

                if(applier == null || applier != event.getEntity()) {
                    double damageModifier = effect.getDamageModifier();
                    event.setDamage((event.getDamage() * damageModifier));
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            CharacterTemplate damager = event.getDamager();
            if (damager.hasEffect("Taunted")) {
                TauntEffect effect = (TauntEffect) damager.getEffect("Taunted");
                Player applier = effect.getApplier();

                if(applier == null || applier != event.getEntity()) {
                    double damageModifier = effect.getDamageModifier();
                    event.setDamage((event.getDamage() * damageModifier));
                }
            }
        }
    }

    public class TauntEffect extends PeriodicExpirableEffect {

        private double damageModifier;

        public TauntEffect(Skill skill, Player applier, long period, long duration, double damageModifier) {
            super(skill, "Taunted", applier, period, duration, applyText, expireText);

            types.add(EffectType.HARMFUL);
            types.add(EffectType.PHYSICAL);
            types.add(EffectType.TAUNT);

            this.damageModifier = damageModifier;
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();

            player.sendMessage(tauntText.replace("%hero%",ChatColor.BOLD + applier.getName() + ChatColor.RESET));
        }

        @Override
        public void tickMonster(Monster monster) {}

        public double getDamageModifier() {
            return damageModifier;
        }

        public void setDamageModifier(double damageModifier) {
            this.damageModifier = damageModifier;
        }
    }
}
