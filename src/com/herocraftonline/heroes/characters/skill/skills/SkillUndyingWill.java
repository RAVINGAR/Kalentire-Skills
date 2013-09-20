package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillUndyingWill extends ActiveSkill {
    private String expireText;

    public SkillUndyingWill(Heroes plugin) {
        super(plugin, "UndyingWill");
        setDescription("You are overcome with an undying will to survive. You cannot be killed for the next $1 seconds. While active, you shrug off disabling effects every $2 seconds.");
        setUsage("/skill undyingwill");
        setArgumentRange(0, 0);
        setIdentifiers("skill undyingwill");
        setTypes(SkillType.DISABLE_COUNTERING, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_PHYSICAL);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    public String getDescription(Hero hero) {
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(4500), false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, Integer.valueOf(500), false);

        String actualDuration = Util.decFormat.format(duration / 1000.0);
        String actualPeriod = Util.decFormat.format(period / 1000.0);

        return getDescription().replace("$1", actualDuration).replace("$2", actualPeriod);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), Integer.valueOf(4500));
        node.set(SkillSetting.PERIOD.node(), Integer.valueOf(500));
        node.set(SkillSetting.USE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is overcome with an undying will!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero%'s will returns to normal.");

        return node;
    }

    public void init() {
        super.init();

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero%'s will returns to normal.").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(4500), false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(4500), false);
        hero.addEffect(new UndyingWillEffect(this, player, period, duration));

        player.getWorld().playSound(player.getLocation(), Sound.WOLF_GROWL, 0.5F, 0.1F);

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {
        public SkillHeroListener() {}

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {

            // If our target isn't a Living Entity exit
            if (!(event.getEntity() instanceof LivingEntity))
                return;

            Entity entity = event.getEntity();
            if (entity instanceof Player) {
                Player player = (Player) entity;
                Hero hero = plugin.getCharacterManager().getHero(player);

                // Don't let them go below 1HP.
                if (hero.hasEffect("UndyingWill")) {
                    double currentHealth = player.getHealth();

                    if (event.getDamage() > currentHealth) {
                        if (currentHealth != 1.0)
                            player.setHealth(1.0);

                        event.setDamage(0.0);
                    }
                }
            }
        }
    }

    public class UndyingWillEffect extends PeriodicExpirableEffect {
        public UndyingWillEffect(Skill skill, Player applifer, long period, long duration) {
            super(skill, "UndyingWill", applifer, period, duration, null, expireText);

            types.add(EffectType.PHYSICAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            removeDisables(hero);
        }

        @Override
        public void tickHero(Hero hero) {
            removeDisables(hero);
        }

        @Override
        public void tickMonster(Monster monster) {}

        private void removeDisables(Hero hero) {
            for (Effect effect : hero.getEffects()) {
                if (effect.isType(EffectType.HARMFUL)) {
                    if (effect.isType(EffectType.DISABLE) || effect.isType(EffectType.SLOW) ||
                            effect.isType(EffectType.VELOCITY_DECREASING) || effect.isType(EffectType.WALK_SPEED_DECREASING) ||
                            effect.isType(EffectType.STUN) || effect.isType(EffectType.ROOT)) {
                        hero.removeEffect(effect);
                    }
                }
            }
        }
    }
}