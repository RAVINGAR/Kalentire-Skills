package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.CharacterDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillRuneword extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillRuneword(Heroes plugin) {
        super(plugin, "Runeword");
        setDescription("Your target $1% more magic damage!");
        setArgumentRange(0, 0);
        setUsage("/skill runeword");
        setIdentifiers("skill runeword");
		setTypes(SkillType.DARK, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("damage-bonus", 1.25);
        node.set(Setting.RADIUS.node(), 10);
        node.set(Setting.APPLY_TEXT.node(), "%target% has been cursed by a Runeword!");
        node.set(Setting.EXPIRE_TEXT.node(), "The Runeword's curse fades from %target%!");
        node.set(Setting.DURATION.node(), 600000); // in Milliseconds - 10 minutes
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%target% has been cursed by a Runeword!").replace("%target%", "$!");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "The Runeword's curse fades from %target%!").replace("%target%", "$!");
    }
    
    @Override
	public SkillResult use(Hero hero, LivingEntity target, String[] args) {
    	Player player = hero.getPlayer();
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 600000, false);
        double damageBonus = SkillConfigManager.getUseSetting(hero, this, "damage-bonus", 1.25, false);
        RunewordEffect effect = new RunewordEffect(this, duration, damageBonus);
        if(Skill.damageCheck(player, target)) {
        	plugin.getCharacterManager().getCharacter(target).addEffect(effect);
        } else {
        	return SkillResult.INVALID_TARGET;
        }
		return SkillResult.NORMAL;
	}

    public class RunewordEffect extends ExpirableEffect {

        private final double damageBonus;

        public RunewordEffect(Skill skill, long duration, double damageBonus) {
            super(skill, "Runeword", duration);
            this.damageBonus = damageBonus;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            Messaging.send(player, applyText, player.getName());
        }

        public double getDamageBonus() {
            return damageBonus;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            Messaging.send(player, expireText, player.getName());
        }
    }

    public class SkillHeroListener implements Listener {

        @EventHandler()
        public void onMagicDamage(CharacterDamageEvent event) {
            if (event.getCause() != DamageCause.MAGIC) {
                return;
            }

            CharacterTemplate character = SkillRuneword.this.plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            if (character.hasEffect("Runeword")) {
                double damageBonus = ((RunewordEffect) character.getEffect("Runeword")).damageBonus;
                event.setDamage((int) (event.getDamage() * damageBonus));
            }           
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double bonus = SkillConfigManager.getUseSetting(hero, this, "damage-bonus", 1.25, false);
        return getDescription().replace("$1", (int)(bonus - 1D) + "");
    }
}
