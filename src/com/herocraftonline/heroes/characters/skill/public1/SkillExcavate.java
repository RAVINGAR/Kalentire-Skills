package com.herocraftonline.heroes.characters.skill.public1;

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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillExcavate extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillExcavate(Heroes plugin) {
        super(plugin, "Excavate");
        this.setDescription("You gain a increased digging speed, and instant breaking of dirt for $1 seconds.");
        this.setUsage("/skill excavate");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill excavate");
        this.setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_EARTH, SkillType.SILENCEABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", 2);
        node.set(SkillSetting.DURATION.node(), 0);
        node.set(SkillSetting.DURATION_INCREASE.node(), 100);
        node.set("apply-text", "%hero% begins excavating!");
        node.set("expire-text", "%hero% is no longer excavating!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, "apply-text", "%hero% begins excavating!").replace("%hero%", "$1");
        this.expireText = SkillConfigManager.getRaw(this, "expire-text", "%hero% is no longer excavating!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        this.broadcastExecuteText(hero);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 0, false);
        duration += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE, 100, false) * hero.getHeroLevel(this));
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
        if (multiplier > 20) {
            multiplier = 20;
        }
        hero.addEffect(new ExcavateEffect(this, hero.getPlayer(), duration, multiplier));

        return SkillResult.NORMAL;
    }

    public class ExcavateEffect extends ExpirableEffect {

        public ExcavateEffect(Skill skill, Player applier, long duration, int amplifier) {
            super(skill, "Excavate", applier, duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, (int) (20 * duration / 1000), amplifier), false);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), SkillExcavate.this.applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), SkillExcavate.this.expireText, player.getDisplayName());
        }
    }

    public class SkillBlockListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onBlockDamage(BlockDamageEvent event) {
            if (event.isCancelled() || !SkillExcavate.this.isExcavatable(event.getBlock().getType())) {
                return;
            }

            final Hero hero = SkillExcavate.this.plugin.getCharacterManager().getHero(event.getPlayer());
            if (!hero.hasEffect("Excavate")) {
                return;
            }

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
            //FIXME What is the replacement
            //case SOIL:
            case NETHERRACK:
                return true;
            default:
                return false;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 0, false);
        duration += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE, 100, false) * hero.getHeroLevel(this));
        return this.getDescription().replace("$1", (duration / 1000) + "");
    }
}
