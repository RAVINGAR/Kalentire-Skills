package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillSupremacy extends ActiveSkill {
	
	private String applyText;
    private String expireText;

    public SkillSupremacy(Heroes plugin) {
        super(plugin, "Supremacy");
        setDescription("You increase your party's damage with weapons and skills by $1%!");
        setArgumentRange(0, 0);
        setUsage("/skill supremacy");
        setIdentifiers("skill supremacy");
        setTypes(SkillType.BUFF, SkillType.SILENCABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("damage-bonus", 1.25);
        node.set(Setting.RADIUS.node(), 10);
        node.set(Setting.APPLY_TEXT.node(), "Your pulsate with power!");
        node.set(Setting.EXPIRE_TEXT.node(), "You feel power fading!");
        node.set(Setting.DURATION.node(), 600000); //10 minutes
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "You pulsate with power!");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "You feel power fading");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 600000, false);
        double damageBonus = SkillConfigManager.getUseSetting(hero, this, "damage-bonus", 1.25, false);

        SupremacyEffect effect = new SupremacyEffect(this, duration, damageBonus);
        if (!hero.hasParty()) {
        	if (hero.hasEffect("Supremacy")) {
                if (((SupremacyEffect) hero.getEffect("Supremacy")).getDamageBonus() > effect.getDamageBonus()) {
                    Messaging.send(hero.getPlayer(), "You have a more powerful effect already!");
                } else {
                	hero.addEffect(effect);
                }
            }
        } else {
            int range = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 10, false);
            int rangeSquared = range * range;
            Location loc = player.getLocation();
            for (Hero pHero : hero.getParty().getMembers()) {
                Player pPlayer = pHero.getPlayer();
                if (!pPlayer.getWorld().equals(player.getWorld())) {
                    continue;
                }
                if (pPlayer.getLocation().distanceSquared(loc) > rangeSquared) {
                    continue;
                }
                if (pHero.hasEffect("Supremacy")) {
                    if (((SupremacyEffect) pHero.getEffect("Supremacy")).getDamageBonus() > effect.getDamageBonus()) {
                        continue;
                    }
                }
                pHero.addEffect(effect);
            }
        }

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class SupremacyEffect extends ExpirableEffect {

        private final double damageBonus;

        public SupremacyEffect(Skill skill, long duration, double damageBonus) {
            super(skill, "Supremacy", duration);
            this.damageBonus = damageBonus;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            Messaging.send(player, applyText);
        }

        public double getDamageBonus() {
            return damageBonus;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            Messaging.send(player, expireText);
        }
    }

    public class SkillHeroListener implements Listener {

        @EventHandler()
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getCause() != DamageCause.ENTITY_ATTACK) {
                return;
            }

            CharacterTemplate character = event.getDamager();
            if (event.getDamager().hasEffect("Supremacy")) {
                double damageBonus = ((SupremacyEffect) character.getEffect("Supremacy")).damageBonus;
                event.setDamage((int) (event.getDamage() * damageBonus));
            }           
        }
        
        @EventHandler
        public void onSkillDamage(SkillDamageEvent event) {
        	
        	CharacterTemplate character = event.getDamager();
        	if(character.hasEffect("Supremacy")) {
        		double damageBonus = ((SupremacyEffect) character.getEffect("Supremacy")).damageBonus;
        		event.setDamage((int)(event.getDamage() * damageBonus));
        	}
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double bonus = SkillConfigManager.getUseSetting(hero, this, "damage-bonus", 1.25, false);
        return getDescription().replace("$1", Util.stringDouble((bonus - 1) * 100));
    }

}
