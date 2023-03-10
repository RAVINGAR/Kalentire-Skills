package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillDecay extends TargettedSkill {
    private String applyText;
    private String expireText;

    public SkillDecay(final Heroes plugin) {
        super(plugin, "Decay");
        setDescription("You disease your target dealing $1 disease damage over $2 second(s).");
        setUsage("/skill decay");
        setArgumentRange(0, 0);
        setIdentifiers("skill decay");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DISEASE, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 17, false);
        final double tickDamageIncrease = hero.getAttributeValue(AttributeType.INTELLECT) * SkillConfigManager.getUseSetting(hero,
                this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.17, false);
        tickDamage += tickDamageIncrease;

        final String formattedDamage = Util.decFormat.format(tickDamage * ((double) duration / (double) period));
        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 7);
        node.set(SkillSetting.DURATION.node(), 20000);
        node.set(SkillSetting.PERIOD.node(), 2500);
        node.set(SkillSetting.DAMAGE_TICK.node(), (double) 17);
        node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 0.17);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s flesh has begun to rot!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer decaying alive!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s flesh has begun to rot!").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% is no longer decaying alive!").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, true);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, (double) 17, false);
        final double tickDamageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 0.17, false);
        tickDamage += (tickDamageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        plugin.getCharacterManager().getCharacter(target).addEffect(new DecayEffect(this, player, duration, period, tickDamage));

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_HURT, 0.8F, 2.0F);

        return SkillResult.NORMAL;
    }

    public class DecayEffect extends PeriodicDamageEffect {

        public DecayEffect(final Skill skill, final Player applier, final long duration, final long period, final double tickDamage) {
            super(skill, "Decay", applier, period, duration, tickDamage);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.DISEASE);
        }

        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);
            final LivingEntity p = monster.getEntity();
            new BukkitRunnable() {

                private double time = 0;

                @Override
                public void run() {
                    final Location location = p.getLocation().add(0, 0.5, 0);
                    if (time < 1.0) {
                        //p.getWorld().spigot().playEffect(location, Effect.TILE_BREAK, Material.SLIME_BLOCK.getId(), 0, 0.5F, 0.5F, 0.5F, 0.1f, 10, 16);
                        p.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 10, 0.5, 0.5, 0.5, 0.1, Bukkit.createBlockData(Material.SLIME_BLOCK));
                    } else {
                        cancel();
                    }
                    time += 0.02;
                }
            }.runTaskTimer(plugin, 1, 6);
            broadcast(monster.getEntity().getLocation(), "    " + applyText, CustomNameManager.getName(monster), applier.getName());
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            final Player p = player;
            new BukkitRunnable() {

                private double time = 0;

                @Override
                public void run() {
                    final Location location = p.getLocation().add(0, 0.5, 0);
                    if (time < 1.0) {
                        //p.getWorld().spigot().playEffect(location, Effect.TILE_BREAK, Material.SLIME_BLOCK.getId(), 0, 0.5F, 0.5F, 0.5F, 0.1f, 10, 16);
                        p.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 10, 0.5, 0.5, 0.5, 0.1, Bukkit.createBlockData(Material.SLIME_BLOCK));
                    } else {
                        cancel();
                    }
                    time += 0.02;
                }
            }.runTaskTimer(plugin, 1, 6);
            broadcast(player.getLocation(), "    " + applyText, player.getName(), applier.getName());
        }

        @Override
        public void removeFromMonster(final Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, CustomNameManager.getName(monster), applier.getName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName(), applier.getName());
        }
    }
}
