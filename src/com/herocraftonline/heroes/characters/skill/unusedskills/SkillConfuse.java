package com.herocraftonline.heroes.characters.skill.unusedskills;

import java.util.Random;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillConfuse extends TargettedSkill {

    private static final Random random = new Random();

    private String applyText;
    private String expireText;

    public SkillConfuse(Heroes plugin) {
        super(plugin, "Confuse");
        setDescription("You confuse the target for $1 seconds.");
        setUsage("/skill confuse");
        setArgumentRange(0, 0);
        setIdentifiers("skill confuse");
        setTypes(SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.AGGRESSIVE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.PERIOD.node(), 1000);
        node.set("max-drift", 0.35);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% is confused!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has regained his wit!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%target% is confused!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "%target% has regained his wit!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, true);
        float maxDrift = (float) SkillConfigManager.getUseSetting(hero, this, "max-drift", 0.35, false);
        plugin.getCharacterManager().getCharacter(target).addEffect(new ConfuseEffect(this, hero.getPlayer(), duration, period, maxDrift));
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public class ConfuseEffect extends PeriodicExpirableEffect {

        private final float maxDrift;

        public ConfuseEffect(Skill skill, Player applier, long duration, long period, float maxDrift) {
            super(skill, "Confuse", applier, period, duration);
            this.maxDrift = maxDrift;
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.MAGIC);
            addMobEffect(9, (int) (duration / 1000) * 20, 127, false);
        }

        public void adjustVelocity(LivingEntity lEntity) {
            Vector velocity = lEntity.getVelocity();

            float angle = random.nextFloat() * 2 * 3.14159f;
            float xAdjustment = maxDrift * (float) Math.cos(angle);
            float zAdjustment = maxDrift * (float) Math.sin(angle);

            velocity.add(new Vector(xAdjustment, 0f, zAdjustment));
            velocity.setY(0);
            lEntity.setVelocity(velocity);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, Messaging.getLivingEntityName(monster));
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

        @Override
        public void tickMonster(Monster monster) {
            adjustVelocity(monster.getEntity());
            if (monster instanceof Creature) {
                ((Creature) monster).setTarget(null);
            }
        }

        @Override
        public void tickHero(Hero hero) {
            adjustVelocity(hero.getPlayer());
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
