package com.herocraftonline.heroes.characters.skill.pack7;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
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
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillAtrophy extends TargettedSkill {
    private String applyText;
    private String expireText;

    public SkillAtrophy(Heroes plugin) {
        super(plugin, "Atrophy");
        setDescription("You begin to decay your targets muscles dealing $1 disease damage over $2 second(s).");
        setUsage("/skill atrophy");
        setArgumentRange(0, 0);
        setIdentifiers("skill atrophy");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DISEASE, SkillType.SILENCEABLE,
                SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.DURATION, false);
        int period = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.PERIOD, false);
        double tickDamage = SkillConfigManager.getScaledUseSettingDouble(hero, this, "tick-damage", false);

        return getDescription()
                .replace("$1", Util.decFormat.format(tickDamage * ((double) duration / (double) period)))
                .replace("$2", Util.decFormat.format(duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 7);
        config.set(SkillSetting.DURATION.node(), 20000);
        config.set(SkillSetting.PERIOD.node(), 2500);
        config.set(SkillSetting.DAMAGE_TICK.node(), 17d);
        config.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 0.17);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s flesh has begun to rot!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer rotting alive!");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%target%'s flesh has begun to rot!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% is no longer rotting alive!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        int duration = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.DURATION, false);
        int period = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.PERIOD, true);

        double tickDamage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE_TICK, false);

        final CharacterTemplate targetCharacter = plugin.getCharacterManager().getCharacter(target);
        targetCharacter.addEffect(new AtrophyEffect(this, player, duration, period, tickDamage));

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_HURT, 0.8F, 2.0F);

        return SkillResult.NORMAL;
    }

    public class AtrophyEffect extends PeriodicDamageEffect {

        public AtrophyEffect(Skill skill, Player applier, long duration, long period, double tickDamage) {
            super(skill, "Atrophy", applier, period, duration, tickDamage);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.DISEASE);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            playDiseaseParticleEffectOnTarget(monster.getEntity());
            broadcast(monster.getEntity().getLocation(), applyText, CustomNameManager.getName(monster), applier.getDisplayName());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            playDiseaseParticleEffectOnTarget(player);
            broadcast(player.getLocation(), applyText, player.getDisplayName(), applier.getDisplayName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), expireText, CustomNameManager.getName(monster).toLowerCase(), applier.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName(), applier.getDisplayName());
        }

        public void playDiseaseParticleEffectOnTarget(LivingEntity entity) {
            new BukkitRunnable() {
                private double time = 0;

                @Override
                public void run() {
                    Location location = entity.getLocation().add(0, 0.5, 0);
                    if (time < 1.0) {
                        //entity.getWorld().spigot().playEffect(location, Effect.TILE_BREAK, Material.SLIME_BLOCK.getId(), 0, 0.5F, 0.5F, 0.5F, 0.1f, 10, 16);
                        entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 10, 0.5, 0.5, 0.5, 0.1, Bukkit.createBlockData(Material.SLIME_BLOCK));
                    } else {
                        cancel();
                    }
                    time += 0.02;
                }
            }.runTaskTimer(plugin, 1, 6);
        }
    }
}