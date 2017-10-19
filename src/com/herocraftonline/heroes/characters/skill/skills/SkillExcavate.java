package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;

public class SkillExcavate extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillExcavate(Heroes plugin) {
        super(plugin, "Excavate");
        setDescription("You gain a increased digging speed, and instant breaking of dirt for $1 seconds.");
        setUsage("/skill excavate");
        setArgumentRange(0, 0);
        setIdentifiers("skill excavate");
        setTypes(SkillType.BUFFING, SkillType.SILENCEABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 0, false);
        duration += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_LEVEL, 100, false) * hero.getLevel(this));

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", 3);
        node.set(SkillSetting.DURATION.node(), 0);
        node.set(SkillSetting.DURATION_INCREASE_PER_LEVEL.node(), 200);
        node.set("apply-text", ChatComponents.GENERIC_SKILL + "%hero% begins excavating!");
        node.set("expire-text", ChatComponents.GENERIC_SKILL + "%hero% is no longer excavating!");
        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, "apply-text", ChatComponents.GENERIC_SKILL + "%hero% begins excavating!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, "expire-text", ChatComponents.GENERIC_SKILL + "%hero% is no longer excavating!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 0, false);
        duration += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_LEVEL, 100, false) * hero.getLevel(this));

        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 3, false);
        if (multiplier > 20) {
            multiplier = 20;
        }

        hero.addEffect(new ExcavateEffect(this, player, duration, multiplier));
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_EXPERIENCE_ORB_PICKUP.value(), 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class ExcavateEffect extends ExpirableEffect {

        public ExcavateEffect(Skill skill, Player applier, long duration, int amplifier) {
            super(skill, "Excavate", applier, duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, (int) (duration / 1000) * 20, amplifier), false);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
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
}
