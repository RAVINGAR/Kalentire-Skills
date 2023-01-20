package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass;
import com.herocraftonline.heroes.characters.classes.scaling.Scaling;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.effects.common.interfaces.WalkSpeedPercentDecrease;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillDeepWound extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillDeepWound(final Heroes plugin) {
        super(plugin, "DeepWound");
        setDescription("You inflict a deep wound on your target, slowing them by $1% and causing them to bleed for $2 damage over $3 second(s).");
        setUsage("/skill deepwound");
        setArgumentRange(0, 0);
        setIdentifiers("skill deepwound", "skill dwound");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double reductionPercent = 1.0 - SkillConfigManager.getUseSetting(hero, this, "movespeed-reduction-percent", 0.2, true);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        final double period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        final double damage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);

        return getDescription()
                .replace("$1", reductionPercent * 100 + "")
                .replace("$2", Util.decFormat.format(damage * ((double) duration / period)))
                .replace("$3", Util.decFormat.format((double) duration / 1000));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("weapons", Util.axes);
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.PERIOD.node(), 2000);
        node.set("tick-damage", 10);
        node.set("movespeed-reduction-percent", 0.2);
        node.set(SkillSetting.MAX_DISTANCE.node(), 2);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been deeply wounded by %hero%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has recovered from their deep wound!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                        ChatComponents.GENERIC_SKILL + "%target% has been deeply wounded by %hero%!")
                .replace("%target%", "$1").replace("$target$", "$1")
                .replace("%hero%", "$2").replace("$hero$", "$2");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                        ChatComponents.GENERIC_SKILL + "%target% has recovered from their deep wound.")
                .replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final Player player = hero.getPlayer();
        final HeroClass heroClass = hero.getHeroClass();

        final Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.axes).contains(item.name())) {
            player.sendMessage("You can't use Deep Wound with that weapon!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        final Scaling itemDamage = heroClass.getItemDamage(item);
        final double damage = itemDamage == null ? 0 : itemDamage.getScaled(hero);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, true);
        final double tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 10, false);
        final double moveSpeedReductionPercent = SkillConfigManager.getUseSetting(hero, this, "movespeed-reduction-percent", 0.2, true);
        plugin.getCharacterManager().getCharacter(target).addEffect(new DeepWoundEffect(this, player, period, duration, tickDamage, moveSpeedReductionPercent));

        player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 0.5, 0), 25, 0, 0, 0, 1);
        player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.MOBSPAWNER_FLAMES, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class DeepWoundEffect extends PeriodicDamageEffect implements WalkSpeedPercentDecrease {

        private double decreasePercent;

        DeepWoundEffect(final Skill skill, final Player applier, final long period, final long duration, final double tickDamage, final double decreasePercent) {
            super(skill, applier.getName() + "-DeepWound", applier, period, duration, tickDamage, applyText, expireText);
            this.decreasePercent = decreasePercent;

            types.add(EffectType.BLEED);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.WALK_SPEED_DECREASING);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> syncTask(hero), 1L);
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> syncTask(hero), 1L);
        }

        private void syncTask(final Hero hero) {
            hero.resolveMovementSpeed();
        }

        @Override
        public Double getDelta() {
            return decreasePercent;
        }

        @Override
        public void setDelta(final Double delta) {
            this.decreasePercent = delta;
        }
    }
}
