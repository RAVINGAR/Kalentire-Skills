package com.herocraftonline.heroes.characters.skill.skills;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillBeguile extends TargettedSkill {

    private static final Random random = new Random();

    private String applyText;
    private String expireText;

    private boolean ncpEnabled = false;

    public SkillBeguile(Heroes plugin) {
        super(plugin, "Beguile");
        setDescription("You beguile the target for $1 seconds.");
        setUsage("/skill beguile");
        setArgumentRange(0, 0);
        setIdentifiers("skill beguile");
        setTypes(SkillType.SILENCABLE, SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.AGGRESSIVE);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 125, false);
        duration += durationIncrease;

        String formattedDuration = Util.decFormat.format((double) duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.PERIOD.node(), 1000);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 125);
        node.set("max-drift", 0.35);
        node.set("ncp-exemption-duration", Integer.valueOf(500));
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% is beguiled!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has regained his wit!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%target% is beguiled!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "%target% has regained his wit!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 125, false);
        duration += durationIncrease;

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, true);

        float maxDrift = (float) SkillConfigManager.getUseSetting(hero, this, "max-drift", 0.35, false);

        broadcastExecuteText(hero, target);

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        targetCT.addEffect(new ConfuseEffect(this, duration, period, maxDrift));

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ZOMBIE_UNFECT, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class ConfuseEffect extends PeriodicExpirableEffect {

        private final float maxDrift;

        public ConfuseEffect(Skill skill, long duration, long period, float maxDrift) {
            super(skill, "Beguile", period, duration);

            this.maxDrift = maxDrift;
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.MAGIC);

            addMobEffect(9, (int) (duration / 1000) * 20, 127, false);
        }

        public void adjustVelocity(LivingEntity lEntity) {
            Vector velocity = lEntity.getVelocity();

            float angle = random.nextFloat() * 2 * 3.14159f;
            float xAdjustment = (float) (maxDrift * Math.cos(angle));
            float zAdjustment = (float) (maxDrift * Math.sin(angle));

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
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), expireText, Messaging.getLivingEntityName(monster));
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
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
            Player player = hero.getPlayer();

            // Let's bypass the nocheat issues...
            if (ncpEnabled) {
                if (!player.isOp()) {
                    long duration = SkillConfigManager.getUseSetting(hero, skill, "ncp-exemption-duration", 500, false);
                    if (duration > 0) {
                        NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(skill, duration);
                        hero.addEffect(ncpExemptEffect);
                    }
                }
            }

            adjustVelocity(hero.getPlayer());
        }
    }

    private class NCPExemptionEffect extends ExpirableEffect {

        public NCPExemptionEffect(Skill skill, long duration) {
            super(skill, "NCPExemptionEffect_MOVING", duration);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.MOVING);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.MOVING);

        }
    }
}
