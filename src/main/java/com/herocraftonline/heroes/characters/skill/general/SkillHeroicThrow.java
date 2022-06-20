package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
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
    public SkillHeroicThrow(Heroes plugin) {
        super(plugin, "HeroicThrow");
        setDescription("Grab your target and chuck them in the opposite direction! Distance thrown increases per level.");
        setUsage("/skill HeroicThrow");
        setArgumentRange(0,0);
        setIdentifiers("skill heroicthrow");
        setTypes(SkillType.FORCE, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.INTERRUPTING, SkillType.SILENCEABLE);
        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null)
            ncpEnabled = true;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(5));
        node.set("horizontal-power", Double.valueOf(0.5));
        node.set("horizontal-power-increase", Double.valueOf(0.025));
        node.set("vertical-power", Double.valueOf(0.5));
        node.set("vertical-power-increase", Double.valueOf(0.0175));
        node.set("ncp-exemption-duration", 1500);
        node.set("safefall-duration", 1500);
        node.set("toss-delay", Double.valueOf(0.2));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(5));
        return node;
    }

    @SuppressWarnings("deprecation")
	@Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 5, false);
        if(target.equals(player)) {
       	 return SkillResult.INVALID_TARGET_NO_MSG;
       }
       
        broadcastExecuteText(hero, target);
        damageEntity(target, player, damage,DamageCause.ENTITY_ATTACK);
      
        

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

        Location originalLoc = player.getLocation();
        Location flippedLoc = new Location(originalLoc.getWorld(), originalLoc.getX(), originalLoc.getY(), originalLoc.getZ(), (originalLoc.getYaw() < 180 ? originalLoc.getYaw() - 180 : originalLoc.getYaw() + 180), originalLoc.getPitch());
        player.teleport(flippedLoc);
        
        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        Material mat = targetLoc.getBlock().getRelative(BlockFace.DOWN).getType();

        boolean weakenVelocity = false;
        switch (mat) {
            case WATER:
            case LAVA:
            case SOUL_SAND:
                weakenVelocity = true;
                break;
            default:
                break;
        }
        long sfDuration = SkillConfigManager.getUseSetting(hero, this, "safefall-duration", 1500, false);

        	CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        	targetCT.addEffect(new SafeFallEffect(this, player, sfDuration));
        
        
        double tempVPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", Double.valueOf(0.25), false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase", Double.valueOf(0.0075), false);
        tempVPower += (vPowerIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        if (weakenVelocity)
            tempVPower *= 0.75;

        final double vPower = tempVPower;

        Vector pushUpVector = new Vector(0, vPower, 0);
        target.setVelocity(pushUpVector);

        final double xDir = playerLoc.getX() - targetLoc.getX();
        final double zDir = playerLoc.getZ() - targetLoc.getZ();

        double tempHPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", Double.valueOf(1.5), false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase", Double.valueOf(0.0375), false);
        tempHPower += (hPowerIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        if (weakenVelocity)
            tempHPower *= 0.75;

        final double hPower = tempHPower;

        // Push them "up" first. THEN toss them.
        double delay = SkillConfigManager.getUseSetting(hero, this, "toss-delay", 0.2, false);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                Vector pushVector = new Vector(xDir, 0, zDir).normalize().multiply(hPower).setY(vPower);
                target.setVelocity(pushVector);
            }
        }, (long) (delay * 20));

        // Play sound

        return SkillResult.NORMAL;
    }
    
    public class SafeFallEffect
      extends ExpirableEffect
    {
    
      
      public SafeFallEffect(Skill skill, String name, Player applier, long duration)
      {
        super(skill, name, applier, duration);
        this.types.add(EffectType.DISPELLABLE);
        this.types.add(EffectType.BENEFICIAL);
        this.types.add(EffectType.SAFEFALL);
        this.types.add(EffectType.MAGIC);
      }
      
      public SafeFallEffect(Skill skill, Player applier, long duration)
      {
        this(skill, "Safefall", applier, duration);
      }
      
      public void apply(CharacterTemplate cT)
      {
        super.apply(cT);
        
      }
      
      public void remove(CharacterTemplate cT)
      {
        super.remove(cT);
       
      }
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