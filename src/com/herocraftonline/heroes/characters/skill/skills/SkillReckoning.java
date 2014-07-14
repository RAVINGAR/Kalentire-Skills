package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.List;

public class SkillReckoning extends ActiveSkill {

    private boolean ncpEnabled = false;

    public SkillReckoning(Heroes plugin) {
        super(plugin, "Reckoning");
        setDescription("Reckon all enemies within $1 blocks, dealing $2 damage and pulling them towards you. Reckoned targets are slowed for $3 seconds. The strength of the slow is increased by your Intellect.");
        setUsage("/skill reckoning");
        setArgumentRange(0, 0);
        setIdentifiers("skill reckoning");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.FORCE, SkillType.AGGRESSIVE, SkillType.INTERRUPTING);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
    }

    @Override
    public String getDescription(Hero hero) {

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(5), false);

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(40), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.5, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(3000), false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", radius + "").replace("$2", damage + "").replace("$3", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 40);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.5);
        node.set(SkillSetting.RADIUS.node(), 8);
        node.set(SkillSetting.DURATION.node(), 750);
        node.set(SkillSetting.DURATION_INCREASE_PER_INTELLECT.node(), 500);
        node.set("slow-amplifier", 0);
        node.set("slow-amplifier-increase-per-intellect", 0.075);
        node.set("ncp-exemption-duration", 500);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(5), false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(40), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.5, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        int intellect = hero.getAttributeValue(AttributeType.INTELLECT);

        int slowAmount = SkillConfigManager.getUseSetting(hero, this, "slow-amount", Integer.valueOf(1), false);
        double slowAmountIncrease = SkillConfigManager.getUseSetting(hero, this, "slow-amount-increase-per-intellect", 0.075, false);
        slowAmount += Math.floor(slowAmountIncrease * intellect);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(750), false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_INTELLECT, Integer.valueOf(12), false);
        duration += Math.ceil(durationIncrease * intellect);

        Location playerLoc = player.getLocation();

        long currentTime = System.currentTimeMillis();
        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            LivingEntity target = (LivingEntity) entity;
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);

            if (!damageCheck(player, target))
                continue;

            Location targetLoc = target.getLocation();

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK, false);

            if (targetCT instanceof Hero) {
                Hero enemy = (Hero) targetCT;
                if (enemy.getDelayedSkill() != null) {
                    if (enemy.cancelDelayedSkill())
                        enemy.setCooldown("global", Heroes.properties.globalCooldown + currentTime);
                }
            }

            SlowEffect sEffect = new SlowEffect(this, player, duration, slowAmount, "", "");
            sEffect.types.add(EffectType.DISPELLABLE);
            targetCT.addEffect(sEffect);

            // Let's bypass the nocheat issues...
            if (ncpEnabled) {
                if (target instanceof Player) {
                    Player targetPlayer = (Player) target;
                    if (!targetPlayer.isOp()) {
                        long ncpDuration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 500, false);
                        NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, targetPlayer, ncpDuration);
                        targetCT.addEffect(ncpExemptEffect);
                    }
                }
            }

            double xDir = (playerLoc.getX() - targetLoc.getX()) / 3D;
            double zDir = (playerLoc.getZ() - targetLoc.getZ()) / 3D;
            Vector v = new Vector(xDir, 0, zDir).multiply(0.5).setY(0.5);
            target.setVelocity(v);
        }

        player.getWorld().playEffect(player.getLocation(), Effect.CLOUD, 3);
        player.getWorld().playSound(player.getLocation(), Sound.AMBIENCE_THUNDER, 0.4F, 1.0F);


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
            NCPExemptionManager.exemptPermanently(player, CheckType.FIGHT);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.MOVING);
            NCPExemptionManager.unexempt(player, CheckType.FIGHT);
        }
    }
}
