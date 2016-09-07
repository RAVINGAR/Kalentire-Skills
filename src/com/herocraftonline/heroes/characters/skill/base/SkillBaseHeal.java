package com.herocraftonline.heroes.characters.skill.base;

import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

//import de.slikey.effectlib.EffectType;
//import de.slikey.effectlib.effect.CylinderEffect;
//import de.slikey.effectlib.util.ParticleEffect;

public abstract class SkillBaseHeal extends TargettedSkill {

    protected SkillBaseHeal(Heroes plugin, String name) {
        super(plugin, name);
    }

    public SkillBaseHeal(Heroes plugin) {
        this(plugin, "Heal");
        setDescription("Heal your target, restoring $1 of their health. You are only healed for $2 health from this ability. You cannot use this ability in combat.");
        setUsage("/skill heal <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill heal");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING.node(), 125, false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 2.0, false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

        String formattedHealing = Util.decFormat.format(healing);
        String formattedSelfHealing = Util.decFormat.format(healing * Heroes.properties.selfHeal);

        return getDescription().replace("$1", formattedHealing).replace("$2", formattedSelfHealing);
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 125, false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 2.0, false);
        healing += hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease;

        double targetHealth = target.getHealth();
        if (targetHealth >= target.getMaxHealth()) {
            if (player.equals(targetHero.getPlayer())) {
                Messaging.send(player, "You are already at full health.");
            }
            else {
                Messaging.send(player, "Target is already fully healed.");
            }
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, healing, this, hero);
        plugin.getServer().getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            Messaging.send(player, "Unable to heal the target at this time!");
            return SkillResult.CANCELLED;
        }
        
		/*CylinderEffect fx = new CylinderEffect(vm);
		fx.setEntity(player);			
		fx.particle = ParticleEffect.VILLAGER_HAPPY;
		fx.height = 2;
		fx.particles = 30;
		fx.solid = false;
		fx.radius = 1;
		fx.offset = new Vector(0, -0.5, 0);
		fx.type = EffectType.INSTANT;
		fx.run();*/
        
        targetHero.heal(hrhEvent.getAmount());

        removeEffects(targetHero);

        broadcastExecuteText(hero, target);

        applySoundEffects(player.getWorld(), target);
        applyParticleEffects(player.getWorld(), target);

        return SkillResult.NORMAL;
    }

    protected abstract void removeEffects(Hero hero);

    protected void applySoundEffects(World world, LivingEntity target) {
        world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.5f, 1.0f);
    }

    protected void applyParticleEffects(World world, LivingEntity target) {
        world.spigot().playEffect(target.getLocation().add(0, 0.5, 0), // location
                org.bukkit.Effect.HAPPY_VILLAGER, // effect
                0, // id
                0, // data
                1, 1, 1, // offset
                1.0f, // speed
                25, // particle count
                1); // radius
		
		/*CylinderEffect fx2 = new CylinderEffect(em);
		fx2.setEntity(targetHero.getPlayer());			
		fx2.particle = ParticleEffect.HEART;
		fx2.height = 2;
		fx2.particles = 15;
		fx2.solid = false;
		fx2.type = EffectType.DELAYED;
		fx2.delay = 20;
		fx2.radius = 1;
		fx2.offset = new Vector(0, -0.5, 0);
		fx2.start();*/
    }
}