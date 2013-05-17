package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillBattery extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    public SkillBattery(Heroes plugin) {
        super(plugin, "Battery");
        setDescription("You grant $1 of your mana to your target.");
        setUsage("/skill battery");
        setArgumentRange(0, 1);
        setTypes(SkillType.SILENCABLE, SkillType.MANA, SkillType.DARK);
        setIdentifiers("skill battery");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("transfer-amount", 20);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
    	Player player = hero.getPlayer();
        if (!(target instanceof Player) || player.equals(target))
        	return SkillResult.INVALID_TARGET;

        Hero tHero = plugin.getCharacterManager().getHero((Player) target);	

        int transferAmount = SkillConfigManager.getUseSetting(hero, this, "transfer-amount", 20, false);
        if (hero.getMana() > transferAmount) {
            if (tHero.getMana() + transferAmount > tHero.getMaxMana()) {
                transferAmount = tHero.getMaxMana() - tHero.getMana();
            }
            hero.setMana(hero.getMana() - transferAmount);
            tHero.setMana(tHero.getMana() + transferAmount);
            player.getWorld().playEffect(player.getLocation(), Effect.EXTINGUISH, 3);
            hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ORB_PICKUP , 0.5F, 1.0F); 
            broadcastExecuteText(hero, target);
            // this is our fireworks shit
            try {
                fplayer.playFirework(player.getWorld(), 
                		target.getLocation().add(0,1.5,0), 
                		FireworkEffect.builder()
                		.flicker(false)
                		.trail(false)
                		.with(FireworkEffect.Type.BURST)
                		.withColor(Color.SILVER)
                		.withFade(Color.NAVY)
                		.build());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return SkillResult.NORMAL;
        } else {
            Messaging.send(hero.getPlayer(), "You need at least $1 mana to transfer.", transferAmount);
            return new SkillResult(ResultType.LOW_MANA, false);
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int amount = SkillConfigManager.getUseSetting(hero, this, "transfer-amount", 20, false);
        return getDescription().replace("$1", amount + "");
    }

}
