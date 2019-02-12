package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SkillHeroesCall extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillHeroesCall(Heroes plugin) {
        super(plugin, "HeroesCall");
        setDescription("You loose a dreadful howl, imposing your beastly presence in a $1 block radius. "
                + "All those who hear the call will have their inner hero awoken. "
                + "The Ender Beast MUST be slain. The calling lasts for $2 seconds. "
                + "Not very effective if you are in your human form.");
        setArgumentRange(0, 0);
        setUsage("/skill heroescall");
        setIdentifiers("skill heroescall");
        setTypes(SkillType.DEBUFFING, SkillType.AREA_OF_EFFECT, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 8, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", radius + "").replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.RADIUS.node(), 8);
        node.set("transform-period", 500);
        node.set(SkillSetting.PERIOD.node(), 4000);
        node.set(SkillSetting.DURATION.node(), 4000);
        node.set("maximum-effective-distance", 40);
        node.set("pull-power-reduction", 6.0);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is filled with a Hero's call and wishes to slay %hero%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer filled with heroic purpose!");
        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this,
                SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% is filled with a Hero's call and wishes to slay %hero%!")
                .replace("%target%", "$1")
                .replace("%hero%", "$2");

        expireText = SkillConfigManager.getRaw(this,
                SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% is no longer filled with heroic purpose!")
                .replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 8, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);

        int period = hero.hasEffect("Transformed")
                ? SkillConfigManager.getUseSetting(hero, this, "transform-period", 500, false)
                : SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 4000, false);

        int maxEffectiveDistance = SkillConfigManager.getUseSetting(hero, this, "maximum-effective-distance", 40, false);
        double pullPowerReduction = SkillConfigManager.getUseSetting(hero, this, "pull-power-reduction", 6.0, false);

        broadcastExecuteText(hero);


        List<CharacterTemplate> actualCallTargets = new ArrayList<CharacterTemplate>();
        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            LivingEntity target = (LivingEntity) entity;
            if (!damageCheck(player, target))
                continue;

            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            target.getWorld().spawnParticle(Particle.SQUID_INK, target.getLocation().add(0, 1, 0), 5, 1.0F, 0.5F, 1.0F, 0);
            if (targetCT.hasEffect("HeroicPurpose"))
                targetCT.removeEffect(hero.getEffect("HeroicPurpose"));

            HeroicPurposeEffect callEffect = new HeroicPurposeEffect(this, player, period, duration, maxEffectiveDistance, pullPowerReduction);
            targetCT.addEffect(callEffect);
            actualCallTargets.add(targetCT);
        }

        hero.addEffect(new HeroicCallingEffect(this, player, duration + 1000, actualCallTargets));

        //player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 0.5F, 0.8F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 0.5f);

        return SkillResult.NORMAL;
    }

    public class HeroicCallingEffect extends ExpirableEffect {

        private final List<CharacterTemplate> callTargets;

        public HeroicCallingEffect(Skill skill, Player applier, long duration, List<CharacterTemplate> callTargets) {
            super(skill, "HeroicCalling", applier, duration);
            this.callTargets = callTargets;
        }

        @Override
        public void removeFromHero(Hero hero) {
            for (CharacterTemplate target : callTargets) {
                if (target == null) // Does this happen if one of the players logs out? I have no idea tbh.
                    continue;
                if (target.hasEffect("HeroicPurpose"))
                    target.removeEffect(target.getEffect("HeroicPurpose"));
            }
        }
    }

    public class HeroicPurposeEffect extends PeriodicExpirableEffect {
        private final int maxEffectiveDistance;
        private final double pullPowerReduction;

        HeroicPurposeEffect(Skill skill, Player applier, long period, long duration, int maxEffectiveDistance, double pullPowerReduction) {
            super(skill, "HeroicPurpose", applier, period, duration, applyText, expireText);
            this.maxEffectiveDistance = maxEffectiveDistance;

            this.pullPowerReduction = pullPowerReduction;

            types.add(EffectType.HARMFUL);
            types.add(EffectType.PHYSICAL);
            types.add(EffectType.TAUNT);

            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (duration / 1000 * 20), 1));
        }

        @Override
        public void tickHero(Hero hero) {
            Player victim = hero.getPlayer();
            Hero currentlyCallingHero = plugin.getCharacterManager().getHero((Player) applier);
            if (currentlyCallingHero == null || !currentlyCallingHero.hasEffect("HeroicCalling") || victim.getLocation().distance(applier.getLocation()) > maxEffectiveDistance)
                hero.removeEffect(this);

            Location targetLocation = applier.getLocation();
            faceTarget(victim, targetLocation);
            pullToTarget(victim, targetLocation);
        }

        @Override
        public void tickMonster(Monster monster) {
            LivingEntity victim = monster.getEntity();
            Hero currentlyCallingHero = plugin.getCharacterManager().getHero((Player) applier);
            if (currentlyCallingHero == null || !currentlyCallingHero.hasEffect("HeroicCalling") || victim.getLocation().distance(applier.getLocation()) > maxEffectiveDistance)
                monster.removeEffect(this);

            Location targetLocation = applier.getLocation();
            faceTarget(victim, targetLocation);
            pullToTarget(victim, targetLocation);

            org.bukkit.entity.Monster bukkitMonster = (org.bukkit.entity.Monster) victim;
            bukkitMonster.setTarget(applier);
        }

        private void faceTarget(LivingEntity victim, Location callerLocation) {

            Location victimLocation = victim.getLocation();
            Vector targetDir = victimLocation.toVector().subtract(callerLocation.toVector());
            float angleToCaller = targetDir.angle(victimLocation.getDirection());

            victim.sendMessage("DEBUG: FOV: " + angleToCaller);
            if (angleToCaller >= -60 && angleToCaller <= 60) {
                // Already facing target. Don't mess with their camera.
                return;
            }

            Vector oldVelocity = victim.getVelocity().clone();
            Vector dir = callerLocation.clone().subtract(victim.getEyeLocation()).toVector();
            Location loc = victim.getLocation().setDirection(dir);
            victim.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
            victim.setVelocity(oldVelocity);
        }

