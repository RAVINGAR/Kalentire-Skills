package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.QuickenEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillAccelerando extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillAccelerando(Heroes plugin) {
        super(plugin, "Accelerando");
        setDescription("You song boons movement speed boost to all nearby party members for $1 seconds.");
        setUsage("/skill accelerando");
        setArgumentRange(0, 0);
        setIdentifiers("skill accelerando", "skill accelerate");
        setTypes(SkillType.BUFF, SkillType.MOVEMENT, SkillType.SILENCABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", 2);
        node.set(Setting.DURATION.node(), 10000);
        node.set(Setting.RADIUS.node(), 15);
        node.set("apply-text", "%hero% gained a burst of speed!");
        node.set("expire-text", "%hero% returned to normal speed!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%hero% gained a burst of speed!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%hero% returned to normal speed!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.NOTE_BASS_DRUM , 12.0F, 1.0F);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.NOTE_BASS_DRUM , 9.0F, 1.0F);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.NOTE_BASS_DRUM , 10.0F, 1.0F);
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 300000, false);
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
        if (multiplier > 20) {
            multiplier = 20;
        }
        QuickenEffect qEffect = new QuickenEffect(this, getName(), duration, multiplier, applyText, expireText);
        if (!hero.hasParty()) {
            hero.addEffect(qEffect);
            return SkillResult.NORMAL;
        }
        Player player = hero.getPlayer();
        int radius = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 15, false);
        int rSquared = radius * radius;
        Location loc = player.getLocation();
        //Apply the effect to all party members
        for (Hero tHero : hero.getParty().getMembers()) {
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
    
    public class SkillEntityListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || event.getDamage() == 0 || !(event.getEntity() instanceof Player)) {
                return;
            }
            
            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect(getName())) {
                hero.removeEffect(hero.getEffect(getName()));
            }
        }
    }
    
    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}