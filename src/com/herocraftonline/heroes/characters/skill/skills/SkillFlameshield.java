package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillFlameshield extends ActiveSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    private String applyText;
    private String expireText;

    public SkillFlameshield(Heroes plugin) {
        super(plugin, "Flameshield");
        setDescription("You become resistent to fire for $1 seconds.");
        setUsage("/skill flameshield");
        setArgumentRange(0, 0);
        setIdentifiers("skill flameshield", "skill fshield");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCABLE, SkillType.BUFFING);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 8000);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% conjured a shield of flames!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% lost his shield of flames!");
        node.set("skill-block-text", Messaging.getSkillDenoter() + "%name%'s flameshield has blocked %hero%'s %skill%.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% conjured a shield of flames!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% lost his shield of flames!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        // this is our fireworks shit
        Player player = hero.getPlayer();
        try {
            fplayer.playFirework(player.getWorld(), 
            		player.getLocation().add(0,2,0), 
            		FireworkEffect.builder().flicker(false).trail(false)
            		.with(FireworkEffect.Type.CREEPER)
            		.withColor(Color.RED)
            		.withFade(Color.MAROON)
            		.build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        hero.addEffect(new FlameshieldEffect(this, duration));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ZOMBIE_UNFECT , 0.4F, 1.0F); 
        return SkillResult.NORMAL;
    }

    public class FlameshieldEffect extends ExpirableEffect {

        public FlameshieldEffect(Skill skill, long duration) {
            super(skill, "Flameshield", duration);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.RESIST_FIRE);
            types.add(EffectType.MAGIC);

            addMobEffect(12, (int) ((duration * 20) / 1000), 1, false);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }
}
