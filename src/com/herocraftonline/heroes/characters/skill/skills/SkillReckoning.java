package com.herocraftonline.heroes.characters.skill.skills;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.util.Util;
import fr.neatmonster.nocheatplus.checks.CheckType;
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

    public SkillReckoning(Heroes plugin) {
        super(plugin, "Reckoning");
        setDescription("Reckon all enemies within $1 blocks, dealing $2 damage and pulling them towards you. Reckoned targets are slowed for $3 seconds. The strength of the slow is increased by your Intellect.");
        setUsage("/skill reckoning");
        setArgumentRange(0, 0);
        setIdentifiers("skill reckoning");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.FORCE, SkillType.AGGRESSIVE, SkillType.INTERRUPTING, SkillType.AREA_OF_EFFECT);
    }

    @Override
    public String getDescription(Hero hero) {

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.5, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
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
        node.set("lightning-volume", 0.0F);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.5, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        int intellect = hero.getAttributeValue(AttributeType.INTELLECT);

        int slowAmount = SkillConfigManager.getUseSetting(hero, this, "slow-amount", 1, false);
        double slowAmountIncrease = SkillConfigManager.getUseSetting(hero, this, "slow-amount-increase-per-intellect", 0.075, false);
        slowAmount += Math.floor(slowAmountIncrease * intellect);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 750, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_INTELLECT, 12, false);
        duration += durationIncrease * intellect;

        Location playerLoc = player.getLocation();

        long currentTime = System.currentTimeMillis();
        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            final LivingEntity target = (LivingEntity) entity;
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);

            if (!damageCheck(player, target))
                continue;

            Location targetLoc = target.getLocation();

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC, false);

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

            double xDir = (playerLoc.getX() - targetLoc.getX()) / 3D;
            double zDir = (playerLoc.getZ() - targetLoc.getZ()) / 3D;
            final Vector v = new Vector(xDir, 0, zDir).multiply(0.5).setY(0.5);

            // Let's bypass the nocheat issues...
            NCPUtils.applyExemptions(target, new NCPFunction() {
                
                @Override
                public void execute()
                {
                    target.setVelocity(v);                    
                }
            }, Lists.newArrayList(CheckType.MOVING), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 500, false));
        }


        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.CLOUD, 3);
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.CLOUD, 3);
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.CLOUD, 3);
        player.getWorld().playSound(player.getLocation(), Sound.AMBIENCE_THUNDER, 0.0F, 0.0F);
        player.getWorld().strikeLightningEffect(player.getLocation());


        return SkillResult.NORMAL;
    }
}
