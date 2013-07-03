package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillRuneword extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillRuneword(Heroes plugin) {
        super(plugin, "Runeword");
        setDescription("Your target takes $1% more magic damage!");
        setArgumentRange(0, 0);
        setUsage("/skill runeword");
        setIdentifiers("skill runeword");
		setTypes(SkillType.DARK, SkillType.SILENCABLE, SkillType.DEBUFF, SkillType.HARMFUL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("damage-bonus", 1.25);
        node.set(SkillSetting.RADIUS.node(), 10);
        node.set(SkillSetting.APPLY_TEXT.node(), "§7[§2Skill§7] %target% has been cursed by a Runeword!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "§7[§2Skill§7] The Runeword's curse fades from %target%!");
        node.set(SkillSetting.DURATION.node(), 600000); // in Milliseconds - 10 minutes
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "§7[§2Skill§7] %target% has been cursed by a Runeword!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "§7[§2Skill§7] The Runeword's curse fades from %target%!").replace("%target%", "$1");
    }
    
    @Override
	public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
        double damageBonus = SkillConfigManager.getUseSetting(hero, this, "damage-bonus", 1.25, false);
        RunewordEffect effect = new RunewordEffect(this, duration, damageBonus);
        plugin.getCharacterManager().getCharacter(target).addEffect(effect);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENDERDRAGON_DEATH , 0.5F, 1.0F);
		return SkillResult.NORMAL;
	}

    public class RunewordEffect extends ExpirableEffect {

        private final double damageBonus;

        public RunewordEffect(Skill skill, long duration, double damageBonus) {
            super(skill, "Runeword", duration);
            this.damageBonus = damageBonus;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), applyText, player.getName());
        }

        public double getDamageBonus() {
            return damageBonus;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), expireText, player.getName());
        }
    }

    public class SkillHeroListener implements Listener {
    	
        @EventHandler
        public void onSkillDamage(SkillDamageEvent event) {
        	Skill eventSkill = event.getSkill();
        	if(eventSkill.isType(SkillType.PHYSICAL) || !eventSkill.isType(SkillType.DAMAGING))
        		return;
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
        return getDescription().replace("$1", Math.round((bonus - 1D) * 100D) + "");
    }
}
