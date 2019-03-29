package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.HelixEffect;
import de.slikey.effectlib.effect.SphereEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SkillGravityFlux extends ActiveSkill {

    private final NMSPhysics physics = NMSPhysics.instance();
//    private String applyText;
//    private String expireText;

    public SkillGravityFlux(Heroes plugin) {
        super(plugin, "GravityFlux");
        setDescription("Warp the space in a $1 block radius around the target location, reversing gravity for all of those that are nearby."
                + "Lasts for $2 seconds. Affects both allies and enemies. Use with caution!");
        setArgumentRange(0, 0);
        setUsage("/skill gravityflux");
        setIdentifiers("skill gravityflux");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.RADIUS.node(), 5.0);
        config.set(SkillSetting.DURATION.node(), 3000);
        config.set("levitation-amplifier", 0);
//        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has had their gravity inversed by %hero%!");
//        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s gravity returns to normal.");
        return config;
    }

//    @Override
//    public void init() {
//        super.init();
//
//        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% has had their gravity inversed by %hero%!").replace("%hero%", "$1");
//        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s gravity returns to normal.").replace("%hero%", "$1");
//    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);
        Block targettedBlock = getBlockViaRaycast(player, maxDist, false);
        if (targettedBlock == null)
            return SkillResult.INVALID_TARGET_NO_MSG;

        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5.0, false);
        int amplifier = SkillConfigManager.getUseSetting(hero, this, "levitation-amplifier", 0, false);

        Location targetLoc = targettedBlock.getLocation();
        Collection<Entity> nearbyEnts = targetLoc.getWorld().getNearbyEntities(targetLoc, radius, radius, radius);
        for (Entity ent : nearbyEnts) {
            if (!(ent instanceof LivingEntity))
                continue;

            CharacterTemplate ctTarget = plugin.getCharacterManager().getCharacter((LivingEntity) ent);
            if (ctTarget == null)
                continue;

            ctTarget.addEffect(new HaultGravityEffect(this, player, duration, amplifier));
        }

        return SkillResult.NORMAL;
    }

    private Block getBlockViaRaycast(Player player, int maxDist, boolean allowTransparent) {
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vector normal = eyeLocation.getDirection();
        Vector start = eyeLocation.toVector();
        Vector end = normal.clone().multiply(maxDist).add(start);
        RayCastHit hit = physics.rayCast(world, player, start, end);

        Block targetBlock;
        if (hit == null) {
            targetBlock = world.getBlockAt(end.getBlockX(), end.getBlockY(), end.getBlockZ());
        } else {
            targetBlock = hit.isEntity()
                    ? hit.getEntity().getLocation().getBlock()
                    : hit.getBlock(world);
        }

        if (targetBlock == null) {
            return null;
        } else if (!allowTransparent && targetBlock.getType() == Material.AIR) {
            return null;
        }
        return targetBlock;
    }

    public class HaultGravityEffect extends PeriodicExpirableEffect {

        HaultGravityEffect(Skill skill, Player applier, long duration, int amplifier) {
            super(skill, "HaultedGravity", applier, 1500, duration, null, null);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);

            addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, (int) (duration / 50), amplifier));
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            applyVisuals(hero.getPlayer());
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            applyVisuals(monster.getEntity());
        }

        @Override
        public void tickMonster(Monster monster) {
            LivingEntity ent = monster.getEntity();
            ent.getWorld().playSound(ent.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.4F, 2F);
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.4F, 2F);
        }

        private void applyVisuals(LivingEntity target) {
            final World world = target.getWorld();
            final Location loc = target.getLocation();
            final int durationTicks = (int) this.getDuration() / 50;

            EffectManager em = new EffectManager(plugin);
            HelixEffect visualEffect = new HelixEffect(em);

            DynamicLocation dynamicLoc = new DynamicLocation(target);
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.offset = new Vector(0, -target.getEyeHeight(), 0);
            visualEffect.disappearWithOriginEntity = true;

            visualEffect.radius = 1.5F;
            visualEffect.iterations = durationTicks / visualEffect.period;

            visualEffect.color = Color.PURPLE;
            visualEffect.particle = Particle.REDSTONE;
            visualEffect.particleCount = 3;

            em.start(visualEffect);
            em.disposeOnTermination();
        }
    }
}
