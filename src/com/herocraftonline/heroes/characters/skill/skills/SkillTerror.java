package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillTerror extends TargettedSkill {
	
	private String applyText;
	private String expireText;

	public SkillTerror(Heroes plugin) {
		super(plugin, "Terror");
		setDescription("You terrify your target, impairing their movment and vision for $1 seconds");
        setUsage("/skill terror");
        setArgumentRange(0, 0);
        setIdentifiers("skill terror");
        setTypes(SkillType.DARK, SkillType.DEBUFF, SkillType.ILLUSION, SkillType.MOVEMENT, SkillType.HARMFUL, SkillType.SILENCABLE);
	}
	
	@Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% is terrified!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has overcome his fear!");
        node.set("zoom-multiplier", 5);
        node.set("use-darkness", true);
        node.set("use-nausea", true);
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%target% is terrified!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "%target% has overcome his fear!").replace("%target%", "$1");
    }

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] args) {
		if(!(target instanceof Player))
			return SkillResult.INVALID_TARGET_NO_MSG;
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
		int zoomAmount = SkillConfigManager.getUseSetting(hero, this, "zoom-multiplier", 5, false);
		boolean useDarkness = SkillConfigManager.getUseSetting(hero, this, "use-darkness", true);
		boolean useNausea = SkillConfigManager.getUseSetting(hero, this, "use-nausea", true);
		Hero targetHero = plugin.getCharacterManager().getHero((Player)target);
		targetHero.addEffect(new TerrorEffect(this, duration, zoomAmount, useDarkness, useNausea, applyText, expireText));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENDERMAN_STARE , 0.8F, 1.0F); 
		return SkillResult.NORMAL;
	}
	
	public class TerrorEffect extends ExpirableEffect {
		
		private final String applyText;
		private final String expireText;
		
		public TerrorEffect(Skill skill, long duration, int zoomAmount, boolean useDarkness, boolean useNausea, String applyText, String expireText) {
			super(skill, "Terror", duration);
			this.applyText = applyText;
			this.expireText = expireText;

            types.add(EffectType.DARK);
            types.add(EffectType.SLOW);
            types.add(EffectType.DISABLE);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);

			if(zoomAmount > 0)
				addMobEffect(2, (int) (duration / 1000) * 20, zoomAmount, false);
			if(useDarkness)
				addMobEffect(15, (int) ((duration / 1000) * 20), 3, false);
			if(useNausea)
				addMobEffect(9, (int) (duration / 1000) * 20, 127, false);
		}
		
		@Override
		public void applyToHero(Hero hero) {
			super.applyToHero(hero);
			Player player = hero.getPlayer();
			this.broadcast(player.getLocation(), applyText, player.getName());
		}
		
		public void removeFromHero(Hero hero) {
			super.removeFromHero(hero);
			Player player = hero.getPlayer();
			this.broadcast(player.getLocation(), expireText, player.getName());
		}
		
	}

	@Override
	public String getDescription(Hero hero) {
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false) / 1000;
		return getDescription().replace("$1", duration + "");
	}

}
