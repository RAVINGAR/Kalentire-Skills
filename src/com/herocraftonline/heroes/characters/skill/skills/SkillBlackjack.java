package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;

public class SkillBlackjack extends TargettedSkill {

    public SkillBlackjack(Heroes plugin) {
        super(plugin, "Blackjack");
        setDescription("Strike your target with a Blackjack, dealing $1 damage and stunning the target for $2 seconds. If you are invisible or sneaking, the stun will instead last $3 seconds.");
        setUsage("/skill blackjack");
        setArgumentRange(0, 0);
        setIdentifiers("skill blackjack");
        setTypes(SkillType.PHYSICAL, SkillType.HARMFUL, SkillType.DAMAGING, SkillType.STEALTHY);
    }

    @Override
    public String getDescription(Hero hero) {

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 75, false);
        double normalDuration = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, "normal-stun-duration", 500, false) / 1000.0);
        double stealthtyDuration = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, "stealthy-stun-duration", 1500, false) / 1000.0);

        return getDescription().replace("$1", damage + "").replace("$2", normalDuration + "").replace("$3", stealthtyDuration + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.DAMAGE.node(), 75);
        node.set("normal-stun-duration", 500);
        node.set("stealthy-stun-duration", 1500);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        broadcastExecuteText(hero, target);

        //deal damage
        addSpellTarget(target, hero);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 75, false);
        damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK);

        // Stun, but only if they are a player.
        if (target instanceof Player) {
            long duration = 0;
            if (hero.hasEffect("Sneak") || hero.hasEffect("Invisible"))
                duration = SkillConfigManager.getUseSetting(hero, this, "stealthy-stun-duration", 1500, false);
            else
                duration = SkillConfigManager.getUseSetting(hero, this, "normal-stun-duration", 500, false);

            Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
            targetHero.addEffect(new StunEffect(this, duration));
        }

        // Remove any invis effects the player may have on them at the time of use.
        for (final Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.INVIS)) {
                hero.removeEffect(effect);
            }
        }

        return SkillResult.NORMAL;
    }
}

/*

                    OLD BLACKJACK BELOW

package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillBlackjack extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillBlackjack(Heroes plugin) {
        super(plugin, "Blackjack");
		setDescription("Prepare a blackjack. Once prepared, your attacks have a $1% chance to stun your target.");
        setUsage("/skill blackjack");
        setArgumentRange(0, 0);
        setIdentifiers("skill blackjack", "skill bjack");
		setTypes(SkillType.PHYSICAL, SkillType.STEALTHY, SkillType.BUFF);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.APPLY_TEXT.node(), "You prepare your blackjack!");
		node.set(SkillSetting.EXPIRE_TEXT.node(), "You sheathe your blackjack!");
        node.set("stun-duration", 5000);
        node.set("stun-chance", 0.20);
        node.set(SkillSetting.DURATION.node(), 20000);
        return node;
    }

    @Override
    public void init() {
        super.init();
		applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "You prepare your blackjack!").replace("%hero%", "$1");
		expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "You sheathe your blackjack!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        hero.addEffect(new BlackjackEffect(this, duration));
        return SkillResult.NORMAL;
    }

    public class BlackjackEffect extends ExpirableEffect {

        public BlackjackEffect(Skill skill, long duration) {
            super(skill, "Blackjack", duration);
            this.types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
			Messaging.send(hero.getPlayer(), applyText, new Object[0]);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
			Messaging.send(hero.getPlayer(), expireText, new Object[0]);
        }

    }

    public class SkillEntityListener implements Listener {

        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
                return;
            }
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
                if (subEvent.getCause() != DamageCause.ENTITY_ATTACK || !(subEvent.getDamager() instanceof Player)) {
                    return;
                }

                Hero attackingHero = plugin.getCharacterManager().getHero((Player) subEvent.getDamager());
                if (!attackingHero.hasEffect("Blackjack")) {
                    return;
                }
                
                Hero defendingHero = plugin.getCharacterManager().getHero((Player) event.getEntity());
                
                if (defendingHero.hasEffect("Stun")) {
                	return;
                }

                double chance = SkillConfigManager.getUseSetting(attackingHero, skill, "stun-chance", 0.20, false);
                if (Util.nextRand() < chance) {
                    int duration = SkillConfigManager.getUseSetting(attackingHero, skill, "stun-duration", 5000, false);
                    defendingHero.addEffect(new StunEffect(skill, duration));
                }
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double chance = SkillConfigManager.getUseSetting(hero, this, "stun-chance", .20, false) * 100;
        return getDescription().replace("$1", Util.stringDouble(chance));
    }
}
*/