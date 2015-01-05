package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.Map.Entry;

public class SkillBlizzard extends ActiveSkill {

    private Map<Snowball, Long> blizzardIceBolts = new LinkedHashMap<Snowball, Long>(100) {
        private static final long serialVersionUID = 4632858378318784263L;

        @Override
        protected boolean removeEldestEntry(Entry<Snowball, Long> eldest) {
            return (size() > 7000 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    private String applyText;
    private String expireText;

    public SkillBlizzard(Heroes plugin) {
        super(plugin, "Blizzard");
        setDescription("Summon a powerful Blizzard at your target location. The blizzard rains down several ice bolts at the target location, each dealing $1 damage and slowing any targets hit for $2 seconds.");
        setUsage("/skill blizzard");
        setArgumentRange(0, 0);
        setIdentifiers("skill blizzard");
        setTypes(SkillType.ABILITY_PROPERTY_ICE, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AREA_OF_EFFECT, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        long duration = SkillConfigManager.getUseSetting(hero, this, "slow-duration", 2000, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.2);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set(SkillSetting.DAMAGE.node(), 15);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.5);
        node.set("max-storm-height", 4);
        node.set("downward-velocity", 0.8);
        node.set("velocity-deviation", 0.5);
        node.set("delay-between-firing", 0.1);
        node.set("icebolts-launched", 4);
        node.set("icebolts-launched-per-intellect", 0.5);
        node.set("slow duration", 1000);
        node.set("slow-multiplier", 1);
        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%target% has been slowed by %hero%'s Blizzard!").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%target% is no longer slowed!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);

        int numIceBolts = SkillConfigManager.getUseSetting(hero, this, "icebolts-launched", 12, false);
        double numIceBoltsIncrease = SkillConfigManager.getUseSetting(hero, this, "icebolts-launched-per-intellect", 0.325, false);
        numIceBolts += (int) (numIceBoltsIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        double delayBetween = SkillConfigManager.getUseSetting(hero, this, "delay-between-firing", 0.2, false);
        final double velocityDeviation = SkillConfigManager.getUseSetting(hero, this, "velocity-deviation", 0.2, false);
        final double yVelocity = SkillConfigManager.getUseSetting(hero, this, "downward-velocity", 0.5, false);

        int stormHeight = SkillConfigManager.getUseSetting(hero, this, "max-storm-height", 10, false);

        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);
        double maxDistIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT, 0.2, false);
        maxDist += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * maxDistIncrease);

        Block tBlock = player.getTargetBlock(null, maxDist);
     // Block tBlock = player.getTargetBlock(null, maxDist);
        if (tBlock == null)
            return SkillResult.INVALID_TARGET;

        broadcastExecuteText(hero);
        player.getWorld().playSound(player.getLocation(), Sound.AMBIENCE_THUNDER, 0.2F, 1.0F);

        // Create a cicle of icebolt launch locations, based on skill radius.
        List<Location> possibleLaunchLocations = Util.getCircleLocationList(tBlock.getLocation().add(new Vector(.5, .5, .5)), radius, 1, true, true, stormHeight);
        int numPossibleLaunchLocations = possibleLaunchLocations.size();

        Collections.shuffle(possibleLaunchLocations);

        long time = System.currentTimeMillis();
        final Random ranGen = new Random((int) ((time / 2.0) * 12));

        // Play the firework effects in a sequence
        final World world = tBlock.getLocation().getWorld();
        int k = 0;
        for (int i = 0; i < numIceBolts; i++) {
            if (k >= numPossibleLaunchLocations) {
                Collections.shuffle(possibleLaunchLocations);
                k = 0;
            }

            final Location fLoc = possibleLaunchLocations.get(k);
            k++;

            final int j = i;
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    if (j % 8 == 0) {
                        Util.playClientEffect(player, fLoc, "fire", new Vector(0, 0, 0), 1F, 10, true);
                        world.playSound(fLoc, Sound.AMBIENCE_THUNDER, 1.1F, 1.0F);
                    }

                    double randomX = ranGen.nextGaussian() * velocityDeviation;
                    double randomZ = ranGen.nextGaussian() * velocityDeviation;

                    Vector vel = new Vector(randomX, -yVelocity, randomZ);

                    Snowball iceBolt = world.spawn(fLoc, Snowball.class);
                    iceBolt.setShooter(player);
                    iceBolt.setVelocity(vel);
                    blizzardIceBolts.put(iceBolt, System.currentTimeMillis());
                }
            }, (long) ((delayBetween * i) * 20));
        }
        player.getWorld().playSound(player.getLocation(), Sound.FIZZ, 0.5F, 1.0F);
        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler()
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            Entity projectile = subEvent.getDamager();
            if (!(projectile instanceof Snowball) || !blizzardIceBolts.containsKey(projectile)) {
                return;
            }
            event.setCancelled(true);
            blizzardIceBolts.remove(projectile);

            ProjectileSource source = ((Projectile) subEvent.getDamager()).getShooter();
            if (!(source instanceof LivingEntity))
                return;
            Entity dmger = (LivingEntity) source;
            if (dmger instanceof Player) {
                Hero hero = plugin.getCharacterManager().getHero((Player) dmger);

                if (!damageCheck((Player) dmger, (LivingEntity) event.getEntity()))
                    return;

                LivingEntity target = (LivingEntity) event.getEntity();
                CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                // Check if entity is immune to further firewave hits
                if (targetCT.hasEffect("BlizzardAntiMultiEffect")) {
                    event.setCancelled(true);
                    return;
                }

                event.getEntity().setFireTicks(0);

                double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 50, false);
                double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
                damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

                long duration = SkillConfigManager.getUseSetting(hero, skill, "slow-duration", 2000, false);
                int amplifier = SkillConfigManager.getUseSetting(hero, skill, "slow-multiplier", 1, false);

                SlowEffect iceSlowEffect = new SlowEffect(skill, (Player) dmger, duration, amplifier, applyText, expireText);
                iceSlowEffect.types.add(EffectType.DISPELLABLE);
                iceSlowEffect.types.add(EffectType.ICE);

                targetCT.addEffect(iceSlowEffect);
                targetCT.addEffect(new ExpirableEffect(skill, "BlizzardAntiMultiEffect", (Player) dmger, 500));

              //addSpellTarget((LivingEntity) event.getEntity(), hero);
                addSpellTarget(event.getEntity(), hero);
                damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);
                event.setCancelled(true);
            }
        }
    }
}
