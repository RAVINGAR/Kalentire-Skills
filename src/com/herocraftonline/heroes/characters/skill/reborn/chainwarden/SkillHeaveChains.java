package com.herocraftonline.heroes.characters.skill.reborn.chainwarden;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class SkillHeaveChains extends ActiveSkill {
    public SkillHeaveChains(Heroes plugin) {
        super(plugin, "HeaveChains");
        setDescription("Heave all yo damn chains (Tell delf to fix this)");
        setUsage("/skill heavechains");
        setIdentifiers("skill heavechains");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.MULTI_GRESSIVE);
    }

    public String getDescription(Hero hero) {
        return super.getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.RADIUS.node(), 30);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        SkillYank yankSkill = (SkillYank) plugin.getSkillManager().getSkill("Yank");
        if (yankSkill == null) {
            Heroes.log(Level.SEVERE, "SkillYank is missing from the server. SkillHeave will no longer work. SkillYank _must_ be available to the class that has SkillHeave.");
            player.sendMessage("One of the Admins or devs broke this skill. Tell them to read the heroes logs to fix it.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int hitCount = 0;
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 30.0, false);
        for (Entity entity : player.getNearbyEntities(radius, radius / 2.0, radius)) {
            if (!(entity instanceof LivingEntity))
                continue;

            LivingEntity target = (LivingEntity) entity;
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            String effectName = player.getName() + "-Hooked";
            if (!targetCT.hasEffect(effectName))
                continue;

            SkillResult result = yankSkill.use(hero, target, new String[]{"NoBroadcast"});
            if (result == SkillResult.NORMAL) {
                hitCount++;
            }
        }

        if (hitCount < 1)
            return SkillResult.INVALID_TARGET;

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

//    private void damageEnemy(Hero hero, LivingEntity target, Player player) {
//        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50.0, false);
//        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.0, false);
//        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);
//
//        if (damage > 0) {
//            addSpellTarget(target, hero);
//            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
//        }
//    }
//
//    private void pullTarget(Hero hero, LivingEntity target, double vPower, double hPower, double xDir, double zDir) {
//        double delay = SkillConfigManager.getUseSetting(hero, this, "pull-delay", 0.2, false);
//        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
//            public void run() {
//                Vector pushVector = new Vector(xDir, 0, zDir).normalize().multiply(hPower).setY(vPower);
//                target.setVelocity(pushVector);
//                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHEST_LOCKED, 0.5F, 1.5F);
//            }
//        }, (long) (delay * 20));
//    }
//
//    private void pushTargetUpwards(Hero hero, LivingEntity target, double vPower, boolean reduceFallDamage) {
//        final Vector pushUpVector = new Vector(0, vPower, 0);
//
//        long exemptionDuration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1000, false);
//        if (exemptionDuration > 0) {
//            NCPUtils.applyExemptions(target, new NCPFunction() {
//                @Override
//                public void execute() {
//                    target.setVelocity(pushUpVector);
//                }
//            }, Lists.newArrayList("MOVING"), exemptionDuration);
//        } else {
//            target.setVelocity(pushUpVector);
//        }
//
//        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHEST_LOCKED, 0.5F, 1.5F);
//
//        if (reduceFallDamage)
//            target.setFallDistance(target.getFallDistance() - 3F);
//    }
//
//    private boolean shouldWeaken(Location targetLoc) {
//        Material mat = targetLoc.getBlock().getRelative(BlockFace.DOWN).getType();
//        switch (mat) {
//            case WATER:
//            case LAVA:
//            case SOUL_SAND:
//                return true;
//            default:
//                return false;
//        }
//    }
}