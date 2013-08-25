package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillSpear extends TargettedSkill {

    private boolean ncpEnabled = false;

    public SkillSpear(Heroes plugin) {
        super(plugin, "Spear");
        setDescription("Spear your target, pulling him back towards you and dealing $1 damage");
        setUsage("/skill spear");
        setArgumentRange(0, 0);
        setIdentifiers("skill spear");
        setTypes(SkillType.PHYSICAL, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.INTERRUPT);

        try {
            if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
                ncpEnabled = true;
            }
        }
        catch (Exception e) {}
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 82, false);

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 8);
        node.set("weapons", Util.shovels);
        node.set("ncp-exemption-duration", 1000);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        Material item = player.getItemInHand().getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.shovels).contains(item.name())) {
            Messaging.send(player, "You can't use spear with that weapon!");
            return SkillResult.FAIL;
        }

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
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
                    NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, duration);
                    CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                    targetCT.addEffect(ncpExemptEffect);
                }
            }
        }

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        double xDir = (playerLoc.getX() - targetLoc.getX()) / 3;
        double zDir = (playerLoc.getZ() - targetLoc.getZ()) / 3;
        Vector v = new Vector(xDir, 0, zDir).multiply(0.5).setY(0.5);
        target.setVelocity(v);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.HURT, 0.8F, 1.0F);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    private class NCPExemptionEffect extends ExpirableEffect {

        public NCPExemptionEffect(Skill skill, long duration) {
            super(skill, "NCPExemptionEffect_MOVING", duration);
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
