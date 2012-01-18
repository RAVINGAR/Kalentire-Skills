package com.herocraftonline.dev.heroes.skill.skills;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.PeriodicExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Setting;
import com.herocraftonline.dev.heroes.util.Util;

public class SkillBoltstorm extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillBoltstorm(Heroes plugin) {
        super(plugin, "Boltstorm");
        setDescription("You call bolts of lightning down upon nearby enemies for $1 seconds, each bolt will deal $2 damage.");
        setUsage("/skill boltstorm");
        setArgumentRange(0, 0);
        setIdentifiers("skill boltstorm");
        setTypes(SkillType.LIGHTNING, SkillType.SILENCABLE, SkillType.DAMAGING);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.RADIUS.node(), 7); // radius
        node.set(Setting.DURATION.node(), 10000); // in milliseconds
        node.set(Setting.PERIOD.node(), 1000); // in milliseconds
        node.set(Setting.DAMAGE.node(), 4); // Per-tick damage
        node.set(Setting.APPLY_TEXT.node(), "%hero% has summoned a boltstorm!");
        node.set(Setting.EXPIRE_TEXT.node(), "%hero%'s boltstorm has subsided!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%hero% has summoned a boltstorm!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%hero%'s boltstorm has subsided!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        int period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 1000, true);
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
        hero.addEffect(new BoltStormEffect(this, period, duration));
        return SkillResult.NORMAL;
    }

    public class BoltStormEffect extends PeriodicExpirableEffect {

        public BoltStormEffect(Skill skill, long period, long duration) {
            super(skill, "Boltstorm", period, duration);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.LIGHTNING);
        }

        @Override
        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

        @Override
        public void tick(Hero hero) {
            super.tick(hero);

            Player player = hero.getPlayer();
            int range = SkillConfigManager.getUseSetting(hero, skill, Setting.RADIUS, 7, false);

            List<LivingEntity> targets = new ArrayList<LivingEntity>();
            for (Entity entity : player.getNearbyEntities(range, range, range)) {
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }

                LivingEntity target = (LivingEntity) entity;

                // never target the caster
                if (target.equals(player) || hero.getSummons().contains(target)) {
                    continue;
                }

                // check if the target is damagable
                if (!damageCheck(player, target)) {
                    continue;
                }

                targets.add(target);
            }

            if (targets.isEmpty())
                return;

            int damage = SkillConfigManager.getUseSetting(hero, skill, Setting.DAMAGE, 4, false);
            LivingEntity target = targets.get(Util.rand.nextInt(targets.size()));
            target.getWorld().strikeLightningEffect(target.getLocation());
            
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC);
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 4, false);
        return getDescription().replace("$1", duration / 1000 + "").replace("$2", damage + "");
    }
}
