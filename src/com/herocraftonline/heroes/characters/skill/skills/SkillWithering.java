package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillWithering extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillWithering(Heroes plugin) {
        super(plugin, "Withering");
        setDescription("You wither your target in darkness, causing nausea for $1 damage over $2 seconds and $3 damage as a finisher");
        setUsage("/skill withering");
        setArgumentRange(0, 0);
        setIdentifiers("skill withering");
        setTypes(SkillType.DARK, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 6000);
        node.set(SkillSetting.PERIOD.node(), 2000);
        node.set("tick-damage", 2);
        node.set("finish-damage", 20);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target%'s begins to wither away!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target%'s is no longer withered!");

        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target%'s begins to wither away!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target%'s is no longer withered!").replace("%target%", "$1");
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, true);
        int tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 2, false);
        int finishDamage = SkillConfigManager.getUseSetting(hero, this, "finish-damage", 15, false);

        tickDamage *= duration / period;
        return getDescription().replace("$1", tickDamage + "").replace("$2", duration / 1000 + "").replace("$3", finishDamage + "");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, true);
        int tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 2, false);
        int finishDamage = SkillConfigManager.getUseSetting(hero, this, "finish-damage", 15, false);
        plugin.getCharacterManager().getCharacter(target).addEffect(new WitheringEffect(this, duration, period, tickDamage, finishDamage, player));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_SHOOT, 0.5F, 1.0F);

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public class WitheringEffect extends PeriodicDamageEffect {

        private double finishDamage;

        public WitheringEffect(Skill skill, long duration, long period, double tickDamage, double finishDamage, Player applier) {
            super(skill, "Withering", period, duration, tickDamage, applier);

            this.finishDamage = finishDamage;

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.DARK);
            types.add(EffectType.WITHER);
            types.add(EffectType.HARMFUL);

            addMobEffect(9, (int) ((duration + 4000) / 1000) * 20, 3, false);
            addMobEffect(20, (int) (duration / 1000) * 20, 1, false);
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

            if (monster.getEntity().isDead())
                return;

            skill.addSpellTarget(monster.getEntity(), getApplierHero());
            damageEntity(monster.getEntity(), getApplier(), finishDamage, DamageCause.MAGIC);

            broadcast(monster.getEntity().getLocation(), expireText, Messaging.getLivingEntityName(monster));
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            if (player.isDead())
                return;

            skill.addSpellTarget(hero.getEntity(), getApplierHero());
            damageEntity(player, getApplier(), finishDamage, DamageCause.MAGIC);

            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }
}
