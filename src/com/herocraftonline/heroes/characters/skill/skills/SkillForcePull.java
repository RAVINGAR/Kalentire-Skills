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
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillForcePull extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillForcePull(Heroes plugin) {
        super(plugin, "Forcepull");
        setDescription("Forces your target toward you.");
        setUsage("/skill forcepull");
        setArgumentRange(0, 0);
        setIdentifiers("skill forcepull", "skill fpull");
        setTypes(SkillType.FORCE, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.INTERRUPT);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(0));
        node.set("horizontal-power", Double.valueOf(3.0));
        node.set("vertical-power", Double.valueOf(0.5));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 0, false);
        if (damage > 0) {
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC);
        }

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        final double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 1.0, false);
        Vector pushUpVector = new Vector(0, vPower, 0);
        target.setVelocity(pushUpVector);

        final double xDir = (playerLoc.getX() - targetLoc.getX()) / 3;
        final double zDir = (playerLoc.getZ() - targetLoc.getZ()) / 3;
        final double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 3.0, false);

        // push them "up" first. THEN we can pull them to us.
        double delay = SkillConfigManager.getUseSetting(hero, this, "pull-delay", 0.5, false);
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

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

}