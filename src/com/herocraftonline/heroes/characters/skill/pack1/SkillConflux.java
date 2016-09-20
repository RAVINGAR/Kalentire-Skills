package com.herocraftonline.heroes.characters.skill.pack1;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SkillConflux extends ActiveSkill {

    private static final Random random = new Random();

    private String applyText;
    private String expireText;

    public SkillConflux(Heroes plugin) {
        super(plugin, "Conflux");
        setDescription("You conflux of insanity in the area, confusing targets for $1 seconds.");
        setUsage("/skill conflux");
        setArgumentRange(0, 0);
        setIdentifiers("skill conflux");
        setTypes(SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 125, false);
        duration += durationIncrease * hero.getAttributeValue(AttributeType.CHARISMA);

        String formattedDuration = Util.decFormat.format((double) duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.PERIOD.node(), 1000);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 125);
        node.set("max-drift", 2.1);
        node.set("ncp-exemption-duration", 500);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% is confluxd!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% has regained his wit!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% is confluxd!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% has regained his wit!").replace("%target%", "$1");
    }

    public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius)
    {
        World world = centerPoint.getWorld();

        double increment = (2 * Math.PI) / particleAmount;

        ArrayList<Location> locations = new ArrayList<Location>();

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
    public SkillResult use(Hero hero, String[] args) {


        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 125, false);
        duration += durationIncrease * hero.getAttributeValue(AttributeType.CHARISMA);

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, true);

        float maxDrift = (float) SkillConfigManager.getUseSetting(hero, this, "max-drift", 2.1, false);


        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_EXPERIENCE_ORB_PICKUP.value(), 0.8F, 1.0F);
        //double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60, false);
        //double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        //damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        int maxTargets = SkillConfigManager.getUseSetting(hero, this, "max-targets", 0, false);
        int targetsHit = 0;
        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            // Check to see if we've exceeded the max targets
            if (maxTargets > 0 && targetsHit >= maxTargets) {
                break;
            }

            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            LivingEntity target = (LivingEntity) entity;
            if (!damageCheck(player, target))
                continue;

            addSpellTarget(target, hero);
            //damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK);

            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            ConfuseEffect confluxEffect = new ConfuseEffect(this, player, duration, period, maxDrift);
            confluxEffect.types.add(EffectType.DISPELLABLE);
            targetCT.addEffect(confluxEffect);

            targetsHit++;
        }

        for (double r = 1; r < radius * 2; r++)
        {
            ArrayList<Location> particleLocations = circle(player.getLocation(), 45, r / 2);
            for (int i = 0; i < particleLocations.size(); i++)
            {
                player.getWorld().spigot().playEffect(particleLocations.get(i), Effect.MAGIC_CRIT, 0, 0, 0, 0.1F, 0, 0.1F, 1, 16);
            }
        }

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class ConfuseEffect extends PeriodicExpirableEffect {

        private final float maxDrift;

        public ConfuseEffect(Skill skill, Player applier, long duration, long period, float maxDrift) {
            super(skill, "Conflux", applier, period, duration);

            types.add(EffectType.HARMFUL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);

            this.maxDrift = maxDrift;

            addMobEffect(9, (int) (duration / 1000) * 20, 127, false);
        }

        public void adjustVelocity(LivingEntity lEntity) {
            Vector velocity = lEntity.getVelocity();

            double angle = random.nextDouble() * 2 * Math.PI;
            float xAdjustment = (float) (maxDrift * Math.cos(angle));
            float zAdjustment = (float) (maxDrift * Math.sin(angle));

            Material belowMat = lEntity.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
            switch (belowMat) {
                case STATIONARY_WATER:
                case STATIONARY_LAVA:
                case WATER:
                case LAVA:
                case SOUL_SAND:
                    xAdjustment *= 0.75;
                    zAdjustment *= 0.75;
                    break;
                default:
                    break;
            }

            velocity.setX(xAdjustment);
            velocity.setZ(zAdjustment);
            lEntity.setVelocity(velocity);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, Messaging.getLivingEntityName(monster));
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

        @Override
        public void tickMonster(Monster monster) {
            adjustVelocity(monster.getEntity());
            if (monster instanceof Creature) {
                ((Creature) monster).setTarget(null);
            }
        }

        @Override
        public void tickHero(final Hero hero) {

            // Let's bypass the nocheat issues...
            NCPUtils.applyExemptions(hero.getPlayer(), new NCPFunction() {

                @Override
                public void execute()
                {
                    adjustVelocity(hero.getPlayer());
                }
            }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, skill, "ncp-exemption-duration", 500, false));
        }
    }
}
