package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillFullHeal extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    public SkillFullHeal(Heroes plugin) {
        super(plugin, "FullHeal");
        setDescription("You restore your target to full health.");
        setUsage("/skill fullheal <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill fullheal");
        setTypes(SkillType.LIGHT, SkillType.HEAL, SkillType.SILENCABLE);
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        int healAmount = (int) Math.ceil(target.getMaxHealth() - target.getHealth());
        HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, healAmount, this, hero);
        plugin.getServer().getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            Messaging.send(hero.getPlayer(), "Unable to heal the target at this time!");
            return SkillResult.CANCELLED;
        }
        targetHero.heal(hrhEvent.getAmount());
        //this should be the new heal for Bukkit Damage/Health
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.LEVEL_UP , 0.9F, 1.0F);
        broadcastExecuteText(hero, target);
        // this is our fireworks shit
        Player player = hero.getPlayer();
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0,1.5,0), FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BALL).withColor(Color.FUCHSIA).withFade(Color.WHITE).build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
