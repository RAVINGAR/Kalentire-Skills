package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.List;

//TODO change to passive
public class SkillHolyAura extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillHolyAura(final Heroes plugin) {
        super(plugin, "HolyAura");
        setUsage("/skill holyaura");
        setArgumentRange(0, 0);
        setDescription("You begin to radiate with a Holy Aura, healing all allies within $1 blocks (other than yourself)" +
                " for $2 health every $3 seconds. Your aura dissipates after $4 seconds. Any undead targets within your Holy Aura will also be dealt $5 damage.");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.HEALING, SkillType.BUFFING);
        setIdentifiers("skill holyaura");
    }

    @Override
    public String getDescription(final Hero hero) {
        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 15, false);

        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 16000, false);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING.node(), 17, false);
        healing = getScaledHealing(hero, healing);
        final double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.15, false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

        double undeadDamage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", 20, false);
        final double undeadDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "undead-damage-increase-per-wisdom", 0.375, false);
        undeadDamage += (hero.getAttributeValue(AttributeType.WISDOM) * undeadDamageIncrease);

        final String formattedHealing = Util.decFormat.format(healing);
        final String formattedPeriod = Util.decFormat.format(period / 1000.0);
        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        final String formattedDamage = Util.decFormat.format(undeadDamage);

        return getDescription().replace("$1", radius + "").replace("$2", formattedHealing).replace("$3", formattedPeriod).replace("$4", formattedDuration).replace("$5", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 16000);
        node.set(SkillSetting.PERIOD.node(), 2000);
        node.set(SkillSetting.HEALING.node(), 17);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.15);
        node.set(SkillSetting.RADIUS.node(), 6);
        node.set("undead-damage", 20);
        node.set("undead-damage-increase-per-wisdom", 0.375);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% begins to radiate a holy aura!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has lost their holy aura!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% begins to radiate a holy aura!").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% has lost their holy aura!").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 16000, false);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 17, false);
        healing = getScaledHealing(hero, healing);
        final double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.15, false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

        double undeadDamage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", 20, false);
        final double undeadDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "undead-damage-increase-per-wisdom", 0.375, false);
        undeadDamage += (hero.getAttributeValue(AttributeType.WISDOM) * undeadDamageIncrease);

        hero.addEffect(new HolyAuraEffect(this, player, duration, period, healing, undeadDamage));

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 7.0F, 16);

        return SkillResult.NORMAL;
    }

    public class HolyAuraEffect extends PeriodicExpirableEffect {

        final double tickHeal;
        final double undeadDamage;

        public HolyAuraEffect(final Skill skill, final Player applier, final long duration, final long period, final double tickHeal, final double undeadDamage) {
            super(skill, "HolyAuraEffect", applier, period, duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HEALING);
            types.add(EffectType.LIGHT);
            types.add(EffectType.AREA_OF_EFFECT);

            this.tickHeal = tickHeal;
            this.undeadDamage = undeadDamage;
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

        @Override
        public void tickHero(final Hero hero) {
            healNerby(hero);
        }

        private void healNerby(final Hero hero) {

            final Player player = hero.getPlayer();

            final int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 6, false);
            final int radiusSquared = radius * radius;

            for (double r = 1; r < radius * 2; r++) {
                final List<Location> particleLocations = GeometryUtil.circle(player.getLocation(), 36, r / 2);
                for (final Location particleLocation : particleLocations) {
                    //player.getWorld().spigot().playEffect(particleLocations.get(i), Effect.FIREWORKS_SPARK, 0, 0, 0, 0.1F, 0, 0.1F, 1, 16);
                    player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, particleLocation, 1, 0, 0.1, 0, 0.1);
                }
            }

            // Check if the hero has a party
            if (hero.hasParty()) {
                final Location playerLocation = player.getLocation();
                // Loop through the player's party members and heal as necessary
                for (final Hero member : hero.getParty().getMembers()) {
                    if (member.equals(hero))        // Skip the player
                    {
                        continue;
                    }

                    final Location memberLocation = member.getPlayer().getLocation();

                    // Ensure the party member is in the same world.
                    if (memberLocation.getWorld().equals(playerLocation.getWorld())) {

                        if (memberLocation.distanceSquared(playerLocation) <= radiusSquared) {
                            // Check to see if they are close enough to the player to receive healing

                            final HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(member, tickHeal, skill, hero);
                            Bukkit.getPluginManager().callEvent(healEvent);
                            if (!healEvent.isCancelled()) {
                                member.heal(healEvent.getDelta());
                            }
                        }
                    }
                }
            }

            // Damage nearby undead
            for (final Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (!(entity instanceof LivingEntity) || (entity instanceof Player)) {
                    continue;
                }

                final LivingEntity lETarget = (LivingEntity) entity;
                if (!(Util.isUndead(plugin, lETarget))) {
                    continue;
                }

                // Damage for 50% of heal value
                addSpellTarget(lETarget, hero);
                skill.damageEntity(lETarget, player, undeadDamage, DamageCause.MAGIC);
            }
        }

        @Override
        public void tickMonster(final Monster monster) {
        }
    }
}
