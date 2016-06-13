package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;

public class SkillSuperJump extends ActiveSkill {

    public VisualEffect fplayer = new VisualEffect();

    public SkillSuperJump(Heroes plugin) {
        super(plugin, "SuperJump");
        setDescription("You launch into the air, and float safely to the ground.");
        setUsage("/skill superjump");
        setArgumentRange(0, 0);
        setIdentifiers("skill superjump");
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 5000);
        node.set("horizontal-power", 0.5);
        node.set("horizontal-power-increase-per-dexterity", 0.0125);
        node.set("vertical-power", 0.5);
        node.set("vertical-power-increase-per-dexterity", 0.00625);
        node.set("ncp-exemption-duration", 2000);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        Material mat = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();

        broadcastExecuteText(hero);

        int dexterity = hero.getAttributeValue(AttributeType.DEXTERITY);

        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.5, false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-dexterity", 0.0125, false);
        vPower += dexterity * vPowerIncrease;

        if (vPower > 4.0)
            vPower = 4.0;

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

        NCPUtils.applyExemptions(player, new NCPFunction() {
            
            @Override
            public void execute()
            {
                // Super Jump!
                player.setVelocity(velocity);
                player.setFallDistance(-8f);
            }
        }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1500, false));

        int duration = (int) SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 5000, false);
        hero.addEffect(new JumpEffect(this, player, duration));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 1.0F);

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

    private class JumpEffect extends SafeFallEffect {

        public JumpEffect(Skill skill, Player applier, long duration) {
            super(skill, applier, duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.PHYSICAL);
            types.add(EffectType.JUMP_BOOST);

            addMobEffect(8, (int) (duration / 1000 * 20), 5, false);
        }
    }
}