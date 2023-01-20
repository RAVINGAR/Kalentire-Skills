package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillRejuvenate extends TargettedSkill {
    private String expireText;
    private String applyText;

    public SkillRejuvenate(final Heroes plugin) {
        super(plugin, "Rejuvenate");
        setDescription("You restore $1 health to the target over $2 second(s). You are only healed for $3 health from this effect.");
        setUsage("/skill rejuvenate <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill rejuvenate");
        setTypes(SkillType.BUFFING, SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 20000, false);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 10, false);
        healing = getScaledHealing(hero, healing);
        final double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.25, false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

        final String formattedHealing = Util.decFormat.format(healing * ((double) duration / (double) period));
        final String formattedSelfHealing = Util.decFormat.format((healing * ((double) duration / (double) period)) * Heroes.properties.selfHeal);
        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedHealing).replace("$2", formattedDuration).replace("$3", formattedSelfHealing);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.DURATION.node(), 20000);
        node.set(SkillSetting.PERIOD.node(), 2000);
        node.set(SkillSetting.HEALING_TICK.node(), 10);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.25);

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% is rejuvenating health!").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% has stopped rejuvenating health!").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        final Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

        if (target.getHealth() >= target.getMaxHealth()) {
            player.sendMessage("Target is already fully healed.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 10, false);
        healing = getScaledHealing(hero, healing);
        final double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.25, false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 20000, false);

        final RejuvenateEffect rEffect = new RejuvenateEffect(this, player, period, duration, healing);
        targetHero.addEffect(rEffect);

        return SkillResult.NORMAL;
    }

    public class RejuvenateEffect extends PeriodicHealEffect {

        public RejuvenateEffect(final Skill skill, final Player applier, final long period, final long duration, final double tickHealth) {
            super(skill, "Rejuvenate", applier, period, duration, tickHealth);

            types.add(EffectType.MAGIC);
            types.add(EffectType.DISPELLABLE);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

        @Override
        public void tickHero(final Hero hero) {
            super.tickHero(hero);
            final Player player = hero.getPlayer();
            //player.getWorld().spigot().playEffect(player.getLocation(), Effect.HAPPY_VILLAGER, 0, 0, 0.5F, 1.0F, 0.5F, 0.1F, 25, 16);
            player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation(), 25, 0.5, 1, 0.5, 0.1);
        }
    }
}
