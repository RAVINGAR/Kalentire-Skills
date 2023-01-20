package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.libs.slikey.effectlib.Effect;
import com.herocraftonline.heroes.libs.slikey.effectlib.util.RandomUtils;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class SkillArrowStorm extends ActiveSkill {

    private final Map<Arrow, Long> stormArrows = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 4632858378318784263L;

        @Override
        protected boolean removeEldestEntry(final Entry<Arrow, Long> eldest) {
            return (size() > 7000 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    private String applyText;
    private String expireText;

    public SkillArrowStorm(final Heroes plugin) {
        super(plugin, "ArrowStorm");
        setDescription("Summon a powerful storm of arrows at your target location. Arrows storm down from the sky, dealing $1 damage and slowing any targets hit for $2 seconds.");
        setUsage("/skill ArrowStorm");
        setArgumentRange(0, 0);
        setIdentifiers("skill ArrowStorm");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AREA_OF_EFFECT, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);

    }

    @Override
    public String getDescription(final Hero hero) {
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, "slow-duration", 2000, false);

        final String formattedDuration = Util.decFormat.format(duration / 1000.0);
        final String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set(SkillSetting.DAMAGE.node(), 15);
        node.set("max-storm-height", 10);
        node.set("downward-velocity", 1.0);
        node.set("velocity-deviation", 0.0);
        node.set("delay-between-firing", 0.1);
        node.set("storm-arrows-launched", 100);
        node.set("slow duration", 1000);
        node.set("slow-multiplier", 1);
        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% has been slowed by %hero%'s ArrowStorm!").replace("%target%", "$1").replace("$target$", "$1").replace("%hero%", "$2").replace("$hero$", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% is no longer slowed!").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);

        final int numstormArrows = SkillConfigManager.getUseSetting(hero, this, "storm-arrows-launched", 12, false);


        final double delayBetween = SkillConfigManager.getUseSetting(hero, this, "delay-between-firing", 0.2, false);
        final double velocityDeviation = SkillConfigManager.getUseSetting(hero, this, "velocity-deviation", 0.2, false);
        final double yVelocity = SkillConfigManager.getUseSetting(hero, this, "downward-velocity", 0.5, false);

        final int stormHeight = SkillConfigManager.getUseSetting(hero, this, "max-storm-height", 10, false);

        final int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);

        final Block tBlock = player.getTargetBlock((HashSet<Material>) null, maxDist);
        // Block tBlock = player.getTargetBlock(null, maxDist);
        if (tBlock == null) {
            return SkillResult.INVALID_TARGET;
        }

        broadcastExecuteText(hero);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.2F, 1.0F);

        // Create a cicle of stormArrow launch locations, based on skill radius.
        final Location stormCenter = tBlock.getLocation().add(new Vector(.5, stormHeight + 0.5d, .5));
        final List<Location> possibleLaunchLocations = Util.getCircleLocationList(stormCenter, radius, 1, true, true, 0);
        final int numPossibleLaunchLocations = possibleLaunchLocations.size();

        Collections.shuffle(possibleLaunchLocations);

        final long time = System.currentTimeMillis();
        final Random ranGen = new Random((int) ((time / 2.0) * 12));

        // Play the firework effects in a sequence
        final World world = tBlock.getLocation().getWorld();
        int k = 0;
        for (int i = 0; i < numstormArrows; i++) {
            if (k >= numPossibleLaunchLocations) {
                Collections.shuffle(possibleLaunchLocations);
                k = 0;
            }

            final Location fLoc = possibleLaunchLocations.get(k);
            k++;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {

                final double randomX = ranGen.nextGaussian() * velocityDeviation;
                final double randomZ = ranGen.nextGaussian() * velocityDeviation;

                final Vector vel = new Vector(randomX, -yVelocity, randomZ);

                final Arrow stormArrow = world.spawn(fLoc, Arrow.class);
                //stormArrow.getWorld().spigot().playEffect(stormArrow.getLocation(), Effect.EXPLOSION_LARGE, 0, 0, 0.4F, 0.4F, 0.4F, 0.0F, 2, 32);
                //stormArrow.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, stormArrow.getLocation(), 2, 0.4, 0.4, 0.4, 0);

                cloudEffect(stormArrow.getLocation());
                stormArrow.setShooter(player);
                stormArrow.setVelocity(vel);
                stormArrows.put(stormArrow, System.currentTimeMillis());

            }, (long) ((delayBetween * i) * 20));
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_BURN, 0.5F, 1.0F);
        return SkillResult.NORMAL;
    }

    public void cloudEffect(final Location location) {
        //Cloud Effect
        final Effect visualEffect = new Effect(effectLib) {
            public final Particle cloudParticle = Particle.CLOUD;
            public final Color cloudColor = Color.WHITE;
            public final Particle mainParticle = Particle.REDSTONE;
            public final Color mainParticleColor = Color.GREEN;
            public final float cloudSize = .7f;
            public final float particleRadius = cloudSize - .1f;
            public final double yOffset = .8;

            @Override
            public void onRun() {
                final Location location = getLocation();
                location.add(0, yOffset, 0);
                for (int i = 0; i < 50; i++) {
                    final Vector v = RandomUtils.getRandomCircleVector().multiply(RandomUtils.random.nextDouble() * cloudSize);
                    display(cloudParticle, location.add(v), cloudColor, 0, 7);
                    location.subtract(v);
                }
                final Location l = location.add(0, .2, 0);
                for (int i = 0; i < 15; i++) {
                    final int r = RandomUtils.random.nextInt(2);
                    final double x = RandomUtils.random.nextDouble() * particleRadius;
                    final double z = RandomUtils.random.nextDouble() * particleRadius;
                    l.add(x, 0, z);
                    if (r != 1) {
                        display(mainParticle, l, mainParticleColor);
                    }
                    l.subtract(x, 0, z);
                    l.subtract(x, 0, z);
                    if (r != 1) {
                        display(mainParticle, l, mainParticleColor);
                    }
                    l.add(x, 0, z);
                }
            }
        };

        visualEffect.type = com.herocraftonline.heroes.libs.slikey.effectlib.EffectType.INSTANT;
//        visualEffect.period = 5;
//        visualEffect.iterations = 50;
        visualEffect.asynchronous = true;
        visualEffect.setLocation(location);

        visualEffect.start();
    }


    public class SkillEntityListener implements Listener {

        private final Skill skill;

        public SkillEntityListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler()
        public void onEntityDamage(final EntityDamageEvent event) {
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            final EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            final Entity projectile = subEvent.getDamager();
            if (!(projectile instanceof Arrow) || !stormArrows.containsKey(projectile)) {
                return;
            }

            event.setDamage(0.0);
            event.setCancelled(true);
            stormArrows.remove(projectile);

            final ProjectileSource source = ((Projectile) subEvent.getDamager()).getShooter();
            if (!(source instanceof LivingEntity)) {
                return;
            }
            final Entity dmger = (LivingEntity) source;
            if (dmger instanceof Player) {
                final Hero hero = plugin.getCharacterManager().getHero((Player) dmger);

                if (!damageCheck((Player) dmger, (LivingEntity) event.getEntity())) {
                    return;
                }

                final LivingEntity target = (LivingEntity) event.getEntity();
                final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                // Check if entity is immune to further firewave hits
                if (targetCT.hasEffect("ArrowStormAntiMultiEffect")) {
                    event.setCancelled(true);
                    return;
                }

                double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 50, false);
                final double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
                damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

                final long duration = SkillConfigManager.getUseSetting(hero, skill, "slow-duration", 2000, false);
                final int amplifier = SkillConfigManager.getUseSetting(hero, skill, "slow-multiplier", 1, false);

                final SlowEffect arrowSlowEffect = new SlowEffect(skill, (Player) dmger, duration, amplifier, applyText, expireText);
                arrowSlowEffect.types.add(EffectType.DISPELLABLE);
                arrowSlowEffect.types.add(EffectType.AREA_OF_EFFECT);

                targetCT.addEffect(arrowSlowEffect);
                targetCT.addEffect(new ExpirableEffect(skill, "ArrowStormAntiMultiEffect", (Player) dmger, 500));

                //addSpellTarget((LivingEntity) event.getEntity(), hero);
                addSpellTarget(event.getEntity(), hero);
                damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);
                event.setCancelled(true);
            }
        }
    }
}






