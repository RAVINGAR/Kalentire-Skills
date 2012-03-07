package com.herocraftonline.heroes.characters.skill.skills;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Setting;

public class SkillChainLightning extends TargettedSkill {

    public SkillChainLightning(Heroes plugin) {
        super(plugin, "ChainLightning");
        setDescription("You call down a bolt of lightning that bounces to nearby enemies.");
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

        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 6, false);

        // Damage the first target
        target.getWorld().strikeLightningEffect(target.getLocation());
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        // Try to bounce
        Set<Entity> previousTargets = new HashSet<Entity>();
        previousTargets.add(target);
        int range = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 7, false);
        int bounces = SkillConfigManager.getUseSetting(hero, this, "max-bounces", 3, false);
        int maxBounce = bounces + 1;
        boolean keepBouncing = true;
        while (bounces > 0 && keepBouncing) {
            for (Entity entity : target.getNearbyEntities(range, range, range)) {
                keepBouncing = false;
                if (!(entity instanceof LivingEntity) || previousTargets.contains(entity) || !damageCheck(player, (LivingEntity) entity)) {
                    continue;
                }
                
                //  make sure the target has LoS
                if (checkTarget(target, entity)) {
                    plugin.getCharacterManager().getCharacter(target).addEffect(new DelayedBolt(this, (maxBounce - bounces) * 200, hero, damage));
                    keepBouncing = true;
                    break;
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
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            addSpellTarget(monster.getEntity(), applier);
            damageEntity(monster.getEntity(), applier.getPlayer(), bounceDamage, DamageCause.MAGIC);
            monster.getEntity().getWorld().strikeLightningEffect(monster.getEntity().getLocation());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player target = hero.getPlayer();
            addSpellTarget(target, applier);
            damageEntity(target, applier.getPlayer(), bounceDamage, DamageCause.MAGIC);
            target.getWorld().strikeLightningEffect(target.getLocation());
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