//        protected boolean isInFieldOfVision(LivingEntity e1, LivingEntity e2){
//
//            float yawPrime = e2.getLocation().getYaw();
//            float pitchPrime = e2.getLocation().getPitch();
//
//            e2.faceEntity(e1, 360F, 360F);
//
//            //switch values of prime rotation variables with current rotation variables
//            float f = e2.getLocation().getYaw();
//            float f2 = e2.getLocation().getPitch();
//            e2.getLocation().setYaw(yawPrime);
//            e2.getLocation().setPitch(pitchPrime);
//            yawPrime = f;
//            pitchPrime = f1;
//            //assuming field of vision consists of everything within X degrees from getLocation().getYaw() and Y degrees from getLocation().getPitch(), check if entity 2's current getLocation().getYaw() and getLocation().getPitch() within this X and Y range
//            float X = 60F; //this is only a guess, I don't know the actual range
//            float Y = 45F; //this is only a guess, I don't know the actual range
//            float yawFOVMin = e2.getLocation().getYaw() - X >= 0F ? e2.getLocation().getYaw() - X : 360F + e2.getLocation().getYaw() - X;
//            float yawFOVMax = e2.getLocation().getYaw() + X < 360F ? e2.getLocation().getYaw() + X : -360F + e2.getLocation().getYaw() + X;
//            //NOTE: I dont recall the range of getLocation().getPitch(); 0 to 180?
//            float pitchFOVMin = e2.getLocation().getPitch() - Y >= 0F ? e2.getLocation().getPitch() - Y : 180F + e2.getLocation().getPitch() - Y;
//            float pitchFOVMax = e2.getLocation().getPitch() + Y < 180F ? e2.getLocation().getPitch() + Y : -180F + e2.getLocation().getPitch() + Y;
//            if(yawPrime >= yawFOVMin && yawPrime <= yawFOVMax && pitchPrime >= pitchFOVMin && pitchPrime <= pitchFOVMax && e2.canEntityBeSean(e1))
//                return true;
//            else return false;
//        }

        private void pullToTarget(LivingEntity victim, Location callerLocation)
        {
            Location victimLocation = victim.getLocation();
            if (callerLocation.distance(victimLocation) > 75)
                return;

            double xDir = (callerLocation.getX() - victimLocation.getX()) / pullPowerReduction;
            double zDir = (callerLocation.getZ() - victimLocation.getZ()) / pullPowerReduction;
            final Vector v = new Vector(xDir, 0, zDir);
            victim.setVelocity(victim.getVelocity().add(v));
        }
    }
}
