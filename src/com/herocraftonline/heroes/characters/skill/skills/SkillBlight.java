package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillBlight extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillBlight(Heroes plugin) {
        super(plugin, "Blight");
        setDescription("You disease your target, dealing $1 dark damage over $2 seconds, enemies that get too close will also be damaged.");
        setUsage("/skill blight <target>");
        setArgumentRange(0, 1);
        setTypes(SkillType.DARK, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
        setIdentifiers("skill blight");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 21000);
        node.set(Setting.PERIOD.node(), 3000);
        node.set("tick-damage", 1);
        node.set(Setting.RADIUS.node(), 4);
        node.set(Setting.APPLY_TEXT.node(), "%target% begins to radiate a cloud of disease!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% is no longer diseased!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%target% begins to radiate a cloud of disease!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%target% is no longer diseased!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 21000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 3000, true);
        int tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        BlightEffect bEffect = new BlightEffect(this, duration, period, tickDamage, player);

        if (target instanceof Player) {
            plugin.getHeroManager().getHero((Player) target).addEffect(bEffect);
        } else 
            plugin.getEffectManager().addEntityEffect(target, bEffect);

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public class BlightEffect extends PeriodicDamageEffect {

        public BlightEffect(Skill skill, long duration, long period, int tickDamage, Player applier) {
            super(skill, "Blight", period, duration, tickDamage, applier);
            this.types.add(EffectType.DISEASE);
            this.types.add(EffectType.DISPELLABLE);
            addMobEffect(19, (int) (duration / 1000) * 20, 0, true);
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
            broadcast(lEntity.getLocation(), expireText, Messaging.getLivingEntityName(lEntity).toLowerCase());
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
            damageNearby(lEntity);
        }

        @Override
        public void tick(Hero hero) {
            super.tick(hero);
            damageNearby(hero.getPlayer());
        }

        private void damageNearby(LivingEntity lEntity) {
            int radius = SkillConfigManager.getUseSetting(applyHero, skill, Setting.RADIUS, 4, false);
            for (Entity target : lEntity.getNearbyEntities(radius, radius, radius)) {
                if (!(target instanceof LivingEntity) || target.equals(applier) || applyHero.getSummons().contains(target)) {
                    continue;
                }

                LivingEntity lTarget = (LivingEntity) target;

                // PvP Check
                if (!damageCheck(getApplier(), lTarget)) {
                    continue;
                }

                if (target instanceof Player) {
                    // Also ignore players that already have the blight effect
                    if (plugin.getHeroManager().getHero((Player) target).hasEffect("Blight")) {
                        continue;
                    }
                } else if (target instanceof LivingEntity && plugin.getEffectManager().entityHasEffect((LivingEntity) target, "Blight")) {
                    continue;
                } else {
                    // Skip this one if for some reason it's not a creature or player
                    continue;
                }

                addSpellTarget(target, applyHero);
                Skill.damageEntity((LivingEntity) target, applier.getPlayer(), tickDamage, DamageCause.MAGIC);
            }
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
