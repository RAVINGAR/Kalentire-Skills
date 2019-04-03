package com.herocraftonline.heroes.characters.skill.reborn.enderbeast;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass;
import com.herocraftonline.heroes.characters.classes.scaling.Scaling;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.effects.common.interfaces.WalkSpeedDecrease;
import com.herocraftonline.heroes.characters.effects.common.interfaces.WalkSpeedPercentDecrease;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillDeepWound extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillDeepWound(Heroes plugin) {
        super(plugin, "DeepWound");
        setDescription("You inflict a deep wound on your target, slowing them by $1% and causing them to bleed for $2 damage over $3 second(s).");
        setUsage("/skill deepwound");
        setArgumentRange(0, 0);
        setIdentifiers("skill deepwound", "skill dwound");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        double reductionPercent = 1.0 - SkillConfigManager.getUseSetting(hero, this, "movespeed-reduction-percent", 0.2, true);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        double period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);

        return getDescription()
                .replace("$1", reductionPercent * 100 + "")
                .replace("$2", Util.decFormat.format(damage * ((double) duration / period)))
                .replace("$3", Util.decFormat.format((double) duration / 1000));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("weapons", Util.axes);
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.PERIOD.node(), 2000);
        node.set("tick-damage", 10);
        node.set("movespeed-reduction-percent", 0.2);
        node.set(SkillSetting.MAX_DISTANCE.node(), 2);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% has been deeply wounded by %hero%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has recovered from their deep wound!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% has been deeply wounded by %hero%!").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has recovered from their deep wound.").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        HeroClass heroClass = hero.getHeroClass();

        Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.axes).contains(item.name())) {
            player.sendMessage("You can't use Deep Wound with that weapon!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Scaling itemDamage = heroClass.getItemDamage(item);
        double damage = itemDamage == null ? 0 : itemDamage.getScaled(hero);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, true);
        double tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 10, false);
        double moveSpeedReductionPercent = SkillConfigManager.getUseSetting(hero, this, "movespeed-reduction-percent", 0.2, true);
        plugin.getCharacterManager().getCharacter(target).addEffect(new DeepWoundEffect(this, player, period, duration, tickDamage, moveSpeedReductionPercent));

        player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 0.5, 0), 25, 0, 0, 0, 1);
        player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.MOBSPAWNER_FLAMES, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_PLAYER_HURT , 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class DeepWoundEffect extends PeriodicDamageEffect implements WalkSpeedPercentDecrease {

        private double decreasePercent;

        DeepWoundEffect(Skill skill, Player applier, long period, long duration, double tickDamage, double decreasePercent) {
            super(skill, applier.getName() + "-DeepWound", applier, period, duration, tickDamage, applyText, expireText);
            this.decreasePercent = decreasePercent;

            types.add(EffectType.BLEED);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.WALK_SPEED_DECREASING);
        }

        @Override
        public Double getDelta() {
            return decreasePercent;
        }

        @Override
        public void setDelta(Double delta) {
            this.decreasePercent = delta;
        }
    }
}
