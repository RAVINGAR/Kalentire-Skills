package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.HeroesEventListener;
import com.herocraftonline.dev.heroes.api.SkillDamageEvent;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.api.WeaponDamageEvent;
import com.herocraftonline.dev.heroes.effects.Effect;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.ExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillSoulBond extends TargettedSkill {

    private String expireText;

    public SkillSoulBond(Heroes plugin) {
        super(plugin, "SoulBond");
        setDescription("You share your targets pain");
        setUsage("/skill soulbond <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill soulbond", "skill sbond");
        setTypes(SkillType.SILENCABLE, SkillType.LIGHT, SkillType.BUFF);

        registerEvent(Type.CUSTOM_EVENT, new SkillHeroesListener(this), Priority.Highest);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 300000);
        node.set("damage-multiplier", .5);
        node.set(Setting.RADIUS.node(), 25);
        node.set(Setting.EXPIRE_TEXT.node(), "%target%'s soul is no longer bound to %hero%!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%target%'s soul is no longer bound to %hero%!").replace("%target%", "$1").replace("%hero%", "$2");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (target.equals(player) || !hero.getSummons().contains(target)) 
        	return SkillResult.INVALID_TARGET;

        if (target instanceof Player && (!hero.hasParty() || !hero.getParty().isPartyMember(plugin.getHeroManager().getHero((Player) target))))
        	return SkillResult.INVALID_TARGET;

        // Remove the previous effect before applying a new one
        if (hero.hasEffect("SoulBond")) {
            hero.removeEffect(hero.getEffect("SoulBond"));
        }

        long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 300000, false);
        SoulBondedEffect sbEffect = new SoulBondedEffect(this, player);
        hero.addEffect(new SoulBondEffect(this, duration, target, sbEffect));

        if (target instanceof Player) {
            plugin.getHeroManager().getHero((Player) target).addEffect(sbEffect);
        } else {
            plugin.getEffectManager().addEntityEffect(target, sbEffect);
        }

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public class SkillHeroesListener extends HeroesEventListener {

        private final Skill skill;
        
        public SkillHeroesListener(Skill skill) {
            this.skill = skill;
        }
        
        @Override
        public void onSkillDamage(SkillDamageEvent event) {
            Heroes.debug.startTask("HeroesSkillListener");
            if (event.isCancelled()) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }
            LivingEntity target = (LivingEntity) event.getEntity();
            
            if (target instanceof Player) {
                Hero tHero = plugin.getHeroManager().getHero((Player) target);

                // Make sure the target doesn't have both effects
                if (tHero.hasEffect("SoulBonded") && !tHero.hasEffect("SoulBond")) {
                    Player applier = ((SoulBondedEffect) tHero.getEffect("SoulBonded")).getApplier();
                    Hero hero = plugin.getHeroManager().getHero(applier);

                    // Distance check
                    int radius = SkillConfigManager.getUseSetting(hero, skill, Setting.RADIUS, 25, false);
                    int radiusSquared = radius * radius;
                    if (applier.getLocation().distanceSquared(target.getLocation()) > radiusSquared) {
                        Heroes.debug.stopTask("HeroesSkillListener");
                        return;
                    }

                    // Split the damage
                    int splitDamage = (int) (event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "damage-multiplier", .5, false));
                    applier.damage(splitDamage, event.getDamager().getPlayer());
                    event.setDamage(event.getDamage() - splitDamage);
                }
            } else {
                if (!plugin.getEffectManager().entityHasEffect(target, "SoulBonded")) {
                    Heroes.debug.stopTask("HeroesSkillListener");
                    return;
                }

                Player applier = ((SoulBondedEffect) plugin.getEffectManager().getEntityEffect(target, "SoulBonded")).getApplier();
                Hero hero = plugin.getHeroManager().getHero(applier);

                // Distance check
                int radius = SkillConfigManager.getUseSetting(hero, skill, Setting.RADIUS, 25, false);
                int radiusSquared = radius * radius;
                if (applier.getLocation().distanceSquared(target.getLocation()) > radiusSquared) {
                    Heroes.debug.stopTask("HeroesSkillListener");
                    return;
                }

                // Split the damage
                int splitDamage = (int) (event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "damage-multiplier", .5, false));
                applier.damage(splitDamage, event.getDamager().getPlayer());
                event.setDamage(event.getDamage() - splitDamage);
            }
            Heroes.debug.stopTask("HeroesSkillListener");
        }

        @Override
        public void onWeaponDamage(WeaponDamageEvent event) {
            Heroes.debug.startTask("HeroesSkillListener");
            if (event.isCancelled() || !(event.getEntity() instanceof LivingEntity)) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }
            
            LivingEntity target = (LivingEntity) event.getEntity();
            
            if (target instanceof Player) {
                Hero tHero = plugin.getHeroManager().getHero((Player) target);

                // Make sure the target doesn't have both effects
                if (tHero.hasEffect("SoulBonded") && !tHero.hasEffect("SoulBond")) {
                    Player applier = ((SoulBondedEffect) tHero.getEffect("SoulBonded")).getApplier();
                    Hero hero = plugin.getHeroManager().getHero(applier);

                    // Distance check
                    int radius = SkillConfigManager.getUseSetting(hero, skill, Setting.RADIUS, 25, false);
                    int radiusSquared = radius * radius;
                    if (applier.getLocation().distanceSquared(target.getLocation()) > radiusSquared) {
                        Heroes.debug.stopTask("HeroesSkillListener");
                        return;
                    }

                    // Split the damage
                    int splitDamage = (int) (event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "damage-multiplier", .5, false));
                    applier.damage(splitDamage, event.getDamager());
                    event.setDamage(event.getDamage() - splitDamage);
                }
            } else {
                if (!plugin.getEffectManager().entityHasEffect(target, "SoulBonded")) {
                    Heroes.debug.stopTask("HeroesSkillListener");
                    return;
                }

                Player applier = ((SoulBondedEffect) plugin.getEffectManager().getEntityEffect(target, "SoulBonded")).getApplier();
                Hero hero = plugin.getHeroManager().getHero(applier);

                // Distance check
                int radius = SkillConfigManager.getUseSetting(hero, skill, Setting.RADIUS, 25, false);
                int radiusSquared = radius * radius;
                if (applier.getLocation().distanceSquared(target.getLocation()) > radiusSquared) {
                    Heroes.debug.stopTask("HeroesSkillListener");
                    return;
                }

                // Split the damage
                int splitDamage = (int) (event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "damage-multiplier", .5, false));
                applier.damage(splitDamage, event.getDamager());
                event.setDamage(event.getDamage() - splitDamage);
            }
            Heroes.debug.stopTask("HeroesSkillListener");
        }
    }

    public class SoulBondedEffect extends Effect {

        private final Player applier;

        public SoulBondedEffect(Skill skill, Player applier) {
            super(skill, "SoulBonded");
            this.applier = applier;
        }

        public Player getApplier() {
            return applier;
        }
    }

    public class SoulBondEffect extends ExpirableEffect {

        private final LivingEntity target;
        private final Effect bondEffect;

        public SoulBondEffect(Skill skill, long duration, LivingEntity target, Effect bondEffect) {
            super(skill, "SoulBond", duration);
            this.target = target;
            this.bondEffect = bondEffect;
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.DISPELLABLE);
        }

        public LivingEntity getTarget() {
            return target;
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            String name = null;

            if (target instanceof Player) {
                name = ((Player) target).getDisplayName();
                plugin.getHeroManager().getHero((Player) target).removeEffect(bondEffect);
            } else {
                name = Messaging.getLivingEntityName(target);
                plugin.getEffectManager().removeEntityEffect(target, bondEffect);
            }

            broadcast(player.getLocation(), expireText, name, player.getDisplayName());
        }
    }
}
