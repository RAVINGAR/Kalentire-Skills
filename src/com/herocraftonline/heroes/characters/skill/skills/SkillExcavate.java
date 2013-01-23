package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillExcavate extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillExcavate(Heroes plugin) {
        super(plugin, "Excavate");
        setDescription("You gain a increased digging speed, and instant breaking of dirt for $1 seconds.");
        setUsage("/skill excavate");
        setArgumentRange(0, 0);
        setIdentifiers("skill excavate");
        setTypes(SkillType.BUFF, SkillType.EARTH, SkillType.SILENCABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(), plugin);
    }
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", 2);
        node.set(Setting.DURATION.node(), 0);
        node.set(Setting.DURATION_INCREASE.node(), 100);
        node.set("apply-text", "%hero% begins excavating!");
        node.set("expire-text", "%hero% is no longer excavating!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, "apply-text", "%hero% begins excavating!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, "expire-text", "%hero% is no longer excavating!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 0, false);
        duration += (SkillConfigManager.getUseSetting(hero, this, Setting.DURATION_INCREASE, 100, false) * hero.getSkillLevel(this));
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
        if (multiplier > 20) {
            multiplier = 20;
        }
        hero.addEffect(new ExcavateEffect(this, duration, multiplier));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ORB_PICKUP , 0.8F, 1.0F); 
        return SkillResult.NORMAL;
    }

    public class ExcavateEffect extends ExpirableEffect {

        public ExcavateEffect(Skill skill, long duration, int amplifier) {
            super(skill, "Excavate", duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            addMobEffect(3, (int) (duration / 1000) * 20, amplifier, false);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }

    public class SkillBlockListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onBlockDamage(BlockDamageEvent event) {
            if (event.isCancelled() || !isExcavatable(event.getBlock().getType())) {
                return;
            }

            Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (!hero.hasEffect("Excavate"))
                return;

            //Since this block is excavatable, and the hero has the effect - lets instabreak it
            event.setInstaBreak(true);
        }
    }

    private boolean isExcavatable(Material m) {
        switch (m) {
        case DIRT:
        case GRASS:
        case GRAVEL:
        case SAND:
        case CLAY:
        case SNOW_BLOCK:
        case SNOW:
        case SOUL_SAND:
        case SOIL:
        case NETHERRACK:
            return true;
        default: 
            return false;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 0, false);
        duration += (SkillConfigManager.getUseSetting(hero, this, Setting.DURATION_INCREASE, 100, false) * hero.getSkillLevel(this));
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
