package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillCripple extends TargettedSkill {
    private String applyText;
    private String removeText;

    public SkillCripple(final Heroes plugin) {
        super(plugin, "Cripple");
        setDescription("Deals pure damage of $2 and $3 every tick to the enemy for $1s (damage stacks with buffs).");
        setUsage("/skill cripple [target]");
        setArgumentRange(0, 1);
        setIdentifiers("skill cripple");
        setTypes(SkillType.DEBUFFING, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_PHYSICAL);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double baseDuration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false);
        final double durationIncreaseFactor = SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0.0D, false);
        long duration = (long) ((baseDuration + durationIncreaseFactor * hero.getHeroLevel(this)) / 1000L);
        duration = duration > 0L ? duration : 0L;

        final int baseDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 5, false);
        final double damageIncreaseFactor = SkillConfigManager.getUseSetting(hero, this, "damage-increase", 0.0D, false);
        int damage = (int) (baseDamage + damageIncreaseFactor * hero.getHeroLevel(this));
        damage = damage > 0 ? damage : 0;

        final int baseDamageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK.node(), 2, false);
        final double damageTickIncreaseFactor = SkillConfigManager.getUseSetting(hero, this, "tick-damage-increase", 0.0D, false);
        int tickDamage = (int) (baseDamageTick + damageTickIncreaseFactor * hero.getHeroLevel(this));
        tickDamage = tickDamage > 0 ? tickDamage : 0;

        final int baseMaxDistance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE.node(), 2, false);
        final double maxDistanceIncreaseFactor = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE.node(), 0.0D, false);
        int maxDistance = (int) (baseMaxDistance + maxDistanceIncreaseFactor * hero.getHeroLevel(this));
        maxDistance = maxDistance > 0 ? maxDistance : 0;

        String description = getDescription().replace("$1", duration + "")
                .replace("$2", damage + "").replace("$3", tickDamage + "")
                .replace("$4", maxDistance + "");


        final int baseCooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 0, false);
        final int cooldownReductionFactor = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 0, false);
        final int cooldown = (baseCooldown - cooldownReductionFactor * hero.getHeroLevel(this)) / 1000;
        if (cooldown > 0) {
            description = description + " CD:" + cooldown + "s";
        }

        final int baseManaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA.node(), 10, false);
        final int manaReductionFactor = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE.node(), 0, false);
        final int mana = baseManaCost - manaReductionFactor * hero.getHeroLevel(this);
        if (mana > 0) {
            description = description + " M:" + mana;
        }

        final int baseHealthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false);
        final int healthCostReductionFactor = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST_REDUCE, mana, true);
        final int healthCost = baseHealthCost - healthCostReductionFactor * hero.getHeroLevel(this);
        if (healthCost > 0) {
            description = description + " HP:" + healthCost;
        }

        final int baseStaminaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA.node(), 0, false);
        final int staminaCostReduction = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA_REDUCE.node(), 0, false);
        final int staminaCost = baseStaminaCost - staminaCostReduction * hero.getHeroLevel(this);
        if (staminaCost > 0) {
            description = description + " FP:" + staminaCost;
        }

        final int delay = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
            description = description + " W:" + delay + "s";
        }

        final int exp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXP.node(), 0, false);
        if (exp > 0) {
            description = description + " XP:" + exp;
        }
        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set("duration-increase", 0);
        node.set(SkillSetting.PERIOD.node(), 1000);
        node.set(SkillSetting.DAMAGE_TICK.node(), 2);
        node.set("tick-damage-increase", 0);
        node.set(SkillSetting.DAMAGE.node(), 5);
        node.set("damage-incrase", 0);
        node.set(SkillSetting.MAX_DISTANCE.node(), 15);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE.node(), 0);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% has been Crippled by %hero%!");
        node.set("remove-text", "%target% has recovered from %hero%s Crippling blow!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getUseSetting(null, this, SkillSetting.APPLY_TEXT.node(),
                        "%target% has been Crippled by %hero%!")
                .replace("%target%", "$1").replace("$target$", "$1").replace("%hero%", "$2").replace("$hero$", "$2");
        removeText = SkillConfigManager.getUseSetting(null, this, "remove-text",
                        "%target% has recovered from %hero%s Crippling blow!")
                .replace("%target%", "$1").replace("$target$", "$1").replace("%hero%", "$2").replace("$hero$", "$2");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] strings) {
        final Player player = hero.getPlayer();
        if (!target.equals(player) && target instanceof Player) {
            final Hero tHero = plugin.getCharacterManager().getHero((Player) target);
            if ((hero.getParty() == null || !hero.getParty().getMembers().contains(tHero))
                    && damageCheck(player, (LivingEntity) tHero.getPlayer())) {
                broadcastExecuteText(hero, target);
                final double baseDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 5.0, false);
                final double damageIncreaseFactor = SkillConfigManager.getUseSetting(hero, this, "damage-increase", 0.0D, false);
                double damage = baseDamage + damageIncreaseFactor * hero.getHeroLevel(this);

                damage = damage > 0 ? damage : 0;
                damageEntity(tHero.getPlayer(), player, damage, DamageCause.ENTITY_ATTACK);

                final int baseDuration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false);
                final double durationIncreaseFactor = SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0.0D, false);
                long duration = baseDuration + (long) (durationIncreaseFactor * hero.getHeroLevel(this));
                duration = duration > 0L ? duration : 0L;

                final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD.node(), 1000, false);

                final double baseDamageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK.node(), 2.0, false);
                final double damageTickIncreaseFactor = SkillConfigManager.getUseSetting(hero, this, "tick-damage-increase", 0.0D, false);
                double tickDamage = baseDamageTick + damageTickIncreaseFactor * hero.getHeroLevel(this);
                tickDamage = tickDamage > 0 ? tickDamage : 0;

                final CrippleEffect cEffect = new CrippleEffect(this, player, period, duration, tickDamage);
                tHero.addEffect(cEffect);
                return SkillResult.NORMAL;
            }
        }

        return SkillResult.INVALID_TARGET;
    }

    public class CrippleEffect extends PeriodicExpirableEffect {
        private final Player caster;
        private final double damageTick;
        private Location prevLocation;

        public CrippleEffect(final Skill skill, final Player caster, final long period, final long duration, final double damageTick) {
            super(skill, "Cripple", caster, period, duration);
            this.caster = caster;
            this.damageTick = damageTick;
            types.add(EffectType.BLEED);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.PHYSICAL);
        }

        @Override
        public void tickHero(final Hero hero) {
            if ((prevLocation != null)
                    && (Math.abs(hero.getPlayer().getLocation().getX() - prevLocation.getX()) >= 1.0D)
                    && (Math.abs(hero.getPlayer().getLocation().getZ() - prevLocation.getZ()) >= 1.0D)) {
                damageEntity(hero.getPlayer(), caster, damageTick, DamageCause.ENTITY_ATTACK, false);
            }

            prevLocation = hero.getPlayer().getLocation();
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            broadcast(hero.getPlayer().getLocation(), applyText, hero.getPlayer().getDisplayName(), caster.getDisplayName());
            prevLocation = hero.getPlayer().getLocation();
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            broadcast(hero.getPlayer().getLocation(), removeText, hero.getPlayer().getDisplayName(), caster.getDisplayName());
        }

        @Override
        public void tickMonster(final com.herocraftonline.heroes.characters.Monster monster) {
            super.tick(monster);
            damageEntity(monster.getEntity(), caster, damageTick, DamageCause.ENTITY_ATTACK, false);
        }
    }
}
