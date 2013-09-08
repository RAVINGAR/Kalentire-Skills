package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillSuperJump extends ActiveSkill {

    private boolean ncpEnabled = false;
    public VisualEffect fplayer = new VisualEffect();

    public SkillSuperJump(Heroes plugin) {
        super(plugin, "SuperJump");
        setDescription("You launch into the air, and float safely to the ground.");
        setUsage("/skill superjump");
        setArgumentRange(0, 0);
        setIdentifiers("skill superjump");
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("horizontal-power", Double.valueOf(0.0));
        node.set("horizontal-power-increase-per-agility", Double.valueOf(0.0));
        node.set("vertical-power", Double.valueOf(1.5));
        node.set("vertical-power-increase-per-agility", Double.valueOf(0.0375));
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set("ncp-exemption-duration", Integer.valueOf(0));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        if (ncpEnabled) {
            if (!player.isOp()) {
                long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1500, false);
                if (duration > 0) {
                    NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, player, duration);
                    hero.addEffect(ncpExemptEffect);
                }
            }
        }

        broadcastExecuteText(hero);

        int agility = hero.getAttributeValue(AttributeType.AGILITY);

        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", Double.valueOf(0.5), false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-agility", Double.valueOf(0.0125), false);
        vPower += agility * vPowerIncrease;
        Vector velocity = player.getVelocity().setY(vPower);

        Vector directionVector = player.getLocation().getDirection();
        directionVector.setY(0);
        directionVector.normalize();

        velocity.add(directionVector);
        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", Double.valueOf(0.5), false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-agility", Double.valueOf(0.0125), false);
        hPower += agility * hPowerIncrease;
        velocity.multiply(new Vector(hPower, vPower, hPower));

        // Super Jump!
        player.setVelocity(velocity);
        player.setFallDistance(-8f);

        int duration = (int) SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 5000, false);
        hero.addEffect(new JumpEffect(this, player, duration));

        player.getWorld().playSound(player.getLocation(), Sound.EXPLODE, 0.5F, 1.0F);

        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), player.getLocation().add(0, 10, 0),
                                 FireworkEffect.builder().flicker(false).trail(false).
                                               with(FireworkEffect.Type.STAR)
                                               .withColor(Color.BLUE)
                                               .withFade(Color.GRAY)
                                               .build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
    }

    public class JumpEffect extends SafeFallEffect {

        public JumpEffect(Skill skill, Player applier, int duration) {
            super(skill, "SuperJump", applier, duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.PHYSICAL);
            types.add(EffectType.JUMP_BOOST);

            addMobEffect(8, duration / 1000 * 20, 5, false);
        }
    }

    private class NCPExemptionEffect extends ExpirableEffect {

        public NCPExemptionEffect(Skill skill, Player applier, long duration) {
            super(skill, "NCPExemptionEffect", applier, duration);
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