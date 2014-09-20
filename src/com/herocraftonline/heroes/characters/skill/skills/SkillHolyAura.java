package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

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
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillHolyAura extends ActiveSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    private String applyText;
    private String expireText;

    public SkillHolyAura(Heroes plugin) {
        super(plugin, "HolyAura");
        setDescription("You begin to radiate with a Holy Aura, healing all allies within $1 blocks (other than yourself) for $2 health every $3 seconds. Your aura dissipates after $4 seconds. Any undead targets within your Holy Aura will also be dealt $5 damage.");
        setUsage("/skill holyaura");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.HEALING, SkillType.BUFFING);
        setIdentifiers("skill holyaura");
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 15, false);

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 16000, false);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING.node(), 17, false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.15, false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

        double undeadDamage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", 20, false);
        double undeadDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "undead-damage-increase-per-wisdom", 0.375, false);
        undeadDamage += (hero.getAttributeValue(AttributeType.WISDOM) * undeadDamageIncrease);

        String formattedHealing = Util.decFormat.format(healing);
        String formattedPeriod = Util.decFormat.format(period / 1000.0);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        String formattedDamage = Util.decFormat.format(undeadDamage);

        return getDescription().replace("$1", radius + "").replace("$2", formattedHealing).replace("$3", formattedPeriod).replace("$4", formattedDuration).replace("$5", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 16000);
        node.set(SkillSetting.PERIOD.node(), 2000);
        node.set(SkillSetting.HEALING.node(), 17);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.15);
        node.set(SkillSetting.RADIUS.node(), 6);
        node.set("undead-damage", 20);
        node.set("undead-damage-increase-per-wisdom", 0.375);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% begins to radiate a holy aura!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% has lost their holy aura!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%target% begins to radiate a holy aura!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%target% has lost their holy aura!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 16000, false);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 17, false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.15, false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

        double undeadDamage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", 20, false);
        double undeadDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "undead-damage-increase-per-wisdom", 0.375, false);
        undeadDamage += (hero.getAttributeValue(AttributeType.WISDOM) * undeadDamageIncrease);

        hero.addEffect(new HolyAuraEffect(this, player, duration, period, healing, undeadDamage));

        try {
            fplayer.playFirework(player.getWorld(), player.getLocation().add(0, 1.5, 0), FireworkEffect.builder().flicker(false)
                    .trail(false).with(FireworkEffect.Type.BALL).withColor(Color.YELLOW).withFade(Color.SILVER).build());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
    }

    public class HolyAuraEffect extends PeriodicExpirableEffect {

        double tickHeal;
        double undeadDamage;

        public HolyAuraEffect(Skill skill, Player applier, long duration, long period, double tickHeal, double undeadDamage) {
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
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

        @Override
        public void tickHero(Hero hero) {
            healNerby(hero);
        }

        private void healNerby(Hero hero) {

            Player player = hero.getPlayer();

            int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 6, false);
            int radiusSquared = radius * radius;

            // Check if the hero has a party
            if (hero.hasParty()) {
                Location playerLocation = player.getLocation();
                // Loop through the player's party members and heal as necessary
                for (Hero member : hero.getParty().getMembers()) {
                    if (member.equals(hero))        // Skip the player
                        continue;

                    Location memberLocation = member.getPlayer().getLocation();

                    // Ensure the party member is in the same world.
                    if (memberLocation.getWorld().equals(playerLocation.getWorld())) {

                        if (memberLocation.distanceSquared(playerLocation) <= radiusSquared) {
                            // Check to see if they are close enough to the player to receive healing

                            HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(member, tickHeal, skill, hero);
                            Bukkit.getPluginManager().callEvent(healEvent);
                            if (!healEvent.isCancelled()) {
                                member.heal(healEvent.getAmount());
                            }
                        }
                    }
                }
            }

            // Damage nearby undead
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (!(entity instanceof LivingEntity) || (entity instanceof Player)) {
                    continue;
                }

                LivingEntity lETarget = (LivingEntity) entity;
                if (!(Util.isUndead(plugin, lETarget)))
                    continue;

                // Damage for 50% of heal value
                addSpellTarget(lETarget, hero);
                Skill.damageEntity(lETarget, player, undeadDamage, DamageCause.MAGIC);
            }
        }

        @Override
        public void tickMonster(Monster monster) {}
    }
}
