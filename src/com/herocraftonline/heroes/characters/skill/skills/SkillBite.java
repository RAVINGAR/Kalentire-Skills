package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillBite extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillBite(Heroes plugin) {
        super(plugin, "Bite");
        setDescription("You bite the target for $1 damage and causing them to bleed for $2 damage over $3 seconds.");
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
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%target% is bleeding from a grievous wound!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%target% has stopped bleeding!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        // Damage the target
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 10, false);
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        // Apply our effect
        long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION.node(), 15000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 3000, true);
        int tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
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

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 10, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 15000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 3000, false);
        int td = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        td = td * duration / period;
        return getDescription().replace("$1", damage + "").replace("$2", td + "").replace("$3", duration / 1000 + "");
    }
}