package com.herocraftonline.heroes.characters.skill.reborn.duelist;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.effects.common.interfaces.HealthRegainReduction;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class SkillMortalWound extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillMortalWound(Heroes plugin) {
        super(plugin, "MortalWound");
        setDescription("You strike your target reducing healing by $1%, and causing them to bleed for $2 damage over $3 second(s).");
        setUsage("/skill mortalwound");
        setArgumentRange(0, 0);
        setIdentifiers("skill mortalwound", "skill mwound");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double heal = 1 - SkillConfigManager.getUseSetting(hero, this, "heal-multiplier", .5, true);
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        double period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        return getDescription().replace("$1", heal * 100 + "").replace("$2", damage * duration / period + "").replace("$3", duration / 1000 + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("weapons", Util.swords);
        node.set(SkillSetting.DURATION.node(), 12000);
        node.set(SkillSetting.PERIOD.node(), 3000);
        node.set("heal-multiplier", .5);
        node.set("tick-damage", 1);
        node.set(SkillSetting.MAX_DISTANCE.node(), 2);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% has been mortally wounded by %hero%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has recovered from their mortal wound!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% has been mortally wounded by %hero%!").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has recovered from their mortal wound!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        HeroClass heroClass = hero.getHeroClass();

        Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords).contains(item.name())) {
            player.sendMessage("You can't use Mortal Wound with that weapon!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        double damage = heroClass.getItemDamage(item) == null ? 0 : heroClass.getItemDamage(item).getScaled(hero); //getScaled(hero)
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, true);
        double tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        double healMultiplier = SkillConfigManager.getUseSetting(hero, this, "heal-multiplier", 0.5, true);
        plugin.getCharacterManager().getCharacter(target).addEffect(new MortalWoundEffect(this, player, period, duration, tickDamage, healMultiplier));

        //player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 0.5, 0), 25, 0, 0, 0, 1);
        player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.MOBSPAWNER_FLAMES, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_PLAYER_HURT , 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class MortalWoundEffect extends PeriodicDamageEffect implements HealthRegainReduction {

        private double healMultiplier;

        public MortalWoundEffect(Skill skill, Player applier, long period, long duration, double tickDamage, double healMultiplier) {
            super(skill, "MortalWoundEffect", applier, period, duration, tickDamage, applyText, expireText);
            this.healMultiplier = healMultiplier;

            types.add(EffectType.BLEED);
            types.add(EffectType.HARMFUL);
        }

        public double getHealMultiplier() {
            return healMultiplier;
        }

        @Override
        public Double getDelta() {
            return healMultiplier;
        }

        @Override
        public void setDelta(Double delta) {
            this.healMultiplier = delta;
        }
    }

    public class SkillEntityListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityRegainHealth(EntityRegainHealthEvent event) {
            if (!(event.getEntity() instanceof Player)) {
                return;
            }

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("MortalWoundEffect")) {
                MortalWoundEffect mwEffect = (MortalWoundEffect) hero.getEffect("MortalWoundEffect");
                if (mwEffect != null) {
                    event.setAmount((event.getAmount() * mwEffect.getHealMultiplier()));
                }
            }
        }
        
        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onHeroRegainHealth(HeroRegainHealthEvent event) {
            if (event.getHero().hasEffect("MortalWoundEffect")) {
                MortalWoundEffect mwEffect = (MortalWoundEffect) event.getHero().getEffect("MortalWoundEffect");
                if (mwEffect != null) {
                    event.setDelta((event.getDelta() * mwEffect.getHealMultiplier()));
                }
            }
        }
    }
}
