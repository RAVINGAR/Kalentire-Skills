
package com.herocraftonline.heroes.characters.skill.skills;
// old src  http://pastie.org/private/gfhf451ziiv1tbnkpufcwq

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

public class SkillChainLightning extends TargettedSkill {

    public SkillChainLightning(Heroes plugin) {
        super(plugin, "ChainLightning");
        setDescription("Strikes lightning at target location, dealing $1 damage. "
                + "Every $2 seconds, the spell will attempt to bounce to another player within $3 blocks, "
                + "dealing $4% damage on bounce. "
                + "Every successive hit will decrease the caster's cooldown of this skill by $5 seconds.");
        setIdentifiers("skill chainlightning");
        this.setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_LIGHTNING, SkillType.AGGRESSIVE, SkillType.SILENCEABLE);
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        // Skill shouldn't be handling its own targetting
        //if (target == null || hero.getEntity() == target) {
        //    if (target == null) {
        //        hero.getPlayer().sendMessage(ChatColor.GRAY + "[" + ChatColor.GREEN + "Skill" + ChatColor.GRAY + "] No Target!");
        //    } else {
        //        hero.getPlayer().sendMessage(ChatColor.GRAY + "[" + ChatColor.GREEN + "Skill" + ChatColor.GRAY + "] Cannot Target Yourself!");
        //    }
        //    return SkillResult.INVALID_TARGET_NO_MSG;
        //}

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100, false);
        long bounceTime = (long) (SkillConfigManager.getUseSetting(hero, this, "bounceTime", 2000, false));
        int bounceRadius = (int) SkillConfigManager.getUseSetting(hero, this, "bounceRadius", 5, false);
        double damageReductionPercent = SkillConfigManager.getUseSetting(hero, this, "bounceDamageMultiplier", 0.75, false);
        long cdr = (long) (SkillConfigManager.getUseSetting(hero, this, "bounceCooldownReduction", 1000, false));
        int maxBounce = (int) SkillConfigManager.getUseSetting(hero, this, "maxBounce", 5, false);
        CharacterTemplate cT = plugin.getCharacterManager().getCharacter(target);
        cT.addEffect(new ChainLightningEffect(this, plugin, bounceTime, damage, hero, damageReductionPercent, bounceRadius, cdr, maxBounce));
        broadcast(hero.getPlayer().getLocation(), ChatColor.GRAY + "[" + ChatColor.GREEN + "Skill" + ChatColor.GRAY + "] " + hero.getName() + " used ChainLightning!");
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        DecimalFormat dF = new DecimalFormat("##.##");
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100, false);
        int bounceTime = (int) (SkillConfigManager.getUseSetting(hero, this, "bounceTime", 2000, false) * 0.001);
        int bounceRadius = SkillConfigManager.getUseSetting(hero, this, "bounceRadius", 5, false);
        double damageReductionPercent = SkillConfigManager.getUseSetting(hero, this, "bounceDamageMultiplier", 0.75, false);
        int cdr = (int) (SkillConfigManager.getUseSetting(hero, this, "bounceCooldownReduction", 1000, false) * 0.001);
        return getDescription().replace("$1",dF.format(damage))
                .replace("$2", bounceTime + "")
                .replace("$3", bounceRadius + "")
                .replace("$4", dF.format(damageReductionPercent * 100))
                .replace("$5", cdr + "");
    }

    public class ChainLightningEffect extends ExpirableEffect {

        private double damage;
        private Hero caster;
        private double bouncePercent;
        private int bounceRadius;
        private long cdr;
        private long bounceTime;
        private int remaining;
        public ChainLightningEffect(Skill skill, Heroes plugin, long duration, double damage, Hero caster, double bouncePercent, int bounceRadius, long cdr, int remaining) {
            this(skill, "ChainLightningEffect" + System.currentTimeMillis(), plugin, duration, damage, caster, bouncePercent, bounceRadius, cdr, remaining);
        }
        public ChainLightningEffect(Skill skill, String name, Heroes plugin, long duration, double damage, Hero caster, double bouncePercent, int bounceRadius, long cdr, int remaining) {
            super(skill, plugin, name, caster.getPlayer(), duration);
            this.damage = damage;
            this.caster = caster;
            this.bouncePercent = bouncePercent;
            this.bounceRadius = bounceRadius;
            this.cdr = cdr;
            this.bounceTime = duration;
            this.remaining = remaining;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.HARMFUL);
        }

        @Override
        public void apply(CharacterTemplate cT) {
            super.apply(cT);
            addSpellTarget(cT.getEntity(),caster);
            skill.damageEntity(cT.getEntity(), caster.getEntity(), damage, DamageCause.MAGIC, false);
            if (cT instanceof Hero) {
                ((Player)(cT.getEntity())).sendMessage(ChatColor.GRAY + "[" + ChatColor.GREEN + "Skill" + ChatColor.GRAY + "] Hit by ChainLightning from " + caster.getName() + "!");
            }
            cT.getEntity().getLocation().getWorld().spigot().strikeLightningEffect(cT.getEntity().getLocation(), true);
            cT.getEntity().getWorld().playSound(cT.getEntity().getLocation(), Sound.AMBIENCE_THUNDER, getLightningVolume(caster), 1.0F);
        }

        @Override
        public void remove(CharacterTemplate cT) {
            super.remove(cT);
            remaining--;
            if (remaining <= 0) {
                return;
            }

            // Target is a Player if found, entityTarget is a non-player. After checks, target is set to entityTarget if blank and it exists.
            LivingEntity target = null;
            LivingEntity entityTarget = null;
            List<Entity> nearby = cT.getEntity().getNearbyEntities(bounceRadius, bounceRadius, bounceRadius);
            Collections.shuffle(nearby);
            for (Entity e : nearby) {
                if (e instanceof Player) {
                    if (((LivingEntity) e).equals(cT.getEntity())) {
                        continue;
                    }
                    if (((Player) e).equals(caster.getEntity())) {
                        continue;
                    }
                    if (Skill.damageCheck(caster.getPlayer(), (LivingEntity) e)) {
                        target = (Player) e;
                        break;
                    }
                } else if (e instanceof LivingEntity) {
                    if (((LivingEntity) e).equals(cT.getEntity())) {
                        continue;
                    }
                    if (((LivingEntity) e).equals(caster.getEntity())) {
                        continue;
                    }
                    if (Skill.damageCheck(caster.getPlayer(), (LivingEntity) e)) {
                        entityTarget = (LivingEntity) e;
                    }
                }
            }

            if(target == null && entityTarget != null) {
                target = entityTarget;
            }

            if (target != null) {
                Long cd = caster.getCooldown(skill.getName());
                if (cd != null) {
                    caster.setCooldown(skill.getName(), cd-cdr);
                }
                CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                targetCT.addEffect(new ChainLightningEffect(skill,plugin,bounceTime,damage,caster,bouncePercent,bounceRadius,cdr,remaining));
                return;
            } else {
                return;
            }
        }

    }

    public float getLightningVolume(Hero h) {
        return (float) SkillConfigManager.getUseSetting(h, this, "lightning-volume", 0.0F, false);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), (double) 100);
        node.set("bounceTime", (long) 2000);
        node.set("bounceRadius", 5);
        node.set("bounceDamageMultiplier", 0.75);
        node.set("bounceCooldownReduction", (long) 1000);
        node.set("lightning-volume", 0.0F);
        return node;

    }

}