package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.ManaRegenPercentDecreaseEffect;
import com.herocraftonline.heroes.characters.effects.common.ManaRegenPercentIncreaseEffect;
import com.herocraftonline.heroes.characters.effects.common.StaminaRegenPercentDecreaseEffect;
import com.herocraftonline.heroes.characters.effects.common.StaminaRegenPercentIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.characters.skill.tools.MISSILE_TARGET_TYPE;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.util.RandomUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.logging.Level;

public class SkillTimeBomb extends ActiveSkill {

    private String applyText;
    private String expireText;
    private String badApplyText;
    private String badExpireText;

    public SkillTimeBomb(Heroes plugin) {
        super(plugin, "TimeBomb");
        setDescription("You throw a bomb of rejuvinating time. " +
                "Any ally hit will regain extra $2% mana $3% stamina for $6 seconds" +
                "Any enemy hit will regain less extra $4% mana $5% stamina for $6 seconds" +
                "It will also apply 1 TimeShift on all entities hit. ");
        setUsage("/skill timebomb");
        setIdentifiers("skill timebomb");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_TEMPORAL, SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING, SkillType.SILENCEABLE);
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "Your mana and stamina regeneration has increased!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "Your mana and stamina regeneration has returned back to normal.");

        badApplyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "Your mana and stamina regeneration has decreased!");
        badExpireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "Your mana and stamina regeneration has returned back to normal.");
    }

    @Override
    public String getDescription(Hero hero) {

        double manaPercentIncrease = SkillConfigManager.getUseSetting(hero, this, "mana-percent-increase", 1.25, false);
        double staminaPercentIncrease = SkillConfigManager.getUseSetting(hero, this, "stamina-percent-increase", 1.25, false);
        double manaPercentDecrease = SkillConfigManager.getUseSetting(hero, this, "mana-percent-decrease", 0.75, false);
        double staminaPercentDecrease = SkillConfigManager.getUseSetting(hero, this, "stamina-percent-decrease", 0.75, false);

        double duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        return getDescription()
                .replace("$2", Util.decFormat.format(manaPercentIncrease))
                .replace("$3", Util.decFormat.format(staminaPercentIncrease))
                .replace("$4", Util.decFormat.format(manaPercentDecrease))
                .replace("$5", Util.decFormat.format(staminaPercentDecrease))
                .replace("$6", Util.decFormat.format(duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 45.0);
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set("mana-percent-increase", 1.25);
        config.set("stamina-percent-increase", 1.25);
        config.set("mana-percent-decrease", 0.75);
        config.set("stamina-percent-decrease", 0.75);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        Skill timeShiftSkill = (SkillTimeShift) plugin.getSkillManager().getSkill(SkillTimeShift.skillName);
        if (timeShiftSkill == null) {
            Heroes.log(Level.SEVERE, SkillTimeShift.skillName + " is missing from the server. " + getName() + " will no longer work. "
                    + SkillTimeShift.skillName + "_must_ be available to the class that has " + getName() + ".");
            player.sendMessage("One of the Admins or devs broke this skill. Tell them to read the heroes logs to fix it.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero);

        TimeBombMissile missile = new TimeBombMissile(plugin, this, hero);
        missile.fireMissile();

        return SkillResult.NORMAL;
    }

    class TimeBombMissile extends BasicMissile {
        private double explosionRadius;

        TimeBombMissile(Heroes plugin, Skill skill, Hero hero) {
            super(plugin, skill, hero, MISSILE_TARGET_TYPE.NEUTRAL);

            this.explosionRadius = SkillConfigManager.getUseSetting(hero, skill, "explosion-radius", 4.0, false);
            this.visualEffect = new TimeBombVisualEffect(this.effectManager, getEntityDetectRadius(), 0);
        }

        @Override
        protected void onTick() {
            if (this.getTicksLived() % 2 == 0 && this.visualEffect != null) {
                this.visualEffect.setLocation(this.getLocation());
            }

            if (this.getTicksLived() % 4 == 0) {
                getWorld().playSound(getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.5F, 0.5F);
            }
        }

        @Override
        protected void onBlockHit(Block block, Vector hitPoint, BlockFace hitFace, Vector hitForce) {
            performExplosion();
        }

        @Override
        protected void onValidTargetFound(LivingEntity target, Vector hitOrigin, Vector hitForce) {
            performExplosion();
        }

        private void performExplosion() {
            double manaPercentIncrease = SkillConfigManager.getUseSetting(hero, skill, "mana-percent-increase", 1.25, false);
            double staminaPercentIncrease = SkillConfigManager.getUseSetting(hero, skill, "stamina-percent-increase", 1.25, false);
            double manaPercentDecrease = SkillConfigManager.getUseSetting(hero, skill, "mana-percent-decrease", 0.75, false);
            double staminaPercentDecrease = SkillConfigManager.getUseSetting(hero, skill, "stamina-percent-decrease", 0.75, false);
            double duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 5000, false);

            getWorld().playSound(getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, 0.8F);

            Collection<Entity> nearbyEnts = getWorld().getNearbyEntities(getLocation(), this.explosionRadius, this.explosionRadius, this.explosionRadius);
            for (Entity ent : nearbyEnts) {
                if (!(ent instanceof LivingEntity))
                    continue;

                LivingEntity target = (LivingEntity) ent;
                SkillTimeShift timeShiftSkill = (SkillTimeShift) plugin.getSkillManager().getSkill(SkillTimeShift.skillName);
                CharacterTemplate targetCt = plugin.getCharacterManager().getCharacter(target);
                if (hero.isAlliedTo(target)) {
                    if (hero.getPlayer() != target)
                        timeShiftSkill.use(hero, target, new String[]{"NoBroadcast"});
                    targetCt.addEffect(new ManaIncreaseEffect(skill, player, (long) duration, manaPercentIncrease));
                    targetCt.addEffect(new StaminaIncreaseEffect(skill, player, (long) duration, staminaPercentIncrease));
                } else {
                    if (!damageCheck(player, target))
                        continue;

                    timeShiftSkill.use(hero, target, new String[]{"NoBroadcast"});
                    targetCt.addEffect(new ManaDecreaseEffect(skill, player, (long) duration, manaPercentDecrease));
                    targetCt.addEffect(new StaminaDecreaseEffect(skill, player, (long) duration, staminaPercentDecrease));
                }
            }
        }
    }

    class ManaIncreaseEffect extends ManaRegenPercentIncreaseEffect {
        ManaIncreaseEffect(Skill skill, Player applier, long duration, double delta) {
            super(skill, applier, duration, delta, applyText, expireText);
            types.add(EffectType.TEMPORAL);
            types.add(EffectType.SILENT_ACTIONS);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }
    }

    class ManaDecreaseEffect extends ManaRegenPercentDecreaseEffect {
        public ManaDecreaseEffect(Skill skill, Player applier, long duration, double delta) {
            super(skill, applier, duration, delta, badApplyText, badExpireText);
            types.add(EffectType.TEMPORAL);
            types.add(EffectType.SILENT_ACTIONS);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }
    }

    class StaminaIncreaseEffect extends StaminaRegenPercentIncreaseEffect {
        public StaminaIncreaseEffect(Skill skill, Player applier, long duration, double delta) {
            super(skill, applier, duration, delta, null, null);
            types.add(EffectType.SILENT_ACTIONS);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }
    }

    class StaminaDecreaseEffect extends StaminaRegenPercentDecreaseEffect {
        public StaminaDecreaseEffect(Skill skill, Player applier, long duration, double delta) {
            super(skill, applier, duration, delta, null, null);
            types.add(EffectType.SILENT_ACTIONS);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }
    }

    class TimeBombVisualEffect extends Effect {
        private Particle primaryParticle;
        private Color primaryColor;
        private double primaryRadius;
        private double primaryYOffset;
        private int primaryParticleCount;
        private double primaryRadiusDecrease;

        private Particle secondaryParticle;
        private Color secondaryColor;
        private double secondaryRadius;
        private double secondaryYOffset;
        private int secondaryParticleCount;
        private double secondaryRadiusDecrease;

        TimeBombVisualEffect(EffectManager effectManager, double radius, double decreasePerTick) {
            super(effectManager);

            this.period = 1;
            this.iterations = 500;

            this.primaryParticle = Particle.REDSTONE;
            this.primaryColor = Color.GRAY;
            this.primaryRadius = radius;
            this.primaryRadiusDecrease = decreasePerTick / this.period;
            this.primaryYOffset = 0.0D;
            this.primaryParticleCount = 10;

            this.secondaryParticle = Particle.SPELL_MOB;
            this.secondaryColor = Color.GRAY;
            this.secondaryRadius = secondaryRadiusMultiplier(radius);
            this.secondaryRadiusDecrease = secondaryRadiusMultiplier(decreasePerTick) / this.period;
            this.secondaryYOffset = 0.0D;
            this.secondaryParticleCount = 20;
        }

        public void onRun() {
            if (primaryRadiusDecrease != 0.0D)
                this.primaryRadius -= primaryRadiusDecrease;
            if (primaryRadius > 0)
                displaySphere(this.primaryRadius, this.primaryYOffset, this.primaryParticle, this.primaryColor, this.primaryParticleCount);

            displayCenter();

            if (secondaryRadiusDecrease != 0.0D)
                this.secondaryRadius -= secondaryRadiusDecrease;
            if (secondaryRadius > 0)
                displaySphere(this.secondaryRadius, this.secondaryYOffset, this.secondaryParticle, this.secondaryColor, this.secondaryParticleCount);
        }

        private double secondaryRadiusMultiplier(double radiusValue) {
            return radiusValue * 1.2;
        }

        private void displayCenter() {
            Location location = this.getLocation();
            Vector vector = new Vector(0.0D, primaryYOffset, 0.0D);
            location.add(vector);
            this.display(Particle.SPIT, location);
            location.subtract(vector);
        }

        private void displaySphere(double radiusToUse, double yOffset, Particle particle, Color color, int particleCount) {
            Location location = this.getLocation();
            location.add(0.0D, yOffset, 0.0D);

            for (int i = 0; i < particleCount; ++i) {
                Vector vector = RandomUtils.getRandomVector().multiply(radiusToUse);
                location.add(vector);
                this.display(particle, location, color);
                location.subtract(vector);
            }
        }
    }
}