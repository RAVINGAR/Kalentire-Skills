package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.PeriodicDamageEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillDecay extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillDecay(Heroes plugin) {
        super(plugin, "Decay");
        setDescription("Causes your target's flesh to decay rapidly");
        setUsage("/skill decay <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill decay");
        setTypes(SkillType.DARK, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 21000);
        node.set(Setting.PERIOD.node(), 3000);
        node.set("tick-damage", 1);
        node.set(Setting.APPLY_TEXT.node(), "%target%'s flesh has begun to rot!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% is no longer decaying alive!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = getSetting(null, Setting.APPLY_TEXT.node(), "%target%'s flesh has begun to rot!").replace("%target%", "$1");
        expireText = getSetting(null, Setting.EXPIRE_TEXT.node(), "%target% is no longer decaying alive!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        long duration = getSetting(hero, Setting.DURATION.node(), 21000, false);
        long period = getSetting(hero, Setting.PERIOD.node(), 3000, true);
        int tickDamage = getSetting(hero, "tick-damage", 1, false);
        DecayEffect decayEffect = new DecayEffect(this, duration, period, tickDamage, player);

        if (target instanceof Player) {
            plugin.getHeroManager().getHero((Player) target).addEffect(decayEffect);
        } else if (target instanceof LivingEntity) {
            plugin.getEffectManager().addEntityEffect(target, decayEffect);
        } else
        	return SkillResult.INVALID_TARGET;

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public class DecayEffect extends PeriodicDamageEffect {

        public DecayEffect(Skill skill, long duration, long period, int tickDamage, Player applier) {
            super(skill, "Decay", period, duration, tickDamage, applier);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.DISEASE);
        }

        @Override
        public void apply(LivingEntity lEntity) {
            super.apply(lEntity);
            broadcast(lEntity.getLocation(), applyText, Messaging.getLivingEntityName(lEntity));
        }

        @Override
        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void remove(LivingEntity lEntity) {
            super.remove(lEntity);
            broadcast(lEntity.getLocation(), expireText, Messaging.getLivingEntityName(lEntity).toLowerCase());
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }
}
