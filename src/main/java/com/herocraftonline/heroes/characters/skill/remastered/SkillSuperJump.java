package com.herocraftonline.heroes.characters.skill.remastered;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class SkillSuperJump extends ActiveSkill {

    public VisualEffect fplayer = new VisualEffect();

    public SkillSuperJump(Heroes plugin) {
        super(plugin, "SuperJump");
        setDescription("You launch into the air at amazing speeds! " +
                "You will be protected from fall damage for $1 seconds after jumping, but the effect takes a toll on your body, preventing stamina regeneration for the same duration.");
        setUsage("/skill superjump");
        setIdentifiers("skill superjump");
        setArgumentRange(0, 0);
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL);
    }

    @Override
    public String getDescription(Hero hero) {
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 5000, false);
        return getDescription()
                .replace("$1", Util.decFormat.format((double) duration / 1000));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 4000);
        config.set("horizontal-power", 0.0);
        config.set("horizontal-power-increase-per-dexterity", 0.0);
        config.set("vertical-power", 2.0);
        config.set("vertical-power-increase-per-dexterity", 0.0);
        config.set("ncp-exemption-duration", 0);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        superJump(hero, player);

        return SkillResult.NORMAL;
    }

    private void superJump(Hero hero, Player player) {
        int dexterity = hero.getAttributeValue(AttributeType.DEXTERITY);
        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.5, false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-dexterity", 0.0125, false);
        vPower += dexterity * vPowerIncrease;

        if (vPower > 4.0)
            vPower = 4.0;

        Material mat = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
        switch (mat) {
            case WATER:
            case LAVA:
            case SOUL_SAND:
                vPower *= 0.75;
                break;
            default:
                break;
        }

        final Vector velocity = player.getVelocity().setY(vPower);

        Vector directionVector = player.getLocation().getDirection();
        directionVector.setY(0);
        directionVector.normalize();

        velocity.add(directionVector);
        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 0.5, false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-dexterity", 0.0125, false);
        hPower += dexterity * hPowerIncrease;

        if (hPower > 8.0)
            hPower = 8.0;

        switch (mat) {
            case WATER:
            case LAVA:
            case SOUL_SAND:
                hPower *= 0.75;
                break;
            default:
                break;
        }

        velocity.multiply(new Vector(hPower, 1, hPower));

        long exemptionDuration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1500, false);
        if (exemptionDuration > 0) {
            NCPUtils.applyExemptions(player, new NCPFunction() {
                @Override
                public void execute() {
                    applyJumpVelocity(player, velocity);
                }
            }, Lists.newArrayList("MOVING"), exemptionDuration);
        } else {
            applyJumpVelocity(player, velocity);
        }

        int duration = (int) SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 5000, false);
        hero.addEffect(new JumpEffect(this, player, duration));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 1.0F);
        VisualEffect.playInstantFirework(FireworkEffect.builder()
                .flicker(false)
                .trail(false)
                .with(FireworkEffect.Type.STAR)
                .withColor(Color.BLUE)
                .withFade(Color.GRAY)
                .build(), player.getLocation().add(0, 10, 0));
    }

    private void applyJumpVelocity(Player player, Vector velocity) {
        player.setVelocity(velocity);
        player.setFallDistance(-8f);
    }

    private class JumpEffect extends SafeFallEffect {
        JumpEffect(Skill skill, Player applier, long duration) {
            super(skill, applier, duration, null, null);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.PHYSICAL);
            types.add(EffectType.STAMINA_FREEZING);
            this.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, (int) duration / 50, 5));
        }
    }
}