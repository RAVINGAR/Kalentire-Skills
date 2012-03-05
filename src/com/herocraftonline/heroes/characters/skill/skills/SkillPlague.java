package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

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

public class SkillPlague extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillPlague(Heroes plugin) {
        super(plugin, "Plague");
        setDescription("You infect your target with the plague, dealing $1 damage over $2 seconds.!");
        setUsage("/skill plague <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill plague");
        setTypes(SkillType.DARK, SkillType.DAMAGING, SkillType.SILENCABLE, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 21000);
        node.set(Setting.PERIOD.node(), 3000);
        node.set("tick-damage", 1);
        node.set(Setting.RADIUS.node(), 4);
        node.set(Setting.APPLY_TEXT.node(), "%target% is infected with the plague!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% is no longer infected with the plague!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%target% is infected with the plague!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%target% is no longer infected with the plague!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 21000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 3000, true);
        int tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        PlagueEffect bEffect = new PlagueEffect(this, duration, period, tickDamage, player);

        if (target instanceof Player) {
            plugin.getHeroManager().getHero((Player) target).addEffect(bEffect);
        } else
            plugin.getEffectManager().addEntityEffect(target, bEffect);

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public class PlagueEffect extends PeriodicDamageEffect {
        
        public PlagueEffect(Skill skill, long duration, long period, int tickDamage, Player applier) {
            super(skill, "Plague", period, duration, tickDamage, applier);
            addMobEffect(19, (int) (duration / 1000) * 20, 0, true);
        }

        // Clone Constructor
        private PlagueEffect(PlagueEffect pEffect) {
            super(pEffect.getSkill(), pEffect.getName(), pEffect.getPeriod(), pEffect.getRemainingTime(), pEffect.tickDamage, pEffect.applier);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.DISEASE);
            addMobEffect(19, (int) (pEffect.getRemainingTime() / 1000) * 20, 0, true);
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
            spreadToNearbyEntities(lEntity);
        }

        @Override
        public void tick(Hero hero) {
            super.tick(hero);
            spreadToNearbyEntities(hero.getPlayer());
        }

        /**
         * Attempts to spread the effect to all nearby entities
         * Will not target non-pvpable targets
         * 
         * @param lEntity
         */
        private void spreadToNearbyEntities(LivingEntity lEntity) {
            int radius = SkillConfigManager.getUseSetting(applyHero, skill, Setting.RADIUS.node(), 4, false);
            for (Entity target : lEntity.getNearbyEntities(radius, radius, radius)) {
                if (!(target instanceof LivingEntity) || target.equals(applier) || applyHero.getSummons().contains(target)) {
                    continue;
                }

                if (!damageCheck(getApplier(), (LivingEntity) target)) {
                    continue;
                }

                if (target instanceof Player) {
                    Hero tHero = plugin.getHeroManager().getHero((Player) target);
                    // Ignore heroes that already have the plague effect
                    if (tHero.hasEffect("Plague")) {
                        continue;
                    }

                    // Apply the effect to the hero creating a copy of the effect
                    tHero.addEffect(new PlagueEffect(this));
                } else {
                    LivingEntity le = (LivingEntity) target;
                    // Make sure the creature doesn't already have the effect
                    if (plugin.getEffectManager().entityHasEffect(le, "Plague")) {
                        continue;
                    }

                    // Apply the effect to the creature, creating a copy of the effect
                    plugin.getEffectManager().addEntityEffect(le, new PlagueEffect(this));
                }
            }
        }
    }
    
    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
        double period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 2000, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        return getDescription().replace("$1", damage * duration / period + "").replace("$2", duration / 1000 + "");
    }
}
