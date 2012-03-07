package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillDecay extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillDecay(Heroes plugin) {
        super(plugin, "Decay");
        setDescription("You disease your target dealing $1 dark damage over $2 seconds.");
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
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%target%'s flesh has begun to rot!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%target% is no longer decaying alive!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 21000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 3000, true);
        int tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        plugin.getCharacterManager().getCharacter(target).addEffect(new DecayEffect(this, duration, period, tickDamage, player));
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
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), applyText, Messaging.getLivingEntityName(monster));
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), expireText, Messaging.getLivingEntityName(monster).toLowerCase());
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
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 21000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 3000, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        damage = damage * duration / period;
        return getDescription().replace("$1", damage + "").replace("$2", duration / 1000 + "");
    }
}
