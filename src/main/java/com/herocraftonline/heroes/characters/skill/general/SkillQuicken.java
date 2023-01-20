package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.QuickenEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillQuicken extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillQuicken(final Heroes plugin) {
        super(plugin, "Quicken");
        setDescription("You grant a movement speed boost to all nearby party members for $1 second(s).");
        setUsage("/skill quicken");
        setArgumentRange(0, 0);
        setIdentifiers("skill quicken", "skill quick");
        setTypes(SkillType.BUFFING, SkillType.MOVEMENT_INCREASING, SkillType.SILENCEABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", 2);
        node.set(SkillSetting.DURATION.node(), 300000);
        node.set(SkillSetting.RADIUS.node(), 15);
        node.set("apply-text", "%hero% gained a burst of speed!");
        node.set("expire-text", "%hero% returned to normal speed!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% gained a burst of speed!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% returned to normal speed!").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        broadcastExecuteText(hero);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 300000, false);
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
        if (multiplier > 20) {
            multiplier = 20;
        }
        final QuickenEffect qEffect = new QuickenEffect(this, getName(), hero.getPlayer(), duration, multiplier, applyText, expireText);
        if (!hero.hasParty()) {
            hero.addEffect(qEffect);
            return SkillResult.NORMAL;
        }
        final Player player = hero.getPlayer();
        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 15, false);
        final int rSquared = radius * radius;
        final Location loc = player.getLocation();
        //Apply the effect to all party members
        for (final Hero tHero : hero.getParty().getMembers()) {
            if (!tHero.getPlayer().getWorld().equals(player.getWorld())) {
                continue;
            }

            if (loc.distanceSquared(tHero.getPlayer().getLocation()) > rSquared) {
                continue;
            }

            tHero.addEffect(qEffect);
        }
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }

    public class SkillEntityListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(final EntityDamageEvent event) {
            if (event.isCancelled() || event.getDamage() == 0 || !(event.getEntity() instanceof Player)) {
                return;
            }

            final Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect(getName())) {
                hero.removeEffect(hero.getEffect(getName()));
            }
        }
    }
}
