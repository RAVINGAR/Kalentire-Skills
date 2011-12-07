package com.herocraftonline.dev.heroes.skill.skills;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.ExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.hero.HeroManager;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillChainLightning extends TargettedSkill {

    public SkillChainLightning(Heroes plugin) {
        super(plugin, "ChainLightning");
        setDescription("Calls down a bolt of lightning that bounces to other targets");
        setUsage("/skill chainl <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill chainlightning", "skill clightning", "skill chainl", "skill clight");
        setTypes(SkillType.LIGHTNING, SkillType.SILENCABLE, SkillType.HARMFUL, SkillType.DAMAGING);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DAMAGE.node(), 6);
        node.set("bounce-damage", 3);
        node.set(Setting.RADIUS.node(), 7);
        node.set("max-bounces", 3);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int damage = getSetting(hero, Setting.DAMAGE.node(), 6, false);

        // Damage the first target
        addSpellTarget(target, hero);
        target.getWorld().strikeLightningEffect(target.getLocation());
        target.damage(damage, player);

        // Try to bounce
        Set<Entity> previousTargets = new HashSet<Entity>();
        previousTargets.add(target);
        int range = getSetting(hero, Setting.RADIUS.node(), 7, false);
        int bounces = getSetting(hero, "max-bounces", 3, false);
        int maxBounce = bounces + 1;
        boolean keepBouncing = true;
        HeroManager heroManager = plugin.getHeroManager();
        while (bounces > 0 && keepBouncing) {
            for (Entity entity : target.getNearbyEntities(range, range, range)) {
                keepBouncing = false;
                if (!(entity instanceof LivingEntity))
                    continue;

                //PvP/Summon check the target
                if (!damageCheck(player, (LivingEntity) entity))
                    continue;
                
                // never bounce back to the player - and make sure the target has LoS
                if (!previousTargets.contains(entity) && checkTarget(target, entity)) {
                    if (target instanceof Player) {
                        Hero tHero = heroManager.getHero((Player) target);
                        tHero.addEffect(new DelayedBolt(this, (maxBounce - bounces) * 200, hero, damage));
                        keepBouncing = true;
                        break;
                    } else if (target instanceof LivingEntity) {
                        plugin.getEffectManager().addEntityEffect(target, new DelayedBolt(this, (maxBounce - bounces) * 200, hero, damage));
                        keepBouncing = true;
                        break;
                    } else {
                        continue;
                    }
                }
            }

            bounces -= 1;
        }
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    private boolean checkTarget(Entity previousTarget, Entity potentialTarget) {
    	/*
        Vector directional = potentialTarget.getLocation().clone().subtract(previousTarget.getLocation()).toVector();
        try {
            BlockIterator iter = new BlockIterator(previousTarget.getWorld(), previousTarget.getLocation().toVector(), directional, 0, (int) directional.length());
            while (iter.hasNext()) {
                if (!Util.transparentBlocks.contains(iter.next().getType()))
                    return false;
            }
        } catch (IllegalStateException e) {
            return false;
        }
        */
        return true;
    }

    public class DelayedBolt extends ExpirableEffect {

        private final Hero applier;
        private final int bounceDamage;

        public DelayedBolt(Skill skill, long duration, Hero applier, int bounceDamage) {
            super(skill, "DelayedBolt", duration);
            this.applier = applier;
            this.bounceDamage = bounceDamage;
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.LIGHTNING);
        }

        public Hero getApplier() {
            return applier;
        }

        @Override
        public void remove(LivingEntity lEntity) {
            super.remove(lEntity);
            addSpellTarget(lEntity, applier);
            lEntity.damage(bounceDamage, applier.getPlayer());
            lEntity.getWorld().strikeLightningEffect(lEntity.getLocation());
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player target = hero.getPlayer();
            addSpellTarget(target, applier);
            target.damage(bounceDamage, applier.getPlayer());
            target.getWorld().strikeLightningEffect(target.getLocation());
        }
    }
}
