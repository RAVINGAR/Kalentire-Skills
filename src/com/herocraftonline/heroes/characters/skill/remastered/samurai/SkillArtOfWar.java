package com.herocraftonline.heroes.characters.skill.remastered.samurai;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import ru.xezard.glow.data.glow.Glow;
import ru.xezard.glow.data.glow.IGlow;
import ru.xezard.glow.data.glow.manager.GlowsManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SkillArtOfWar extends ActiveSkill {

    private boolean glowApiLoaded;

    public SkillArtOfWar(Heroes plugin) {
        super(plugin, "ArtOfWar");
        setDescription("TODO description");
        setUsage("/skill artofwar");
        setIdentifiers("skill artofwar");
        setArgumentRange(0, 0);
        setTypes(SkillType.SILENCEABLE, SkillType.BUFFING, SkillType.AGGRESSIVE, SkillType.NO_SELF_TARGETTING);
        //Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);

        if (Bukkit.getServer().getPluginManager().getPlugin("XGlow") != null) {
            glowApiLoaded = true;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        //config.set(SkillSetting.DURATION.node(), 120000);
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        LivingEntity target = null;
        for (Entity nearbyEntity : player.getNearbyEntities(5, 5, 5)) {
            // Get first living entity in range
            if (nearbyEntity instanceof LivingEntity) {
                target = (LivingEntity) nearbyEntity;
                break;
            }
        }

        if (target == null) {
            player.sendMessage("No valid target in range to test glowing.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        // Just doing testing for glow apis at the moment
        if (glowApiLoaded) {
            final Optional<IGlow> glowFromEntity = GlowsManager.getInstance().getGlowByEntity(target);
            if (glowFromEntity.isPresent()) {
                // has glow, lets turn it off
                glowFromEntity.get().destroy();
//                hero.getPlayer().getNearbyEntities(5,5,5)
            } else {
                // add glow
                final Glow glow = Glow.builder()
                        .animatedColor(ChatColor.WHITE)
                        .name("test")
                        .build();
                glow.addHolders(target); // add glow to target
                glow.display(player); // allow player to see
            }

//            GlowsManager.getInstance().getGlowByEntity()
        } else {
            player.sendMessage("XGlow API isn't loaded, add it or you cant make targets glow");
        }

        return SkillResult.NORMAL;
    }

//    @Override
//    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
//
//        // Just doing testing for glow apis at the moment
//        if (glowApiLoaded) {
//            final Optional<IGlow> glowFromEntity = GlowsManager.getInstance().getGlowByEntity(target);
//            if (glowFromEntity.isPresent()) {
//                // has glow, lets turn it off
//                glowFromEntity.get().destroy();
////                hero.getPlayer().getNearbyEntities(5,5,5)
//            } else {
//                // add glow
//                final Glow glow = Glow.builder()
//                        .animatedColor(ChatColor.WHITE)
//                        .name("test")
//                        .build();
//                glow.addHolders(target); // add glow to target
//                glow.display(hero.getPlayer()); // allow player to see
//            }
//
////            GlowsManager.getInstance().getGlowByEntity()
//        } else {
//            hero.getPlayer().sendMessage("XGlow API isn't loaded, add it or you cant make targets glow");
//        }
//
//        return SkillResult.NORMAL;
//    }


//    @Override
//    public void apply(Hero hero) {
//        // Note we don't want the default passive effect, we're making our own with a custom constructor
//        hero.addEffect(new ArtOfWarEffect(this, hero.getPlayer()));
//    }

//    public class SkillEntityListener implements Listener {
//        private final PassiveSkill skill;
//
//        public SkillEntityListener(PassiveSkill skill) {
//            this.skill = skill;
//        }
//
//    }

//    public class ArtOfWarEffect extends PassiveEffect {
//        private static final long cooldownMilliseconds = 5000L;
//
//        public ArtOfWarEffect(Skill skill, Player applier) {
//            super(skill, applier, null);
//
//            types.add(EffectType.BENEFICIAL);
//            types.add(EffectType.MAGIC);
//        }
//
//        @Override
//        public void removeFromHero(Hero hero) {
//            super.removeFromHero(hero);
//        }
//
//    }
}
