package com.herocraftonline.heroes.characters.skill.skills;

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
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillFlameshield extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillFlameshield(Heroes plugin) {
        super(plugin, "Flameshield");
        setDescription("You become resistent to fire for $1 seconds.");
        setUsage("/skill flameshield");
        setArgumentRange(0, 0);
        setIdentifiers("skill flameshield", "skill fshield");
        setTypes(SkillType.FIRE, SkillType.SILENCABLE, SkillType.BUFF);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000);
        node.set(Setting.APPLY_TEXT.node(), "%hero% conjured a shield of flames!");
        node.set(Setting.EXPIRE_TEXT.node(), "%hero% lost his shield of flames!");
        node.set("skill-block-text", "%name%'s flameshield has blocked %hero%'s %skill%.");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%hero% conjured a shield of flames!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%hero% lost his shield of flames!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        hero.addEffect(new FlameshieldEffect(this, duration));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ZOMBIE_UNFECT , 10.0F, 1.0F); 
        return SkillResult.NORMAL;
    }

    public class FlameshieldEffect extends ExpirableEffect {

        public FlameshieldEffect(Skill skill, long duration) {
            super(skill, "Flameshield", duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.RESIST_FIRE);
            this.types.add(EffectType.MAGIC);
            this.addMobEffect(12, (int) ((duration * 20) / 1000), 1, false);
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

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
