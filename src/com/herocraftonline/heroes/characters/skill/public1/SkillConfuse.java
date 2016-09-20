package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Random;

public class SkillConfuse extends TargettedSkill {

    private static final Random random = new Random();

    private String applyText;
    private String expireText;

    public SkillConfuse(Heroes plugin) {
        super(plugin, "Confuse");
        this.setDescription("You confuse the target for $1 seconds.");
        this.setUsage("/skill confuse <target>");
        this.setArgumentRange(0, 1);
        this.setIdentifiers("skill confuse");
        this.setTypes(SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.AGGRESSIVE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
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
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%target% is confused!").replace("%target%", "$1");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "%target% has regained his wit!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, true);
        final float maxDrift = (float) SkillConfigManager.getUseSetting(hero, this, "max-drift", 0.35, false);
        this.plugin.getCharacterManager().getCharacter(target).addEffect(new ConfuseEffect(this, hero.getPlayer(), duration, period, maxDrift));
        this.broadcastExecuteText(hero, target);
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
            this.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (int) (20 * duration / 1000), 127), false);
        }

        public void adjustVelocity(LivingEntity lEntity) {
            final Vector velocity = lEntity.getVelocity();

            double angle = random.nextDouble() * 2 * Math.PI;
            final float xAdjustment = (float) (this.maxDrift * Math.cos(angle));
            final float zAdjustment = (float) (this.maxDrift * Math.sin(angle));

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
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), SkillConfuse.this.applyText, player.getDisplayName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            this.broadcast(monster.getEntity().getLocation(), SkillConfuse.this.expireText, CustomNameManager.getName(monster));
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), SkillConfuse.this.expireText, player.getDisplayName());
        }

        @Override
        public void tickMonster(Monster monster) {
            this.adjustVelocity(monster.getEntity());
            if (monster instanceof Creature) {
                ((Creature) monster).setTarget(null);
            }
        }

        @Override
        public void tickHero(Hero hero) {
            this.adjustVelocity(hero.getPlayer());
        }
    }

    @Override
    public String getDescription(Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        return this.getDescription().replace("$1", (duration / 1000) + "");
    }
}
