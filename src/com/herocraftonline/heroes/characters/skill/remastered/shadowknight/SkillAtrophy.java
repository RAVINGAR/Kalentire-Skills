package com.herocraftonline.heroes.characters.skill.remastered.shadowknight;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.*;
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
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DISEASE, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(20000), false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, Integer.valueOf(2500), false);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", Integer.valueOf(17), false);
        double tickDamageIncrease = hero.getAttributeValue(AttributeType.INTELLECT) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(0.17), false);
        tickDamage += tickDamageIncrease;

        String formattedDamage = Util.decFormat.format(tickDamage * ((double) duration / (double) period));
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(7));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(20000));
        node.set(SkillSetting.PERIOD.node(), Integer.valueOf(2500));
        node.set(SkillSetting.DAMAGE_TICK.node(), Double.valueOf(17));
        node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.17));
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s flesh has begun to rot!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer rotting alive!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s flesh has begun to rot!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% is no longer rotting alive!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(20000), false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, Integer.valueOf(2500), true);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, Double.valueOf(17), false);
        double tickDamageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, Double.valueOf(0.17), false);
        tickDamage += (tickDamageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        plugin.getCharacterManager().getCharacter(target).addEffect(new AtrophyEffect(this, player, duration, period, tickDamage));

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
            final LivingEntity p = monster.getEntity();
            new BukkitRunnable() {            
                
                private double time = 0;

                @SuppressWarnings("deprecation")
                @Override
                public void run() 
                {
                	Location location = p.getLocation().add(0, 0.5, 0);
                    if (time < 1.0) 
                    {
                        //p.getWorld().spigot().playEffect(location, Effect.TILE_BREAK, Material.SLIME_BLOCK.getId(), 0, 0.5F, 0.5F, 0.5F, 0.1f, 10, 16);
                        p.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 10, 0.5, 0.5, 0.5, 0.1, Bukkit.createBlockData(Material.SLIME_BLOCK));
                    } 
                    else 
                    {
                        cancel();
                    }
                    time += 0.02;
                }
            }.runTaskTimer(plugin, 1, 6);
            broadcast(monster.getEntity().getLocation(), applyText, CustomNameManager.getName(monster), applier.getDisplayName());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            final Player p = player;
            new BukkitRunnable() {            
                
                private double time = 0;

                @SuppressWarnings("deprecation")
                @Override
                public void run() 
                {
                	Location location = p.getLocation().add(0, 0.5, 0);
                    if (time < 1.0) 
                    {
                        //p.getWorld().spigot().playEffect(location, Effect.TILE_BREAK, Material.SLIME_BLOCK.getId(), 0, 0.5F, 0.5F, 0.5F, 0.1f, 10, 16);
                        p.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 10, 0.5, 0.5, 0.5, 0.1, Bukkit.createBlockData(Material.SLIME_BLOCK));
                    } 
                    else 
                    {
                        cancel();
                    }
                    time += 0.02;
                }
            }.runTaskTimer(plugin, 1, 6);
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
    }
}