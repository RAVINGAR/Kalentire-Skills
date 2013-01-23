package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
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

public class SkillConviction extends ActiveSkill {

	private String applyText;
    private String expireText;

    public SkillConviction(Heroes plugin) {
        super(plugin, "Conviction");
        setDescription("You reduce all damage taken by your party by $1%!");
        setArgumentRange(0, 0);
        setUsage("/skill conviction");
        setIdentifiers("skill conviction");
        setTypes(SkillType.BUFF, SkillType.SILENCABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("damage-modifier", 0.75);
        node.set(Setting.RADIUS.node(), 10);
        node.set(Setting.APPLY_TEXT.node(), "You are filled with renewed convinction!");
        node.set(Setting.EXPIRE_TEXT.node(), "Your sense of convinction begins to fade!");
        node.set(Setting.DURATION.node(), 600000); //10 minutes
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "You are filled with renewed convinction!");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "Your sense of convinction begins to fade!");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 600000, false);
        double damageModifier = SkillConfigManager.getUseSetting(hero, this, "damage-modifier", 0.75, false);

        ConvictionEffect effect = new ConvictionEffect(this, duration, damageModifier, applyText, expireText);
        if (!hero.hasParty()) {
        	if (hero.hasEffect("Conviction")) {
                if (((ConvictionEffect) hero.getEffect("Conviction")).getDamageModifier() < effect.getDamageModifier()) {
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
                if (pHero.hasEffect("Conviction")) {
                    if (((ConvictionEffect) pHero.getEffect("Conviction")).getDamageModifier() < effect.getDamageModifier()) {
                        continue;
                    }
                }
                pHero.addEffect(effect);
            }
        }
        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ANVIL_LAND , 0.6F, 1.0F);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public static class ConvictionEffect extends ExpirableEffect {

        private double damageModifier;
        private String applyText;
        private String expireText;

        public ConvictionEffect(Skill skill, long duration, double damageModifier, String applyText, String expireText) {
            super(skill, "Conviction", duration);
            this.damageModifier = damageModifier;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.MAGIC);
            this.applyText = applyText;
            this.expireText = expireText;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            Messaging.send(player, applyText);
        }

        public double getDamageModifier() {
            return damageModifier;
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

            CharacterTemplate character = SkillConviction.this.plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            if (character.hasEffect("Conviction")) {
                double damageModifier = ((ConvictionEffect) character.getEffect("Conviction")).damageModifier;
                event.setDamage((int) (event.getDamage() * damageModifier));
            }           
        }
        
        @EventHandler
        public void onSkillDamage(SkillDamageEvent event) {
        	
        	CharacterTemplate character = SkillConviction.this.plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
        	if(character.hasEffect("Conviction")) {
        		double damageModifier = ((ConvictionEffect) character.getEffect("Conviction")).damageModifier;
        		event.setDamage((int)(event.getDamage() * damageModifier));
        	}
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double bonus = SkillConfigManager.getUseSetting(hero, this, "damage-modifier", 0.75, false);
        return getDescription().replace("$1", Util.stringDouble((1D - bonus) * 100));
    }

}
