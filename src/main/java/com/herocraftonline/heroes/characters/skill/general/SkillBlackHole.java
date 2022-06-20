package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.BlindEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.physics.RayCastFlag;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;

public class SkillBlackHole extends ActiveSkill {

    public SkillBlackHole(Heroes plugin) {
        super(plugin, "BlackHole");
        setDescription("Summon a black hole on the target location that dislocates and blinds enemies in it for $1 second(s).");
        setUsage("/skill blackhole");
        setArgumentRange(0, 0);
        setIdentifiers("skill blackhole");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.BLINDING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 100, false);
        duration += durationIncrease * hero.getAttributeValue(AttributeType.CHARISMA);
        duration /= 1000; // Divide by 1000 to get seconds

        return getDescription().replace("$1", duration + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 100);

        node.set("blindTargets", true);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        Player player = hero.getPlayer();
        World world = player.getWorld();

        Vector start = player.getEyeLocation().toVector();
        Vector end = player.getEyeLocation().getDirection().multiply(25).add(start);
        RayCastHit hit;

        hit = NMSHandler.getInterface().getNMSPhysics().rayCastBlocks(world, start, end, RayCastFlag.BLOCK_IGNORE_NON_SOLID);

        if(hit == null) {
            player.sendMessage("Invalid location for Black Hole!");
            return SkillResult.CANCELLED;
        }

        Location loc = new Location(world, hit.getBlockX(), hit.getBlockY() + 1, hit.getBlockZ());

        // Get configs
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 100, false);
        duration += durationIncrease * hero.getAttributeValue(AttributeType.CHARISMA);
        duration /= 50; // Divide by 50 to get tick duration, which EffectLib uses

        boolean blind = SkillConfigManager.getUseSetting(hero, this, "blindTargets", true);

        broadcastExecuteText(hero);

        // Initialize the Effect Manager
        EffectManager em = new EffectManager(plugin);

        // Initialize and configure the Effect
        BlackHoleEffect bhe = new BlackHoleEffect(em, this);
        bhe.setLocation(loc);
        bhe.asynchronous = true;

        bhe.iterations = duration;

        bhe.affectPlayers = true;
        bhe.casterPlayer = player;
        bhe.blindTargets = blind;


        // Start the Effect
        bhe.start();
        em.disposeOnTermination();

        return SkillResult.NORMAL;
    }

    private class BlackHoleEffect extends Effect {

        // Effect settings
        public Particle smokeParticle = Particle.SMOKE_LARGE;

        public int strands = 6;
        public int particles = 25;
        public float radius = 5;
        public float curve = 10;
        public double rotation = Math.PI / 4;

        // Skill settings
        private Skill skill;

        public boolean affectPlayers;
        public Player casterPlayer;
        public boolean blindTargets;

        public BlackHoleEffect(EffectManager manager) {
            this(manager, null);
        }

        public BlackHoleEffect(EffectManager manager, Skill parentSkill) {
            super(manager);
            type = EffectType.REPEATING;
            period = 1;
            iterations = 40; // = 2 sec.

            skill = parentSkill;

            affectPlayers = false;
            casterPlayer = null;
        }

        @Override
        public void onRun() {
            final Location loc = getLocation();

            for (int i = 1; i <= strands; i++) {
                for (int j = 1; j <= particles; j++) {
                    float ratio = (float) j / particles;
                    double angle = curve * ratio * 2 * Math.PI / strands + (2 * Math.PI * i / strands) + rotation;
                    double x = Math.cos(angle) * ratio * radius;
                    double z = Math.sin(angle) * ratio * radius;
                    loc.add(x, 0, z);
                    display(smokeParticle, loc);
                    loc.subtract(x, 0, z);
                }
            }

            if (affectPlayers && casterPlayer != null) {
                final Collection<Entity> nearbyEntities = loc.getWorld().getNearbyEntities(loc, 5, 3, 5);

                if(!nearbyEntities.isEmpty())
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            for (Entity ent : nearbyEntities) {
                                if (!(ent instanceof LivingEntity))
                                    continue;

                                LivingEntity lEnt = (LivingEntity) ent;

                                if (!damageCheck(casterPlayer, lEnt))
                                    continue;

                                Vector vector = loc.toVector().subtract(ent.getLocation().toVector());
                                ent.setVelocity(vector);
                                // If fall damage is an issue, fix it here later
                                if (blindTargets && skill != null && ent instanceof Player) {
                                    Hero targetHero = plugin.getCharacterManager().getHero((Player) ent);

                                    if (!targetHero.hasEffect("BlackHoleBlindEffect"))
                                        targetHero.addEffect(new BlindEffect(skill, "BlackHoleBlindEffect", casterPlayer, (iterations * period) * 50)); // iterations * period returns an amount of ticks, * 50 to get millis
                                }
                            }
                        }
                    });
            }

        }
    }
}
