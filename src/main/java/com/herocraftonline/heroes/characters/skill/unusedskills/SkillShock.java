package com.herocraftonline.heroes.characters.skill.unusedskills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillShock extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillShock(final Heroes plugin) {
        super(plugin, "Shock");
        setDescription("Creates a storm around your target, dealing $1 damage to nearby targets every $2 second(s) for $3 second(s)");
        setUsage("/skill shock");
        setArgumentRange(0, 0);
        setIdentifiers("skill shock");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHTNING, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 4);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set(SkillSetting.DURATION.node(), 21000);
        node.set(SkillSetting.PERIOD.node(), 3000);
        node.set(SkillSetting.APPLY_TEXT.node(), "A storm forms around %target%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "The storm around %target% dissipates!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "A storm forms around %target%!").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "The storm around %target% dissipates!").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final Player player = hero.getPlayer();

        final int range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        final int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 4, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 21000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, false);

        plugin.getCharacterManager().getCharacter(target).addEffect(new ShockEffect(this, period, duration, damage, range, player));

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(final Hero hero) {
        final int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 4, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 21000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, false);

        return getDescription().replace("$1", damage + "").replace("$2", (period / 1000) + "").replace("$3", (duration / 1000) + "");
    }

    public class ShockEffect extends PeriodicDamageEffect {

        private final int range;

        public ShockEffect(final Skill skill, final long period, final long duration, final double tickDamage, final int range, final Player applier) {
            super(skill, "Shock", applier, period, duration, tickDamage);
            this.range = range;

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.LIGHTNING);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            broadcast(hero.getPlayer().getLocation(), "    " + applyText, hero.getName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            this.broadcast(hero.getPlayer().getLocation(), "    " + expireText, hero.getName());
        }

        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + applyText, CustomNameManager.getName(monster));
        }

        @Override
        public void removeFromMonster(final Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, CustomNameManager.getName(monster));
        }

        @Override
        public void tickHero(final Hero hero) {
            super.tickHero(hero);
            final Player target = hero.getPlayer();
            final Player player = getApplier();
            target.getWorld().strikeLightningEffect(target.getLocation());
            for (final Entity entity : target.getNearbyEntities(range, range, range)) {
                if (entity instanceof LivingEntity && !entity.equals(player)) {

                    if (!damageCheck(getApplier(), (LivingEntity) entity)) {
                        continue;
                    }
                    addSpellTarget(entity, plugin.getCharacterManager().getHero(getApplier()));
                    damageEntity((LivingEntity) entity, player, getTickDamage(), DamageCause.MAGIC);
                }
            }
        }

        @Override
        public void tickMonster(final Monster monster) {
            final LivingEntity target = monster.getEntity();
            final Player player = getApplier();
            target.getWorld().strikeLightningEffect(target.getLocation());
            for (final Entity entity : target.getNearbyEntities(range, range, range)) {
                if (entity instanceof LivingEntity && !entity.equals(player)) {

                    if (!damageCheck(getApplier(), (LivingEntity) entity)) {
                        continue;
                    }
                    addSpellTarget(entity, plugin.getCharacterManager().getHero(getApplier()));
                    damageEntity((LivingEntity) entity, player, getTickDamage(), DamageCause.MAGIC, false);
                }
            }
        }

    }

}
