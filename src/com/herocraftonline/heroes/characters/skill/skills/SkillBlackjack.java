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
import com.herocraftonline.heroes.util.Util;

public class SkillBlackjack extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillBlackjack(Heroes plugin) {
        super(plugin, "Blackjack");
        setDescription("Your attacks have a $1% chance to stun your target.");
        setUsage("/skill blackjack");
        setArgumentRange(0, 0);
        setIdentifiers("skill blackjack", "skill bjack");
        setTypes(SkillType.PHYSICAL, SkillType.BUFF);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% prepared his blackjack!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% sheathed his blackjack!");
        node.set("stun-duration", 3000);
        node.set("stun-chance", 0.20);
        node.set(SkillSetting.DURATION.node(), 20000);
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% prepared his blackjack!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% sheathed his blackjack!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
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
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
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