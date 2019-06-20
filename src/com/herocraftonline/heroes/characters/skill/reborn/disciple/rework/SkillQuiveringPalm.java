package com.herocraftonline.heroes.characters.skill.reborn.disciple.rework;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
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
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillQuiveringPalm extends TargettedSkill {
    private String effectName;
    private String applyText;
    private String expireText;

    public SkillQuiveringPalm(Heroes plugin) {
        super(plugin, "QuiveringPalm");
        setDescription("Strike your target with a Quivering Palm dealing $1 damage and weakening the target, causing them to take $2% increased melee damage for $3 second(s). " +
                "The strike also disorients the target, causing nausea.");
        setUsage("/skill quiveringpalm");
        setIdentifiers("skill quiveringpalm");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.DEBUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new QuiveringPalmListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        double damageMultiplier = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 1.2D, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 8000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage))
                .replace("$2", Util.decFormat.format((damageMultiplier - 1.0) * 100.0))
                .replace("$3", Util.decFormat.format(duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 4.0);
        config.set(SkillSetting.DURATION.node(), 10000);
        config.set(SkillSetting.DAMAGE.node(), 40.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.0);
        config.set("damage-multiplier", 1.15);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is weakened by a " + ChatColor.BOLD + "QuiveringPalm" + ChatColor.BOLD + "!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has recovered from the effects of the " + ChatColor.BOLD + "QuiveringPalm" + ChatColor.BOLD + "!");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% is weakened by a " + ChatColor.BOLD + "QuiveringPalm" + ChatColor.BOLD + "!")
                .replace("%target%", "$1");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% has recovered from the effects of the " + ChatColor.BOLD + "QuiveringPalm" + ChatColor.BOLD + "!")
                .replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        // Display use Message
        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        double damageMultiplier = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 1.2D, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 8000, false);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        // Play Sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6F, 2.0F);

        // Add the debuff to the target
        CharacterTemplate targCT = this.plugin.getCharacterManager().getCharacter(target);
        QuiveringPalmEffect qpEffect = new QuiveringPalmEffect(this, hero.getPlayer(), duration, damageMultiplier);
        targCT.addEffect(qpEffect);

        //player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spawnParticle(Particle.CRIT_MAGIC, target.getLocation().add(0, 0.5, 0), 25, 0, 0, 0, 1);

        return SkillResult.NORMAL;
    }

    public class QuiveringPalmListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (!(event.getDamager() instanceof Hero) || !(event.getEntity() instanceof LivingEntity))
                return;

            // Ensure that the target is a living entity
            Entity targEnt = event.getEntity();
            if (!(targEnt instanceof LivingEntity))
                return;

            // Check to make sure that the target has the quivering palm effect
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter((LivingEntity) targEnt);
            if (!targetCT.hasEffect(effectName))
                return;

            // Alter the damage being dealt to the target
            QuiveringPalmEffect qpEffect = (QuiveringPalmEffect) targetCT.getEffect(effectName);
            double damageMultiplier = qpEffect.getDamageModifier();

            double damage = event.getDamage() * damageMultiplier;
            event.setDamage(damage);
        }
    }

    // Effect required for implementing an internal cooldown on rune application
    public class QuiveringPalmEffect extends ExpirableEffect {
        private final double damageMultiplier;

        public QuiveringPalmEffect(Skill skill, Player applier, long duration, double damageMultipler) {
            super(skill, effectName, applier, duration, applyText, expireText);

            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.PHYSICAL);

            this.damageMultiplier = damageMultipler;

            addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (int) ((duration + 4000) / 50), 3), false);
        }

        public double getDamageModifier() {
            return damageMultiplier;
        }
    }
}
