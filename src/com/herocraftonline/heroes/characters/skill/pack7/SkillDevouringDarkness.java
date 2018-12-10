package com.herocraftonline.heroes.characters.skill.pack7;

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
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import java.util.ArrayList;

public class SkillDevouringDarkness extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillDevouringDarkness(Heroes plugin) {
        super(plugin, "DevouringDarkness");
        setDescription("You resonate darkness, slowing enemies within $1 blocks for $2 seconds. Your devouring darkness pulses every $3 seconds for the next $4 seconds.");
        setUsage("/skill devouringdarkness");
        setArgumentRange(0, 0);
        setIdentifiers("skill devouringdarkness");
        setTypes(SkillType.MOVEMENT_SLOWING, SkillType.ABILITY_PROPERTY_SONG, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
    }

    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, "darkness-buff-duration", 3000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, "darkness-buff-period", 1500, false);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 6, false);
        int slowDuration = SkillConfigManager.getUseSetting(hero, this, "darkness-slow-duration", 1500, false);

        String formattedPeriod = Util.decFormat.format(period / 1000.0);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedSlowDuration = Util.decFormat.format(slowDuration / 1000.0);

        return getDescription().replace("$1", radius + "").replace("$2", formattedSlowDuration).replace("$3", formattedPeriod).replace("$4", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.USE_TEXT.node(), "");
        node.set("darkness-buff-duration", 3000);
        node.set("darkness-buff-period", 1500);
        node.set(SkillSetting.RADIUS.node(), 6);
        node.set("darkness-slow-duration", 1500);
        node.set("slow-amplifier", 0);
        node.set("slow-amplifier-increase-per-intellect", 0.075);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% emits a devouring darkness!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer emitting darkness.");
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

        //player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), Effect.FIREWORKS_SPARK, 3);
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation().add(0, 2.5, 0), 3, 0, 0, 0, 1);
        //player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), Effect.FLYING_GLYPH, 3);
        player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, player.getLocation().add(0, 2.5, 0), 3, 0, 0, 0, 1);
        //player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), Effect.WITCH_MAGIC, 3);
        player.getWorld().spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 2.5, 0), 3, 0, 0, 0, 1);

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
                    //player.getWorld().spigot().playEffect(particleLocation.add(0, 0.1, 0), Effect.PARTICLE_SMOKE, 0, 0, 0, 0.1F, 0, 0.0F, 1, 16);
                    player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, particleLocation.add(0, 0.1, 0), 1, 0, 0.1, 0, 0);
                }
            }

            int intellect = hero.getAttributeValue(AttributeType.INTELLECT);

            int slowAmount = SkillConfigManager.getUseSetting(hero, skill, "slow-amplifier", 1, false);
            double slowAmountIncrease = SkillConfigManager.getUseSetting(hero, skill, "slow-amplifier-increase-per-intellect", 0.075, false);
            slowAmount += Math.floor(slowAmountIncrease * intellect);

            int slowDuration = SkillConfigManager.getUseSetting(hero, skill, "darkness-slow-duration", 1500, false);

            int maxTargets =SkillConfigManager.getUseSetting(hero, skill, "max-targets", 0, false);
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