package com.herocraftonline.heroes.characters.skill.unusedskills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillChlorobon extends TargettedSkill {

    public final VisualEffect fplayer = new VisualEffect();
    private String expireText;
    private String applyText;

    public SkillChlorobon(final Heroes plugin) {
        super(plugin, "Chlorobon");
        setDescription("You restore $1 health to the target over $2 second(s).");
        setUsage("/skill chlorobon <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill chlorobon");
        setTypes(SkillType.BUFFING, SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(final Hero hero) {
        int heal = SkillConfigManager.getUseSetting(hero, this, "tick-heal", 1, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 21000, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, false);

        heal = heal * duration / period;

        return getDescription().replace("$1", heal + "").replace("$2", duration / 1000 + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("tick-heal", 71);
        node.set(SkillSetting.PERIOD.node(), 3000);
        node.set(SkillSetting.DURATION.node(), 12000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% has been given the gift of Chlorobon!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has lost the gift of Chlorobon.");
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% has been given the gift of Chlorobon!").replace("%target%", "$1").replace("$target$", "$1");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has lost the gift of Chlorobon.").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final Player player = hero.getPlayer();
        if ((target instanceof Player)) {
            final Hero targetHero = this.plugin.getCharacterManager().getHero((Player) target);

            if (target.getHealth() >= target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
                player.sendMessage("Target is already fully healed.");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }

            final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, true);
            final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 12000, false);
            final int tickHealth = SkillConfigManager.getUseSetting(hero, this, "tick-heal", 71, false);
            final ChlorobonEffect cbEffect = new ChlorobonEffect(this, period, duration, tickHealth, player);
            targetHero.addEffect(cbEffect);

            try {
                this.fplayer.playFirework(player.getWorld(), target.getLocation().add(0.0D, 1.5D, 0.0D), FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.BALL_LARGE).withColor(Color.OLIVE).withFade(Color.NAVY).build());
            } catch (final Exception e) {
                e.printStackTrace();
            }

            return SkillResult.NORMAL;
        }

        return SkillResult.INVALID_TARGET;
    }

    public class ChlorobonEffect extends PeriodicHealEffect {
        public ChlorobonEffect(final Skill skill, final long period, final long duration, final double tickHealth, final Player applier) {
            super(skill, "ChlorobonEffect", applier, period, duration, tickHealth);
            this.types.add(EffectType.DISPELLABLE);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), SkillChlorobon.this.applyText, player.getName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), SkillChlorobon.this.expireText, player.getName());
        }
    }
}
