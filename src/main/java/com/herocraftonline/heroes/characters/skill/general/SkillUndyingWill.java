package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillUndyingWill extends ActiveSkill {
    private String expireText;

    public SkillUndyingWill(final Heroes plugin) {
        super(plugin, "UndyingWill");
        setDescription("You are overcome with an undying will to survive. You cannot be killed for the next $1 second(s).");
        setUsage("/skill undyingwill");
        setArgumentRange(0, 0);
        setIdentifiers("skill undyingwill");
        setTypes(SkillType.DISABLE_COUNTERING, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_PHYSICAL);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4500, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 500, false);

        final String actualDuration = Util.decFormat.format(duration / 1000.0);
        final String actualPeriod = Util.decFormat.format(period / 1000.0);

        return getDescription().replace("$1", actualDuration).replace("$2", actualPeriod);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 4500);
        node.set(SkillSetting.PERIOD.node(), 500);
        node.set(SkillSetting.USE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is overcome with an undying will!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s will returns to normal.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s will returns to normal.").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4500, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 500, false);
        hero.addEffect(new UndyingWillEffect(this, player, period, duration));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_GROWL, 0.5F, 0.1F);

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {
        public SkillHeroListener() {
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(final EntityDamageEvent event) {

            // If our target isn't a Living Entity exit
            if (!(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            final Entity entity = event.getEntity();
            if (entity instanceof Player) {
                final Player player = (Player) entity;
                final Hero hero = plugin.getCharacterManager().getHero(player);

                // Don't let them go below 1HP.
                if (hero.hasEffect("UndyingWill")) {
                    final double currentHealth = player.getHealth();

                    if (event.getDamage() > currentHealth) {
                        if (currentHealth != 1.0) {
                            player.setHealth(1.0);
                        }
                        //player.getWorld().spigot().playEffect(player.getLocation(), Effect.COLOURED_DUST, 0, 0, 0.5F, 1.0F, 0.5F, 0.0F, 25, 16);
                        player.getWorld().spawnParticle(Particle.REDSTONE, player.getLocation(), 25, 0.5, 1, 0.5, 0, new Particle.DustOptions(Color.RED, 1));
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5F, 0.8F);

                        event.setDamage(0.0);
                    }
                }
            }
        }
    }

    public class UndyingWillEffect extends ExpirableEffect {
        public UndyingWillEffect(final Skill skill, final Player applifer, final long period, final long duration) {
            super(skill, "UndyingWill", applifer, duration, null, expireText);

            types.add(EffectType.PHYSICAL);
        }
    }
}