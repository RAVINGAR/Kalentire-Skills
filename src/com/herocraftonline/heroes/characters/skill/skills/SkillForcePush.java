package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillForcePush extends TargettedSkill {

    private boolean ncpEnabled = false;

    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillForcePush(Heroes plugin) {
        super(plugin, "Forcepush");
        setDescription("Deal $1 physical damage and force your target away from you. The push power is affected by your Intellect.");
        setUsage("/skill forcepush");
        setArgumentRange(0, 0);
        setIdentifiers("skill forcepush");
        setTypes(SkillType.FORCE, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null)
            ncpEnabled = true;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(50), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(1.6), false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(5));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(50));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), Double.valueOf(1.6));
        node.set("horizontal-power", Double.valueOf(1.5));
        node.set("horizontal-power-increase-per-intellect", Double.valueOf(0.0375));
        node.set("vertical-power", Double.valueOf(0.25));
        node.set("vertical-power-increase-per-intellect", Double.valueOf(0.0075));
        node.set("ncp-exemption-duration", 1500);
        node.set("push-delay", Double.valueOf(0.2));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(50), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(1.6), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        if (damage > 0) {
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK, false);
        }

        // Let's bypass the nocheat issues...
        if (ncpEnabled) {
            if (target instanceof Player) {
                Player targetPlayer = (Player) target;
                if (!targetPlayer.isOp()) {
                    long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1500, false);
                    if (duration > 0) {
                        NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, targetPlayer, duration);
                        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                        targetCT.addEffect(ncpExemptEffect);
                    }
                }
            }
        }

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        double tempVPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", Double.valueOf(0.25), false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-intellect", Double.valueOf(0.0075), false);
        tempVPower += (vPowerIncrease * hero.getAttributeValue(AttributeType.INTELLECT));
        final double vPower = tempVPower;

        Vector pushUpVector = new Vector(0, vPower, 0);
        target.setVelocity(pushUpVector);

        final double xDir = targetLoc.getX() - playerLoc.getX();
        final double zDir = targetLoc.getZ() - playerLoc.getZ();

        double tempHPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", Double.valueOf(1.5), false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-intellect", Double.valueOf(0.0375), false);
        tempHPower += (hPowerIncrease * hero.getAttributeValue(AttributeType.INTELLECT));
        final double hPower = tempHPower;

        // Push them "up" first. THEN we can push them away.
        double delay = SkillConfigManager.getUseSetting(hero, this, "push-delay", 0.2, false);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                // Push them away
                //double yDir = player.getVelocity().getY();
                Vector pushVector = new Vector(xDir, 0, zDir).normalize().multiply(hPower).setY(vPower);
                target.setVelocity(pushVector);
            }
        }, (long) (delay * 20));

        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0, 1.5, 0), FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.BALL).withColor(Color.YELLOW).withFade(Color.NAVY).build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }

    private class NCPExemptionEffect extends ExpirableEffect {

        public NCPExemptionEffect(Skill skill, Player applier, long duration) {
            super(skill, "NCPExemptionEffect_MOVING", applier, duration);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.MOVING);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.MOVING);
        }
    }
}
