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
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillManaburn extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();    
    public SkillManaburn(Heroes plugin) {
        super(plugin, "Manaburn");
        setDescription("Removes $1 mana from the target and gives it to you.");
        setUsage("/skill manaburn");
        setArgumentRange(0, 1);
        setIdentifiers("skill manaburn", "skill mburn");
        setTypes(SkillType.DAMAGING, SkillType.SILENCABLE, SkillType.MANA, SkillType.HARMFUL);
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
        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;

        Hero tHero = plugin.getCharacterManager().getHero((Player) target);

        int transferamount = SkillConfigManager.getUseSetting(hero, this, "transfer-amount", 20, false);
        if (tHero.getMana() > transferamount) {
            if (hero.getMana() + transferamount > hero.getMaxMana()) {
                transferamount = hero.getMaxMana() - hero.getMana();
            }
            tHero.setMana(tHero.getMana() - transferamount);
            broadcastExecuteText(hero, target);
            // this is our fireworks shit
            try {
                fplayer.playFirework(player.getWorld(), target.getLocation(), FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BALL_LARGE).withColor(Color.BLUE).withFade(Color.AQUA).build());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            player.getWorld().playEffect(player.getLocation(), Effect.EXTINGUISH, 3);
            hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.CAT_HISS , 10.0F, 1.0F); 
            return SkillResult.NORMAL;
        } else {
            Messaging.send(player, "Target does not have enough mana!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int mana = SkillConfigManager.getUseSetting(hero, this, "transfer-amount", 20, false);
        return getDescription().replace("$1", mana + "");
    }
}
