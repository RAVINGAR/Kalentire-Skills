package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.BlindEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.libs.slikey.effectlib.Effect;
import com.herocraftonline.heroes.libs.slikey.effectlib.EffectManager;
import com.herocraftonline.heroes.libs.slikey.effectlib.EffectType;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;

public class SkillBlackHole extends ActiveSkill {

    public SkillBlackHole(final Heroes plugin) {
        super(plugin, "BlackHole");
        setDescription("Summon a black hole on the target location that dislocates and blinds enemies in it for $1 second(s).");
        setUsage("/skill blackhole");
        setArgumentRange(0, 0);
        setIdentifiers("skill blackhole");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.BLINDING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(final Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        final int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 100, false);
        duration += durationIncrease * hero.getAttributeValue(AttributeType.CHARISMA);
        duration /= 1000; // Divide by 1000 to get seconds

        return getDescription().replace("$1", duration + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 100);
        node.set(SkillSetting.PERIOD.node(), 500);

        return node;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] strings) {
        final Player player = hero.getPlayer();
        final World world = player.getWorld();

        final Vector start = player.getEyeLocation().toVector();
        final Vector end = player.getEyeLocation().getDirection().multiply(25).add(start);
        final RayCastHit hit = NMSHandler.getInterface().getNMSPhysics().rayCastBlocks(world, start, end, (block) -> true);
        if (hit == null) {
            player.sendMessage(ChatColor.GRAY + "You cannot summon a black hole there!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        final Location loc = new Location(world, hit.getBlockX(), hit.getBlockY() + 1, hit.getBlockZ());
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 500, false);
        final int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, 5000, false);

        // Initialize and configure the Effect
        final BlackHoleEffect blackhole = new BlackHoleEffect(effectLib, player, this, period, duration);
        blackhole.setLocation(loc);
        blackhole.start();

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    private class BlackHoleEffect extends Effect {
        private final int strands = 6;
        private final int particles = 25;
        private final float radius = 5;
        private final float curve = 10;
        private final double rotation = Math.PI / 4;

        private final int originalPeriod;

        // Skill settings
        private final Skill skill;

        private final Player casterPlayer;

        /**
         * @param manager
         * @param attacker
         * @param parentSkill
         * @param period      The period in milliseconds
         * @param duration    The duration in milliseconds
         */

        public BlackHoleEffect(final EffectManager manager, final Player attacker, final Skill parentSkill, final int period, final int duration) {
            super(manager);
            type = EffectType.REPEATING;
            this.casterPlayer = attacker;
            this.originalPeriod = period;
            this.period = period / 50;
            this.iterations = (duration / 50) / this.period;
            skill = parentSkill;
        }

        @Override
        public void onRun() {
            final Location loc = getLocation();

            for (int i = 1; i <= strands; i++) {
                for (int j = 1; j <= particles; j++) {
                    final float ratio = (float) j / particles;
                    final double angle = curve * ratio * 2 * Math.PI / strands + (2 * Math.PI * i / strands) + rotation;
                    final double x = Math.cos(angle) * ratio * radius;
                    final double z = Math.sin(angle) * ratio * radius;
                    loc.add(x, 0, z);
                    display(Particle.SMOKE_LARGE, loc);
                    display(Particle.SPELL_WITCH, loc);
                    loc.subtract(x, 0, z);
                }
            }

            final Collection<Entity> nearbyEntities = loc.getWorld().getNearbyEntities(loc, radius, radius, radius);

            if (!nearbyEntities.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (final Entity ent : nearbyEntities) {
                        if (!(ent instanceof LivingEntity)) {
                            continue;
                        }

                        final LivingEntity lEnt = (LivingEntity) ent;

                        if (!damageCheck(casterPlayer, lEnt)) {
                            continue;
                        }

                        final Vector vector = loc.toVector().subtract(ent.getLocation().toVector());
                        ent.setVelocity(vector);
                        if (ent instanceof Player) {
                            final Hero targetHero = plugin.getCharacterManager().getHero((Player) ent);

                            if (!targetHero.hasEffect("BlackHoleBlindEffect")) {
                                targetHero.addEffect(new BlindEffect(skill, "BlackHoleBlindEffect", casterPlayer, originalPeriod * 3L));
                            }
                        }
                    }
                });
            }
        }
    }
}
