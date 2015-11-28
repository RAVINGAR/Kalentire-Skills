package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.ArrayList;

public class SkillDevouringDarkness extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillDevouringDarkness(Heroes plugin) {
        super(plugin, "DevouringDarkness");
        setDescription("You resonate darkness, pulsing for $1 damage and slowing enemies within $2 blocks for $3 seconds. Your devouring darkness pulses every $4 seconds for the next $5 seconds.");
        setUsage("/skill devouringdarkness");
        setArgumentRange(0, 0);
        setIdentifiers("skill devouringdarkness");
        setTypes(SkillType.MOVEMENT_SLOWING, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_SONG, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
    }

    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, "darkness-buff-duration", 3000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, "darkness-buff-period", 1500, false);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 6, false);
        int slowDuration = SkillConfigManager.getUseSetting(hero, this, "darkness-slow-duration", 1500, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 17, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.125, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.CHARISMA));

        String formattedPeriod = Util.decFormat.format(period / 1000.0);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedSlowDuration = Util.decFormat.format(slowDuration / 1000.0);
        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage).replace("$2", radius + "").replace("$3", formattedSlowDuration).replace("$4", formattedPeriod).replace("$5", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.USE_TEXT.node(), "");
        node.set("darkness-buff-duration", 3000);
        node.set("darkness-buff-period", 1500);
        node.set(SkillSetting.RADIUS.node(), 6);
        node.set(SkillSetting.DAMAGE.node(), 17);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.125);
        node.set("darkness-slow-duration", 1500);
        node.set("slow-amplifier", 0);
        node.set("slow-amplifier-increase-per-intellect", 0.075);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% emits a devouring darkness!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is no longer emitting darkness.");
        node.set(SkillSetting.DELAY.node(), 1000);
        node.set(SkillSetting.COOLDOWN.node(), 1000);
        node.set("max-targets", 5);


        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {

        Player player = hero.getPlayer();
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, "darkness-buff-duration", 3000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, "darkness-buff-period", 1500, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 6, false);

        hero.addEffect(new DevouringDarknessEffect(this, hero.getPlayer(), period, duration, radius));

        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), Effect.FIREWORKS_SPARK, 3);
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), Effect.FLYING_GLYPH, 3);
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), Effect.WITCH_MAGIC, 3);

        return SkillResult.NORMAL;
    }

    public class DevouringDarknessEffect extends PeriodicExpirableEffect {

        private final int radius;

        public DevouringDarknessEffect(SkillDevouringDarkness skill, Player applier, int period, int duration, int radius) {
            super(skill, "DevouringDarkness", applier, period, duration, applyText, expireText);

            types.add(EffectType.BENEFICIAL);

            this.radius = radius;
        }

        public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius)
        {
            World world = centerPoint.getWorld();

            double increment = (2 * Math.PI) / particleAmount;

            ArrayList<Location> locations = new ArrayList<>();

            for (int i = 0; i < particleAmount; i++)
            {
                double angle = i * increment;
                double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
                double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
                locations.add(new Location(world, x, centerPoint.getY(), z));
            }
            return locations;
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();

            for (double r = 1; r < radius; r++)
            {
                ArrayList<Location> particleLocations = circle(player.getLocation(), 36, r);
                for (Location particleLocation : particleLocations) {
                    player.getWorld().spigot().playEffect(particleLocation.add(0, 0.1, 0), Effect.PARTICLE_SMOKE, 0, 0, 0, 0.1F, 0, 0.0F, 1, 16);
                }
            }

            int intellect = hero.getAttributeValue(AttributeType.INTELLECT);

            int slowAmount = SkillConfigManager.getUseSetting(hero, skill, "slow-amplifier", 1, false);
            double slowAmountIncrease = SkillConfigManager.getUseSetting(hero, skill, "slow-amplifier-increase-per-intellect", 0.075, false);
            slowAmount += Math.floor(slowAmountIncrease * intellect);

            int slowDuration = SkillConfigManager.getUseSetting(hero, skill, "darkness-slow-duration", 1500, false);

            double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 17, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.125, false);
            damage += damageIncrease * intellect;

            // An example of not limiting if damage is 0. Since this isn't the case on live, it makes for a good example.
            int maxTargets = damage > 0 ? SkillConfigManager.getUseSetting(hero, skill, "max-targets", 0, false) : 0;
            int targetsHit = 0;
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                // Check to see if we've exceeded the max targets
                if (maxTargets > 0 && targetsHit >= maxTargets) {
                    break;
                }

                if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity)) {
                    continue;
                }

                CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter((LivingEntity) entity);

                if (damage > 0) {
                    addSpellTarget(entity, hero);
                    damageEntity((LivingEntity) entity, player, damage, DamageCause.MAGIC, false);
                }

                SlowEffect sEffect = new SlowEffect(skill, player, slowDuration, slowAmount, null, null);
                sEffect.types.add(EffectType.DISPELLABLE);
                targetCT.addEffect(sEffect);

                targetsHit++;
            }
        }

        @Override
        public void tickMonster(Monster monster) {}
    }
}