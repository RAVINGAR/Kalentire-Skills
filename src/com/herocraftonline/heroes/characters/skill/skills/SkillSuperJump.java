package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillSuperJump extends ActiveSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    public SkillSuperJump(Heroes plugin) {
        super(plugin, "SuperJump");
        setDescription("You launch into the air, and float safely to the ground.");
        setUsage("/skill superjump");
        setArgumentRange(0, 0);
        setIdentifiers("skill superjump");
        setTypes(SkillType.MOVEMENT, SkillType.PHYSICAL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set("jump-force", 4.0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        float jumpForce = (float) SkillConfigManager.getUseSetting(hero, this, "jump-force", 1.0, false);
        Vector v1 = new Vector(0, jumpForce, 0);
        Vector v = player.getVelocity().add(v1);
        player.setVelocity(v);
        player.setFallDistance(-8f);
        int duration = (int) SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 5000, false);
        hero.addEffect(new JumpEffect(this, duration));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.EXPLODE , 0.5F, 1.0F); 
        broadcastExecuteText(hero);
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), player.getLocation().add(0,10,0), 
            		FireworkEffect.builder().flicker(false).trail(false).
            		with(FireworkEffect.Type.STAR)
            		.withColor(Color.BLUE)
            		.withFade(Color.GRAY)
            		.build());
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

    public class JumpEffect extends SafeFallEffect {

        public JumpEffect(Skill skill, int duration) {
            super(skill, "Jump", duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.PHYSICAL);

            addMobEffect(8, duration / 1000 * 20, 5, false);
        }
    }
}