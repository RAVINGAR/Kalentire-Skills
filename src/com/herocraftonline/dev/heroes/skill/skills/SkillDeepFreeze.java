package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityListener;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.HeroesEventListener;
import com.herocraftonline.dev.heroes.api.SkillDamageEvent;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.PeriodicExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillDeepFreeze extends TargettedSkill {

    private String applyText;
    private String expireText;
    private String shatterText;

    public SkillDeepFreeze(Heroes plugin) {
        super(plugin, "DeepFreeze");
        setDescription("Freezes your target in place. If they take fire damage they will shatter");
        setUsage("/skill deepfreeze <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill deepfreeze", "skill dfreeze");
        setTypes(SkillType.ICE, SkillType.SILENCABLE, SkillType.DEBUFF, SkillType.DAMAGING, SkillType.HARMFUL);

        registerEvent(Type.CUSTOM_EVENT, new SkillHeroListener(), Priority.Monitor);
        registerEvent(Type.ENTITY_COMBUST, new SkillEntityListener(), Priority.Monitor);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000);
        node.set(Setting.DAMAGE.node(), 1);
        node.set("shatter-damage", 11);
        node.set(Setting.APPLY_TEXT.node(), "%target% was frozen in place!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% has thawed from their icy prison!");
        node.set("shatter-text", "%target%'s icy prison shattered from the intense heat!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = getSetting(null, Setting.APPLY_TEXT.node(), "%target% was frozen in place!").replace("%target%", "$1");
        expireText = getSetting(null, Setting.EXPIRE_TEXT.node(), "%target% has thawed from their icy prison!").replace("%target%", "$1");
        shatterText = getSetting(null, "shatter-text", "%target%'s icy prison shattered from the intense heat!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        long duration = getSetting(hero, Setting.DURATION.node(), 5000, false);
        FreezeEffect fEffect = new FreezeEffect(this, duration, hero);

        if (target instanceof Player) {
            plugin.getHeroManager().getHero((Player) target).addEffect(fEffect);
        } else if (target instanceof Creature) {
            plugin.getEffectManager().addCreatureEffect((Creature) target, fEffect);
        } else
        	return SkillResult.INVALID_TARGET;

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public class FreezeEffect extends PeriodicExpirableEffect {

        private static final long period = 100;
        private final Hero applier;
        private double x, y, z;

        public FreezeEffect(Skill skill, long duration, Hero applier) {
            super(skill, "Freeze", period, duration);
            this.applier = applier;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.ICE);
            this.types.add(EffectType.ROOT);
            this.types.add(EffectType.UNBREAKABLE);
        }

        @Override
        public void apply(Creature creature) {
            super.apply(creature);
            creature.setFireTicks(0);
            Location location = creature.getLocation();
            x = location.getX();
            y = location.getY();
            z = location.getZ();

            broadcast(location, applyText, Messaging.getCreatureName(creature));
        }

        @Override
        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            player.setFireTicks(0);
            Location location = player.getLocation();
            x = location.getX();
            y = location.getY();
            z = location.getZ();

            broadcast(location, applyText, player.getDisplayName());
        }

        public Hero getApplier() {
            return applier;
        }
        
        public void shatter(Creature creature) {
            super.remove(creature);
            int damage = getSetting(applier, "shatter-damage", 7, false);
            addSpellTarget(creature, applier);
            creature.damage(damage, applier.getPlayer());
            broadcast(creature.getLocation(), shatterText, Messaging.getCreatureName(creature));
        }
        
        public void shatter(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            int damage = getSetting(applier, "shatter-damage", 7, false);
            addSpellTarget(player, applier);
            player.damage(damage, applier.getPlayer());
            broadcast(player.getLocation(), shatterText, player.getDisplayName());
        }
        
        @Override
        public void remove(Creature creature) {
            super.remove(creature);
            broadcast(creature.getLocation(), expireText, Messaging.getCreatureName(creature));
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

        @Override
        public void tick(Creature creature) {
            super.tick(creature);
            
            Location location = creature.getLocation();
            if (location.getX() != x || location.getY() != y || location.getZ() != z) {
                location.setX(x);
                location.setY(y);
                location.setZ(z);
                location.setYaw(creature.getLocation().getYaw());
                location.setPitch(creature.getLocation().getPitch());
                creature.teleport(location);
            }
        }
        
        @Override
        public void tick(Hero hero) {
            super.tick(hero);

            Player player = hero.getPlayer();
            Location location = player.getLocation();
            if (location.getX() != x || location.getY() != y || location.getZ() != z) {
                location.setX(x);
                location.setY(y);
                location.setZ(z);
                location.setYaw(player.getLocation().getYaw());
                location.setPitch(player.getLocation().getPitch());
                player.teleport(location);
            }
        }
    }

    public class SkillEntityListener extends EntityListener {

        @Override
        public void onEntityCombust(EntityCombustEvent event) {
            Heroes.debug.startTask("HeroesSkillListener");
            if (event.isCancelled() || !(event.getEntity() instanceof LivingEntity)) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }

            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                Hero tHero = plugin.getHeroManager().getHero(player);
                if (tHero.hasEffect("Freeze")) {
                    FreezeEffect fEffect = (FreezeEffect) tHero.getEffect("Freeze");
                    fEffect.shatter(tHero);
                    tHero.manualRemoveEffect(fEffect);
                }
            } else if (event.getEntity() instanceof Creature) {
                Creature creature = (Creature) event.getEntity();
                if (plugin.getEffectManager().creatureHasEffect(creature, "Freeze")) {
                    FreezeEffect fEffect = (FreezeEffect) plugin.getEffectManager().getCreatureEffect(creature, "Freeze");
                    fEffect.shatter(creature);
                    plugin.getEffectManager().manualRemoveCreatureEffect(creature, fEffect);
                }
            }
            Heroes.debug.stopTask("HeroesSkillListener");
        }
    }

    public class SkillHeroListener extends HeroesEventListener {

        @Override
        public void onSkillDamage(SkillDamageEvent event) {
            Heroes.debug.startTask("HeroesSkillListener");
            if (event.isCancelled() || event.getDamage() == 0 || !event.getSkill().isType(SkillType.FIRE)) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }

            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                Hero tHero = plugin.getHeroManager().getHero(player);
                if (tHero.hasEffect("Freeze")) {
                    FreezeEffect fEffect = (FreezeEffect) tHero.getEffect("Freeze");
                    fEffect.shatter(tHero);
                    tHero.manualRemoveEffect(fEffect);
                }
            } else if (event.getEntity() instanceof Creature) {
                Creature creature = (Creature) event.getEntity();
                if (plugin.getEffectManager().creatureHasEffect(creature, "Freeze")) {
                    FreezeEffect fEffect = (FreezeEffect) plugin.getEffectManager().getCreatureEffect(creature, "Freeze");
                    fEffect.shatter(creature);
                    plugin.getEffectManager().manualRemoveCreatureEffect(creature, fEffect);
                }
            }
            Heroes.debug.stopTask("HeroesSkillListener");
        }
    }
}
