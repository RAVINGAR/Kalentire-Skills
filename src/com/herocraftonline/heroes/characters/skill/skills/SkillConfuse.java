package com.herocraftonline.heroes.characters.skill.skills;

import java.util.Random;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillConfuse extends TargettedSkill {

    private static final Random random = new Random();

    private String applyText;
    private String expireText;

    public SkillConfuse(Heroes plugin) {
        super(plugin, "Confuse");
        setDescription("You confuse the target for $1 seconds.");
        setUsage("/skill confuse <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill confuse");
        setTypes(SkillType.SILENCABLE, SkillType.ILLUSION, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 10000);
        node.set(Setting.PERIOD.node(), 1000);
        node.set("max-drift", 0.35);
        node.set(Setting.APPLY_TEXT.node(), "%target% is confused!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% has regained his wit!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT.node(), "%target% is confused!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT.node(), "%target% has regained his wit!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 2000, true);
        float maxDrift = (float) SkillConfigManager.getUseSetting(hero, this, "max-drift", 0.35, false);
        if (target instanceof Player) {
            plugin.getHeroManager().getHero((Player) target).addEffect(new ConfuseEffect(this, duration, period, maxDrift));
        } else
            plugin.getEffectManager().addEntityEffect(target, new ConfuseEffect(this, duration, period, maxDrift));


        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public class ConfuseEffect extends PeriodicExpirableEffect {

        private final float maxDrift;

        public ConfuseEffect(Skill skill, long duration, long period, float maxDrift) {
            super(skill, "Confuse", period, duration);
            this.maxDrift = maxDrift;
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.DISPELLABLE);
            addMobEffect(9, (int) (duration / 1000) * 20, 127, false);
        }

        public void adjustVelocity(LivingEntity lEntity) {
            Vector velocity = lEntity.getVelocity();

            float angle = random.nextFloat() * 2 * 3.14159f;
            float xAdjustment = maxDrift * net.minecraft.server.MathHelper.cos(angle);
            float zAdjustment = maxDrift * net.minecraft.server.MathHelper.sin(angle);

            velocity.add(new Vector(xAdjustment, 0f, zAdjustment));
            velocity.setY(0);
            lEntity.setVelocity(velocity);
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
            broadcast(lEntity.getLocation(), expireText, Messaging.getLivingEntityName(lEntity));
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

        @Override
        public void tick(LivingEntity lEntity) {
            super.tick(lEntity);
            adjustVelocity(lEntity);
            if (lEntity instanceof Creature)
                ((Creature) lEntity).setTarget(null);
        }

        @Override
        public void tick(Hero hero) {
            super.tick(hero);
            adjustVelocity(hero.getPlayer());
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
