package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.Stacking;
import com.herocraftonline.heroes.characters.skill.*;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillShatterTime extends TargettedSkill {

    public SkillShatterTime(Heroes plugin) {
        super(plugin, "ShatterTime");
        setDescription("Attempt to shatter time around your target, dealing $1 damage and an additional "
                + "$2 damage for each time based effect affecting them.");//If used on an ally, it will at shattertime of lightning down on the target dealing $1 damage.");
        setUsage("/skill shattertime");
        setArgumentRange(0, 0);
        setIdentifiers("skill shattertime");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40.0, false);
        double damagePerStack = SkillConfigManager.getUseSetting(hero, this, "damage-per-time-effect", 10.0, false);

        return getDescription().replace("$1", damage + "").replace("$2", damagePerStack + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 40.0);
        node.set("damage-per-time-effect", 10.0);
        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
        node.set("damage-delay", 1000);
        node.set("temporal-ward-duration", 500);
        node.set(SkillSetting.USE_TEXT.node(), "%hero% is shattering %target%'s time!");
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        final Player player = hero.getPlayer();

        CharacterTemplate ctTarget = plugin.getCharacterManager().getCharacter(target);
        if (ctTarget == null)
            return SkillResult.INVALID_TARGET;

        if (ctTarget.hasEffect("TemporallyWarded")) {
            player.sendMessage(ChatColor.WHITE + "Unable to shift " + target.getName() + "'s time. They are currently warded against time altering effects!");
            return SkillResult.INVALID_TARGET;
        }

        double baseDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40.0, false);
        final double damagePerStack = SkillConfigManager.getUseSetting(hero, this, "damage-per-time-effect", 10.0, false);
        final int temporalWardDuration = SkillConfigManager.getUseSetting(hero, this, "temporal-ward-duration", 500, false);

        double damage = baseDamage;
        for (Effect effect : ctTarget.getEffects()) {
            if (!effect.getName().contains("Time"))
                continue;

            if (effect instanceof Stacking) {
                Stacking stack = (Stacking) effect;
                damage += (damagePerStack * stack.getStackCount());
            } else {
                damage += damagePerStack;
            }
            ctTarget.removeEffect(effect);
        }

        final Skill skill = this;
        final World world = target.getWorld();
        final Location loc = target.getLocation();
        final double finalDamage = damage;
        final int delaySeconds = SkillConfigManager.getUseSetting(hero, this, "damage-delay", 1000, false);
        final int delayTicks = delaySeconds / 50;
        final int displayPeriod = 3;

        EffectManager em = new EffectManager(plugin);
        SphereEffect visualEffect = new SphereEffect(em);

        DynamicLocation dynamicLoc = new DynamicLocation(target);
        visualEffect.setDynamicOrigin(dynamicLoc);
        visualEffect.disappearWithOriginEntity = true;

        visualEffect.radius = 5;
        visualEffect.radiusIncrease = -0.5;
        visualEffect.period = displayPeriod;
        visualEffect.iterations = delayTicks / displayPeriod;

        visualEffect.color = Color.ORANGE;
        visualEffect.particle = Particle.REDSTONE;
        visualEffect.particleCount = 3;

        visualEffect.callback = new Runnable() {

            @Override
            public void run() {
                plugin.getDamageManager().addSpellTarget(target, hero, skill);
                damageEntity(target, player, finalDamage, DamageCause.MAGIC, false);
                world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0F);

                if (!ctTarget.hasEffect("TemporallyWarded")) {
                    SkillTemporalWard.TemporalWardEffect effect = new SkillTemporalWard.TemporalWardEffect(skill, player, temporalWardDuration);
                    effect.types.add(EffectType.SILENCE);
                    effect.types.add(EffectType.DISARM);
                    ctTarget.addEffect(effect);
                }
            }
        };

        em.start(visualEffect);
        em.disposeOnTermination();

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }
}
