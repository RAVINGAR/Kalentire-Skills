package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillExcavate extends ActiveSkill implements Listenable {

    private final Listener listener;
    private String applyText;
    private String expireText;

    public SkillExcavate(final Heroes plugin) {
        super(plugin, "Excavate");
        setDescription("You gain a increased digging speed, and instant breaking of dirt for $1 second(s).");
        setUsage("/skill excavate");
        setArgumentRange(0, 0);
        setIdentifiers("skill excavate");
        setTypes(SkillType.BUFFING, SkillType.SILENCEABLE);
        listener = new SkillBlockListener();
    }

    @Override
    public String getDescription(final Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 0, false);
        duration += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_LEVEL, 100, false) * hero.getHeroLevel(this));

        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
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

        applyText = SkillConfigManager.getRaw(this, "apply-text", ChatComponents.GENERIC_SKILL + "%hero% begins excavating!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, "expire-text", ChatComponents.GENERIC_SKILL + "%hero% is no longer excavating!").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 0, false);
        duration += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_LEVEL, 100, false) * hero.getHeroLevel(this));

        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 3, false);
        if (multiplier > 20) {
            multiplier = 20;
        }

        hero.addEffect(new ExcavateEffect(this, player, duration, multiplier));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    private boolean isExcavatable(final Material m) {
        switch (m) {
            case DIRT:
            case GRASS:
            case GRAVEL:
            case SAND:
            case CLAY:
            case SNOW_BLOCK:
            case SNOW:
            case SOUL_SAND:
                //FIXME What do here
                //case SOIL:
            case NETHERRACK:
                return true;
            default:
                return false;
        }
    }

    public class ExcavateEffect extends ExpirableEffect {

        public ExcavateEffect(final Skill skill, final Player applier, final long duration, final int amplifier) {
            super(skill, "Excavate", applier, duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, (int) (duration / 1000) * 20, amplifier), false);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }
    }

    public class SkillBlockListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onBlockDamage(final BlockDamageEvent event) {
            if (event.isCancelled() || !isExcavatable(event.getBlock().getType())) {
                return;
            }

            final Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (!hero.hasEffect("Excavate")) {
                return;
            }

            //Since this block is excavatable, and the hero has the effect - lets instabreak it
            event.setInstaBreak(true);
        }
    }
}
