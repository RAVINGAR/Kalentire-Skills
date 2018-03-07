package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;

public class SkillIceblade extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();    
    private String applyText;
    private String expireText;
    
    
    public SkillIceblade(Heroes plugin) {
        super(plugin, "Iceblade");
        setDescription("You freeze your target with your weapon, damaging and slowing them for $1 seconds.");
        setUsage("/skill iceblade");
        setArgumentRange(0, 0);
        setIdentifiers("skill iceblade");
        setTypes(SkillType.ABILITY_PROPERTY_ICE, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.FORCE, SkillType.INTERRUPTING);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("weapons", Util.swords);
        node.set(SkillSetting.MAX_DISTANCE.node(), 6);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set("amplitude", 2);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% has been slowed by %hero%'s iceblade!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% is no longer slowed!");
        return node;
    }
    
    @Override
    public void init() {
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% has been slowed by %hero%'s iceblade!").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% is no longer slowed!").replace("%target%", "$1");
    }
    
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords).contains(item.name())) {
            player.sendMessage("You can't use iceblade with that weapon!");
            return SkillResult.FAIL;
        }
        
        double damage = plugin.getDamageManager().getHighestItemDamage(hero, item);
        plugin.getDamageManager().addSpellTarget(target, hero, this);
        damageEntity(target, player, damage, DamageCause.MAGIC);
        //Add the slow effect
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        int amplitude = SkillConfigManager.getUseSetting(hero, this, "amplitude", 4, false);
        SlowEffect sEffect = new SlowEffect(this, hero.getPlayer(), duration, amplitude, applyText, expireText);
        plugin.getCharacterManager().getCharacter(target).addEffect(new IcebladeEffect(this, hero.getPlayer(), 300, sEffect));
        broadcastExecuteText(hero, target);
        // this is our fireworks shit
        /*try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0,1.5,0), 
            		FireworkEffect.builder().flicker(false).trail(false)
            		.with(FireworkEffect.Type.BALL)
            		.withColor(Color.AQUA)
            		.withFade(Color.NAVY)
            		.build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), CompatSound.ENTITY_PLAYER_HURT.value() , 0.8F, 1.0F);
        return SkillResult.NORMAL;
    }
    
    public class IcebladeEffect extends ExpirableEffect {

    	private final Effect effect;
		public IcebladeEffect(Skill skill, Player applier, long duration, Effect afterEffect) {
			super(skill, "Iceblade", applier, duration);
			this.effect = afterEffect;
			this.types.add(EffectType.HARMFUL);
			this.types.add(EffectType.ICE);
			this.types.add(EffectType.DISABLE);
			this.types.add(EffectType.SLOW);
			addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (duration / 1000) * 20, 20), false);
		}
		
		@Override
		public void removeFromHero(Hero hero) {
		    super.removeFromHero(hero);
			hero.addEffect(effect);
		}
		
		@Override
		public void removeFromMonster(Monster monster) {
		    super.removeFromMonster(monster);
		    monster.addEffect(effect);
		}
    }
    
    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
