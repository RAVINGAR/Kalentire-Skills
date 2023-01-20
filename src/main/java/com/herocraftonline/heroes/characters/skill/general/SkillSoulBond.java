package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillSoulBond extends TargettedSkill {

    private String expireText;

    public SkillSoulBond(final Heroes plugin) {
        super(plugin, "SoulBond");
        setDescription("You split damage with your target.");
        setUsage("/skill soulbond <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill soulbond", "skill sbond");
        setTypes(SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_LIGHT, SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroesListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 300000);
        node.set("damage-multiplier", .5);
        node.set(SkillSetting.RADIUS.node(), 25);
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target%'s soul is no longer bound to %hero%!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target%'s soul is no longer bound to %hero%!").replace("%target%", "$1").replace("$target$", "$1").replace("%hero%", "$2").replace("$hero$", "$2");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final Player player = hero.getPlayer();
        if (target.equals(player) || (target instanceof Creature && !hero.getSummons().contains(target))) {
            return SkillResult.INVALID_TARGET;
        }

        if (target instanceof Player && (!hero.hasParty() || !hero.getParty().isPartyMember(plugin.getCharacterManager().getHero((Player) target)))) {
            return SkillResult.INVALID_TARGET;
        }

        // Remove the previous effect before applying a new one
        if (hero.hasEffect("SoulBond")) {
            hero.removeEffect(hero.getEffect("SoulBond"));
        }

        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 300000, false);
        final SoulBondedEffect sbEffect = new SoulBondedEffect(this, player);
        hero.addEffect(new SoulBondEffect(this, hero.getPlayer(), duration, target, sbEffect));
        plugin.getCharacterManager().getCharacter(target).addEffect(sbEffect);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(final Hero hero) {
        return getDescription();
    }

    public static class SoulBondedEffect extends Effect {

        private final Player applier;

        public SoulBondedEffect(final Skill skill, final Player applier) {
            super(skill, "SoulBonded");
            this.applier = applier;
        }

        @Override
        public Player getApplier() {
            return applier;
        }
    }

    public class SkillHeroesListener implements Listener {

        private final Skill skill;

        public SkillHeroesListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onSkillDamage(final SkillDamageEvent event) {
            if (event.isCancelled()) {
                return;
            }
            final LivingEntity target = (LivingEntity) event.getEntity();

            final CharacterTemplate character = plugin.getCharacterManager().getCharacter(target);
            // Make sure the target doesn't have both effects
            if (character.hasEffect("SoulBonded") && !character.hasEffect("SoulBond")) {
                final Player applier = ((SoulBondedEffect) character.getEffect("SoulBonded")).getApplier();
                final Hero hero = plugin.getCharacterManager().getHero(applier);

                // Distance check
                final int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 25, false);
                final int radiusSquared = radius * radius;
                if (applier.getLocation().distanceSquared(target.getLocation()) > radiusSquared) {
                    return;
                }

                // Split the damage
                final double splitDamage = (event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "damage-multiplier", .5, false));
                damageEntity(applier, event.getDamager().getEntity(), splitDamage, DamageCause.MAGIC);
                event.setDamage(event.getDamage() - splitDamage);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onWeaponDamage(final WeaponDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            final LivingEntity target = (LivingEntity) event.getEntity();
            final LivingEntity damager = event.getDamager().getEntity();
            final CharacterTemplate character = plugin.getCharacterManager().getCharacter(target);
            // Make sure the target doesn't have both effects
            if (character.hasEffect("SoulBonded") && !character.hasEffect("SoulBond")) {
                final Player applier = ((SoulBondedEffect) character.getEffect("SoulBonded")).getApplier();
                final Hero hero = plugin.getCharacterManager().getHero(applier);

                // Distance check
                final int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 25, false);
                final int radiusSquared = radius * radius;
                if (applier.getLocation().distanceSquared(target.getLocation()) > radiusSquared) {
                    return;
                }

                // Split the damage
                final double splitDamage = (event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "damage-multiplier", .5, false));
                damageEntity(applier, damager, splitDamage, DamageCause.MAGIC);
                event.setDamage(event.getDamage() - splitDamage);
            }
        }
    }

    public class SoulBondEffect extends ExpirableEffect {

        private final LivingEntity target;
        private final Effect bondEffect;

        public SoulBondEffect(final Skill skill, final Player applier, final long duration, final LivingEntity target, final Effect bondEffect) {
            super(skill, "SoulBond", applier, duration);
            this.target = target;
            this.bondEffect = bondEffect;
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.MAGIC);
        }

        public LivingEntity getTarget() {
            return target;
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            plugin.getCharacterManager().getCharacter(target).removeEffect(bondEffect);
            broadcast(player.getLocation(), "    " + expireText, CustomNameManager.getName(target), player.getName());
        }
    }
}
