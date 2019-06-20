package com.herocraftonline.heroes.characters.skill.reborn.disciple.rework;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.CharacterRegainHealthEvent;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillHarmoniousHand extends TargettedSkill {

    private static final String effectName = "HarmoniousHands";
    private String applyText;
    private String expireText;

    public SkillHarmoniousHand(Heroes plugin) {
        super(plugin, "HarmoniousHand");
        setDescription("Link with your target, forming a pact of healing with them for $1 seconds. Once activated, your physical damage will be reduced by $2%. " +
                "However, for every point of dealt physical damage that is reduced by this ability, you will heal your linked target for that amount. " +
                "Your linked target must be within $3 blocks to receive healing, and has a maximum heal value of $4.");
        setUsage("/skill harmonioushand");
        setIdentifiers("skill harmonioushand");
        setArgumentRange(0, 0);
        setTypes(SkillType.BUFFING, SkillType.HEALING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_PHYSICAL);

        Bukkit.getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, 12000, false);
        double damageReduction = SkillConfigManager.getUseSetting(hero, this, "damage-reduction-percent", 0.75, false);
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 16.0, false);
        double maxHeal = SkillConfigManager.getUseSetting(hero, this, "maximum-healing-allowed", 16.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000.0))
                .replace("$2", Util.decFormat.format(damageReduction * 100.0))
                .replace("$3", Util.decFormat.format(radius))
                .replace("$4", Util.decFormat.format(maxHeal));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 10.0);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has formed a Harmonious Bond with %target%!");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s Harmonious Bond has faded.");
        config.set(SkillSetting.DURATION.node(), 12000);
        config.set("damage-reduction-percent", 0.75);
        config.set(SkillSetting.RADIUS.node(), 16.0);
        config.set("maximum-healing-allowed", 350.0);
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this,
                SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has formed a Harmonious Bond with %target%!")
                .replace("%hero%", "$1")
                .replace("%target%", "$2");

        expireText = SkillConfigManager.getRaw(this,
                SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s Harmonious Bond has faded.")
                .replace("%hero%", "$1")
                .replace("%target%", "$2");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] strings) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);

        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, 12000, false);
        hero.addEffect(new HarmoniousHandCasterEffect(this, player, duration, targetCT));
        targetCT.addEffect(new HarmoniousHandTargetEffect(this, player, duration));

        return SkillResult.NORMAL;
    }

    public class HarmoniousHandTargetEffect extends ExpirableEffect {

        public HarmoniousHandTargetEffect(Skill skill, Player applier, long duration) {
            super(skill, getTargetEffectName(applier), applier, duration + 250); // Give it a little bit extra duration

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.HEALING);
        }
    }

    @NotNull
    private static String getTargetEffectName(Player applier) {
        return applier.getName() + "|HarmoniousHandTarget";
    }

    public class HarmoniousHandCasterEffect extends ExpirableEffect {
        private final CharacterTemplate target;
        private double radius;
        private double radiusSquared;
        private double damageReduction;
        private double maxHealingAllowed;
        private double totalHealingPerformed;

        HarmoniousHandCasterEffect(Skill skill, Player applier, int duration, CharacterTemplate target) {
            super(skill, effectName, applier, duration, applyText, expireText);

            this.target = target;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            this.damageReduction = SkillConfigManager.getUseSetting(hero, skill, "damage-reduction-percent", 0.75, false);
            if (this.damageReduction > 0.99)
                this.damageReduction = 0.99;
            if (this.damageReduction < 0.01)
                this.damageReduction = 0.01;

            this.maxHealingAllowed = SkillConfigManager.getUseSetting(hero, skill, "maximum-healing-allowed", 16.0, false);
            this.radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 12.0, false);
            this.radiusSquared = radius * radius;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            String targetEffectName = getTargetEffectName(applier);
            if (target.hasEffect(targetEffectName)) {
                target.removeEffect(target.getEffect(targetEffectName));
            }
        }

        public double getRadiusSquared() {
            return radiusSquared;
        }

        public double getDamageReduction() {
            return damageReduction;
        }

        public CharacterTemplate getTarget() {
            return target;
        }

        public void addToTotalHealing(Hero hero, double healing) {
            this.totalHealingPerformed += healing;
            if (this.totalHealingPerformed >= maxHealingAllowed) {
                removeFromHero(hero);
            }
        }

        public double getMaxHealingAllowed() {
            return this.maxHealingAllowed;
        }

        public double getTotalHealingPerformed() {
            return this.totalHealingPerformed;
        }
    }

    private class SkillHeroListener implements Listener {
        private Skill skill;

        SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPhysicalDamage(EntityDamageByEntityEvent event) {
            if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof LivingEntity) || event.getCause() != DamageCause.ENTITY_ATTACK) {
                return;
            }

            Player player = (Player) event.getDamager();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.hasEffect(effectName))
                return;

            HarmoniousHandCasterEffect handEffect = (HarmoniousHandCasterEffect) hero.getEffect(effectName);

            CharacterTemplate healTarget = handEffect.getTarget();
            if (!healTarget.hasEffect(getTargetEffectName(player))) {
                hero.removeEffect(handEffect);
                return;
            }

            if (!player.getWorld().equals(healTarget.getEntity().getWorld()))
                return;

            double newDamage = event.getDamage() * handEffect.damageReduction;
            double healing = event.getDamage() - newDamage;

            double radiusSquared = handEffect.getRadiusSquared();
            if (healTarget.getEntity().getLocation().distanceSquared(player.getLocation()) > radiusSquared)
                return;

            if (handEffect.getTotalHealingPerformed() + healing > handEffect.getMaxHealingAllowed()) {
                healing = handEffect.getMaxHealingAllowed() - handEffect.getTotalHealingPerformed();
            }

            // Manually call the heal event instead of using healTarget.tryHeal() because we need to track the amount of healing that is actually done.
            CharacterRegainHealthEvent healEvent = healTarget instanceof Hero
                    ? new HeroRegainHealthEvent(hero, healing, skill)
                    : new CharacterRegainHealthEvent(hero, healing, skill);
            plugin.getServer().getPluginManager().callEvent(healEvent);

            if (!healEvent.isCancelled() && healEvent.getDelta() > 0.0D) {
                healTarget.heal(healEvent.getDelta());

                // Store the healing
                handEffect.addToTotalHealing(hero, healEvent.getDelta());
            } else {
                hero.getEntity().sendMessage("    " + ChatComponents.GENERIC_SKILL + "Your target had their healing prevented!");
            }
        }
    }
}