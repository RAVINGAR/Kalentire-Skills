package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
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
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillBlight extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    
    private String applyText;
    private String expireText;

    public SkillBlight(Heroes plugin) {
        super(plugin, "Blight");
        setDescription("You disease your target, dealing $1 dark damage over $2 seconds, enemies that get too close will also be damaged.");
        setUsage("/skill blight");
        setArgumentRange(0, 0);
        setTypes(SkillType.DARK, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
        setIdentifiers("skill blight");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 21000);
        node.set(SkillSetting.PERIOD.node(), 3000);
        node.set("tick-damage", 1);
        node.set(SkillSetting.RADIUS.node(), 4);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% begins to radiate a cloud of disease!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% is no longer diseased!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% begins to radiate a cloud of disease!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% is no longer diseased!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 21000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, true);
        int tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        plugin.getCharacterManager().getCharacter(target).addEffect(new BlightEffect(this, duration, period, tickDamage, player));
        broadcastExecuteText(hero, target);
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), 
            		target.getLocation().add(0,1.5,0), 
            		FireworkEffect.builder()
            		.flicker(false)
            		.trail(false)
            		.with(FireworkEffect.Type.BALL)
            		.withColor(Color.GRAY)
            		.withFade(Color.GREEN)
            		.build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }

    public class BlightEffect extends PeriodicDamageEffect {

        public BlightEffect(Skill skill, long duration, long period, double tickDamage, Player applier) {
            super(skill, "Blight", period, duration, tickDamage, applier);
            this.types.add(EffectType.DISEASE);
            this.types.add(EffectType.DISPELLABLE);
            addMobEffect(19, (int) (duration / 1000) * 20, 0, true);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), expireText, Messaging.getLivingEntityName(monster).toLowerCase());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

        @Override
        public void tickMonster(Monster monster) {
            super.tickMonster(monster);
            damageNearby(monster.getEntity());
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
            damageNearby(hero.getPlayer());
        }

        private void damageNearby(LivingEntity lEntity) {
            int radius = SkillConfigManager.getUseSetting(applyHero, skill, SkillSetting.RADIUS, 4, false);
            for (Entity target : lEntity.getNearbyEntities(radius, radius, radius)) {
                if (!(target instanceof LivingEntity) || target.equals(applier) || applyHero.getSummons().contains(target)) {
                    continue;
                }

                LivingEntity lTarget = (LivingEntity) target;

                // PvP Check
                if (!damageCheck(getApplier(), lTarget)) {
                    continue;
                }
                if (plugin.getCharacterManager().getCharacter(lTarget).hasEffect("Blight")) {
                    continue;
                }
                addSpellTarget(target, applyHero);
                Skill.damageEntity((LivingEntity) target, applier.getPlayer(), tickDamage, DamageCause.MAGIC);
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 21000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        damage = damage * duration / period;
        return getDescription().replace("$1", damage + "").replace("$2", duration / 1000 + "");
    }
}
