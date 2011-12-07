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

public class SkillBite extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillBite(Heroes plugin) {
        super(plugin, "Bite");
        setDescription("Deals physical damage to the target");
        setUsage("/skill bite <target>");
        setArgumentRange(0, 1);
        setTypes(SkillType.PHYSICAL, SkillType.DAMAGING, SkillType.HARMFUL);
        setIdentifiers("skill bite");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DAMAGE.node(), 10);
        node.set(Setting.MAX_DISTANCE.node(), 2);
        node.set(Setting.DURATION.node(), 15000);
        node.set(Setting.PERIOD.node(), 3000);
        node.set("tick-damage", 1);
        node.set(Setting.APPLY_TEXT.node(), "%target% is bleeding from a grievous wound!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% has stopped bleeding!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = getSetting(null, Setting.APPLY_TEXT.node(), "%target% is bleeding from a grievous wound!").replace("%target%", "$1");
        expireText = getSetting(null, Setting.EXPIRE_TEXT.node(), "%target% has stopped bleeding!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        // Damage the target
        int damage = getSetting(hero, Setting.DAMAGE.node(), 10, false);
        addSpellTarget(target, hero);
        target.damage(damage, player);

        // Apply our effect
        long duration = getSetting(hero, Setting.DURATION.node(), 15000, false);
        long period = getSetting(hero, Setting.PERIOD.node(), 3000, true);
        int tickDamage = getSetting(hero, "tick-damage", 1, false);
        BiteBleedEffect bbEffect = new BiteBleedEffect(this, period, duration, tickDamage, player);
        if (target instanceof Player) {
            plugin.getHeroManager().getHero((Player) target).addEffect(bbEffect);
        } else
            plugin.getEffectManager().addEntityEffect(target, bbEffect);

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public class BiteBleedEffect extends PeriodicDamageEffect {

        public BiteBleedEffect(Skill skill, long period, long duration, int tickDamage, Player applier) {
            super(skill, "BiteBleed", period, duration, tickDamage, applier);
            this.types.add(EffectType.BLEED);
        }

        @Override
        public void apply(LivingEntity lEntity) {
            super.apply(lEntity);
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