package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
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
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseConeShot;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillAtrophy extends SkillBaseConeShot {
    private String applyText;
    private String expireText;

    public SkillAtrophy(final Heroes plugin) {
        super(plugin, "Atrophy");
        setDescription("Spread a disease to your target(s), dealing $1 disease damage to each over $2 second(s).");
        setUsage("/skill atrophy");
        setArgumentRange(0, 0);
        setIdentifiers("skill atrophy");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DISEASE, SkillType.SILENCEABLE,
                SkillType.DAMAGING, SkillType.MULTI_GRESSIVE, SkillType.AREA_OF_EFFECT);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.DURATION, false);
        final int period = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.PERIOD, false);
        final double tickDamage = SkillConfigManager.getScaledUseSettingDouble(hero, this, "tick-damage", false);

        return getDescription()
                .replace("$1", Util.decFormat.format(tickDamage * ((double) duration / (double) period)))
                .replace("$2", Util.decFormat.format(duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 4.0);
        config.set(SkillSetting.RADIUS.node(), 2D);
        config.set("cone-travel-delay", 1);
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
                ChatComponents.GENERIC_SKILL + "%target%'s flesh has begun to rot!").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% is no longer rotting alive!").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    protected void effectTarget(final Hero hero, final Skill skill, final CharacterTemplate target) {
        final int duration = SkillConfigManager.getUseSettingInt(hero, skill, SkillSetting.DURATION, false);
        final int period = SkillConfigManager.getUseSettingInt(hero, skill, SkillSetting.PERIOD, true);
        final double tickDamage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE_TICK, false);

        final Player applier = hero.getPlayer();
        target.addEffect(new AtrophyEffect(this, applier, duration, period, tickDamage));

        final LivingEntity tEntity = target.getEntity();
        tEntity.getWorld().playSound(tEntity.getLocation(), Sound.ENTITY_ZOMBIE_HURT, 0.8F, 2.0F);
    }

    @Override
    protected void spawnParticleEffects(final World world, final Location location) {
        //FIXME this what we want? just using the effect's disease effect for now
        //world.spigot().playEffect(location, Effect.TILE_BREAK, Material.SLIME_BLOCK.getId(), 0, 0.5F, 0.5F, 0.5F, 0.1f, 10, 16);
        world.spawnParticle(Particle.BLOCK_CRACK, location, 10, 0.5, 0.5, 0.5, 0.1, Bukkit.createBlockData(Material.SLIME_BLOCK));
    }

    @Override
    protected void applySoundEffects(final World world, final Location location) {
        world.playSound(location, Sound.ENTITY_ZOMBIE_AMBIENT, 0.8F, 2.0F);
    }

    public class AtrophyEffect extends PeriodicDamageEffect {

        public AtrophyEffect(final Skill skill, final Player applier, final long duration, final long period, final double tickDamage) {
            super(skill, "Atrophy", applier, period, duration, tickDamage);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.DISEASE);
        }

        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);
            playDiseaseParticleEffectOnTarget(monster);
            broadcast(monster.getEntity().getLocation(), applyText, CustomNameManager.getName(monster), applier.getDisplayName());
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            playDiseaseParticleEffectOnTarget(hero);
            broadcast(player.getLocation(), applyText, player.getDisplayName(), applier.getDisplayName());
        }

        @Override
        public void removeFromMonster(final Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), expireText, CustomNameManager.getName(monster).toLowerCase(), applier.getDisplayName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName(), applier.getDisplayName());
        }

        public void playDiseaseParticleEffectOnTarget(final CharacterTemplate character) {
            final LivingEntity entity = character.getEntity();
            new BukkitRunnable() {
                private double time = 0;

                @Override
                public void run() {
                    if (!character.hasEffect("Atrophy") || entity.isDead()) {
                        cancel();
                    }

                    final Location location = entity.getLocation().add(0, 0.5, 0);
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