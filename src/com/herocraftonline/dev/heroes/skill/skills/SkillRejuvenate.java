package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.PeriodicHealEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillRejuvenate extends TargettedSkill {

    private String expireText;
    private String applyText;

    public SkillRejuvenate(Heroes plugin) {
        super(plugin, "Rejuvenate");
        setDescription("You restore $1 health to the target over $2 seconds.");
        setUsage("/skill rejuvenate <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill rejuvenate", "skill rejuv");
        setTypes(SkillType.BUFF, SkillType.HEAL, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("tick-heal", 1);
        node.set(Setting.PERIOD.node(), 3000);
        node.set(Setting.DURATION.node(), 21000);
        node.set(Setting.APPLY_TEXT.node(), "%target% is rejuvenating health!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% has stopped rejuvenating health!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%target% is rejuvenating health!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%target% has stopped rejuvenating health!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (target instanceof Player) {
            Hero targetHero = plugin.getHeroManager().getHero((Player) target);

            if (targetHero.getHealth() >= targetHero.getMaxHealth()) {
                Messaging.send(player, "Target is already fully healed.");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }

            long period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 3000, true);
            long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 21000, false);
            int tickHealth = SkillConfigManager.getUseSetting(hero, this, "tick-heal", 1, false);
            RejuvenateEffect rEffect = new RejuvenateEffect(this, period, duration, tickHealth, player);
            targetHero.addEffect(rEffect);
            return SkillResult.NORMAL;
        }

        return SkillResult.INVALID_TARGET;
    }

    public class RejuvenateEffect extends PeriodicHealEffect {

        public RejuvenateEffect(Skill skill, long period, long duration, int tickHealth, Player applier) {
            super(skill, "Rejuvenate", period, duration, tickHealth, applier);
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
        int heal = SkillConfigManager.getUseSetting(hero, this, "tick-heal", 1, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 21000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 3000, false);
        heal = heal * duration / period;
        return getDescription().replace("$1", heal + "").replace("$2", duration / 1000 + "");
    }
    
}
