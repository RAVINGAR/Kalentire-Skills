package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.integrations.citizens.CitizensHero;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

public class SkillHeroicThrow extends TargettedSkill {

    private boolean ncpEnabled = false;

    public SkillHeroicThrow(final Heroes plugin) {
        super(plugin, "HeroicThrow");
        setDescription("Grab your target and chuck them in the opposite direction! Distance thrown increases per level.");
        setUsage("/skill HeroicThrow");
        setArgumentRange(0, 0);
        setIdentifiers("skill heroicthrow");
        setTypes(SkillType.FORCE, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.INTERRUPTING, SkillType.SILENCEABLE);
        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
    }

    @Override
    public String getDescription(final Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set("horizontal-power", 0.5);
        node.set("horizontal-power-increase", 0.025);
        node.set("vertical-power", 0.5);
        node.set("vertical-power-increase", 0.0175);
        node.set("ncp-exemption-duration", 1500);
        node.set("safefall-duration", 1500);
        node.set("toss-delay", 0.2);
        node.set(SkillSetting.DAMAGE.node(), 5);
        return node;
    }

    @SuppressWarnings("deprecation")
    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final Player player = hero.getPlayer();
        final int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 5, false);
        if (target.equals(player)) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        if (targetCT instanceof CitizensHero) {
            return SkillResult.INVALID_TARGET;
        }

        broadcastExecuteText(hero, target);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        // Let's bypass the nocheat issues...
        if (ncpEnabled) {
            if (target instanceof Player) {
                final Player targetPlayer = (Player) target;
                if (!targetPlayer.isOp()) {
                    final long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1500, false);
                    if (duration > 0) {
                        final NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, targetPlayer, duration);
                        targetCT.addEffect(ncpExemptEffect);
                    }
                }
            }
        }

        final Location originalLoc = player.getLocation();
        final Location flippedLoc = new Location(originalLoc.getWorld(), originalLoc.getX(), originalLoc.getY(), originalLoc.getZ(), (originalLoc.getYaw() < 180 ? originalLoc.getYaw() - 180 : originalLoc.getYaw() + 180), originalLoc.getPitch());
        player.teleport(flippedLoc);

        final Location playerLoc = player.getLocation();
        final Location targetLoc = target.getLocation();

        final Material mat = targetLoc.getBlock().getRelative(BlockFace.DOWN).getType();

        boolean weakenVelocity = false;
        switch (mat) {
            case LAVA:
            case SOUL_SAND:
            case WATER:
                weakenVelocity = true;
                break;
            default:
                break;
        }
        final long sfDuration = SkillConfigManager.getUseSetting(hero, this, "safefall-duration", 1500, false);

        targetCT.addEffect(new SafeFallEffect(this, player, sfDuration));


        double tempVPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.25, false);
        final double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase", 0.0075, false);
        tempVPower += (vPowerIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        if (weakenVelocity) {
            tempVPower *= 0.75;
        }

        final double vPower = tempVPower;

        final Vector pushUpVector = new Vector(0, vPower, 0);
        target.setVelocity(pushUpVector);

        final double xDir = playerLoc.getX() - targetLoc.getX();
        final double zDir = playerLoc.getZ() - targetLoc.getZ();

        double tempHPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 1.5, false);
        final double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase", 0.0375, false);
        tempHPower += (hPowerIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        if (weakenVelocity) {
            tempHPower *= 0.75;
        }

        final double hPower = tempHPower;

        // Push them "up" first. THEN toss them.
        final double delay = SkillConfigManager.getUseSetting(hero, this, "toss-delay", 0.2, false);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            final Vector pushVector = new Vector(xDir, 0, zDir).normalize().multiply(hPower).setY(vPower);
            target.setVelocity(pushVector);
        }, (long) (delay * 20));

        // Play sound

        return SkillResult.NORMAL;
    }

    public static class SafeFallEffect
            extends ExpirableEffect {


        public SafeFallEffect(final Skill skill, final String name, final Player applier, final long duration) {
            super(skill, name, applier, duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.SAFEFALL);
            this.types.add(EffectType.MAGIC);
        }

        public SafeFallEffect(final Skill skill, final Player applier, final long duration) {
            this(skill, "Safefall", applier, duration);
        }

        @Override
        public void apply(final CharacterTemplate cT) {
            super.apply(cT);

        }

        @Override
        public void remove(final CharacterTemplate cT) {
            super.remove(cT);

        }
    }


    private static class NCPExemptionEffect extends ExpirableEffect {

        public NCPExemptionEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, "NCPExemptionEffect_MOVING", applier, duration);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.MOVING);
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.MOVING);
        }
    }
}