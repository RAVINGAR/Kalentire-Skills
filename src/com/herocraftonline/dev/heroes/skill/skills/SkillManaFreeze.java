package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.HeroRegainManaEvent;
import com.herocraftonline.dev.heroes.api.HeroesEventListener;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.ExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillManaFreeze extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillManaFreeze(Heroes plugin) {
        super(plugin, "ManaFreeze");
        setDescription("Prevents your target from regenerating mana for $1 seconds.");
        setUsage("/skill manafreeze");
        setArgumentRange(0, 1);
        setIdentifiers("skill manafreeze", "skill mfreeze");
        setTypes(SkillType.SILENCABLE, SkillType.DEBUFF, SkillType.MANA, SkillType.HARMFUL);

        registerEvent(Type.CUSTOM_EVENT, new HeroListener(), Priority.Highest);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000);
        node.set(Setting.APPLY_TEXT.node(), "%target% has stopped regenerating mana!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% is once again regenerating mana!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT.node(), "%target% has stopped regenerating mana!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT.node(), "%target% is once again regenerating mana!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player))
        	return SkillResult.INVALID_TARGET;

        broadcastExecuteText(hero, target);
        Hero targetHero = plugin.getHeroManager().getHero((Player) target);
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        targetHero.addEffect(new ManaFreezeEffect(this, duration));
        return SkillResult.NORMAL;

    }

    public class HeroListener extends HeroesEventListener {

        @Override
        public void onHeroRegainMana(HeroRegainManaEvent event) {
            Heroes.debug.startTask("HeroesSkillListener");
            if (event.isCancelled()) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }

            if (event.getHero().hasEffect("ManaFreeze")) {
                event.setCancelled(true);
            }
            Heroes.debug.stopTask("HeroesSkillListener");
        }
    }

    public class ManaFreezeEffect extends ExpirableEffect {

        public ManaFreezeEffect(Skill skill, long duration) {
            super(skill, "ManaFreeze", duration);
            this.types.add(EffectType.DISPELLABLE);
        }

        @Override
        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
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
