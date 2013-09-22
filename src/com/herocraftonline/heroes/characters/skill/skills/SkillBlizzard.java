package com.herocraftonline.heroes.characters.skill.skills;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

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
        setTypes(SkillType.ABILITY_PROPERTY_ICE, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.AREA_OF_EFFECT, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        long duration = SkillConfigManager.getUseSetting(hero, this, "slow-duration", Integer.valueOf(2000), false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(12));
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.2));
        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(5));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(15));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.5));
        node.set("max-storm-height", Integer.valueOf(4));
        node.set("velocity-deviation", Double.valueOf(0.5));
        node.set("delay-between-firing", Double.valueOf(0.1));
        node.set("icebolts-launched", Integer.valueOf(4));
        node.set("icebolts-launched-per-intellect", Double.valueOf(0.5));
        node.set("slow duration", Integer.valueOf(1000));
        node.set("slow-multiplier", Integer.valueOf(1));
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

        int stormHeight = SkillConfigManager.getUseSetting(hero, this, "max-storm-height", 10, false);

        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);
        double maxDistIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT, 0.2, false);
        maxDist += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * maxDistIncrease);

        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);

        int numIceBolts = SkillConfigManager.getUseSetting(hero, this, "icebolts-launched", 12, false);
        double numIceBoltsIncrease = SkillConfigManager.getUseSetting(hero, this, "icebolts-launched-per-intellect", 0.325, false);
        numIceBolts += (int) (numIceBoltsIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        double delayBetween = SkillConfigManager.getUseSetting(hero, this, "delay-between-firing", 0.2, false);
        final double velocityDeviation = SkillConfigManager.getUseSetting(hero, this, "velocity-deviation", 0.2, false);
        final double yVelocity = SkillConfigManager.getUseSetting(hero, this, "velocity-deviation", 0.5, false);

        Block tBlock = player.getTargetBlock(null, maxDist);

        broadcastExecuteText(hero);

        // Create a cicle of icebolt launch locations, based on skill radius.
        List<Location> possibleLaunchLocations = Util.getCircleLocationList(tBlock.getLocation().add(new Vector(.5, .5, .5)), radius, 1, true, true, stormHeight);
        int numPossibleLaunchLocations = possibleLaunchLocations.size();

        //        long ticksPerIceBolt = (int) (100 / numPossibleLaunchLocations);

        Collections.shuffle(possibleLaunchLocations);

        long time = System.currentTimeMillis();
        final Random ranGen = new Random((int) ((time / 2.0) * 12));

        // Play the firework effects in a sequence
        final World world = tBlock.getLocation().getWorld();
        int k = 0;
        for (int i = 0; i < numIceBolts; i++) {
            if (k > numPossibleLaunchLocations) {
                k = 0;
                Collections.shuffle(possibleLaunchLocations);
            }

            final Location fLoc = possibleLaunchLocations.get(k);
            k++;

            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    Util.playClientEffect(player, fLoc, "hugeexplosion", new Vector(0, 0, 0), 1F, 10, true);
                    world.playSound(fLoc, Sound.AMBIENCE_THUNDER, 1.1F, 1.0F);

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
            blizzardIceBolts.remove(projectile);

            Entity dmger = ((Snowball) subEvent.getDamager()).getShooter();
            if (dmger instanceof Player) {
                Hero hero = plugin.getCharacterManager().getHero((Player) dmger);

                if (!damageCheck((Player) dmger, (LivingEntity) event.getEntity())) {
                    event.setCancelled(true);
                    return;
                }

                event.getEntity().setFireTicks(0);

                double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, Integer.valueOf(50), false);
                double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(1.0), false);
                damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

                long duration = SkillConfigManager.getUseSetting(hero, skill, "slow-duration", Integer.valueOf(2000), false);
                int amplifier = SkillConfigManager.getUseSetting(hero, skill, "slow-multiplier", 1, false);

                SlowEffect iceSlowEffect = new SlowEffect(skill, (Player) dmger, duration, amplifier, applyText, expireText);
                iceSlowEffect.types.add(EffectType.DISPELLABLE);
                iceSlowEffect.types.add(EffectType.ICE);
                LivingEntity target = (LivingEntity) event.getEntity();
                plugin.getCharacterManager().getCharacter(target).addEffect(iceSlowEffect);

                addSpellTarget((LivingEntity) event.getEntity(), hero);
                damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);

                event.setCancelled(true);
            }
        }
    }
}