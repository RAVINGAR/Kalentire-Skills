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
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;

public class SkillFlameshield extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillFlameshield(final Heroes plugin) {
        super(plugin, "Flameshield");
        setDescription("You become resistant to fire for $1 second(s).");
        setUsage("/skill flameshield");
        setArgumentRange(0, 0);
        setIdentifiers("skill flameshield", "skill fshield");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.BUFFING);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 8000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% conjured a shield of flames!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% lost his shield of flames!");
        node.set("skill-block-text", ChatComponents.GENERIC_SKILL + "%name%'s flameshield has blocked %hero%'s %skill%.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% conjured a shield of flames!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% lost his shield of flames!").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    public ArrayList<Location> circle(final Location centerPoint, final int particleAmount, final double circleRadius) {
        final World world = centerPoint.getWorld();

        final double increment = (2 * Math.PI) / particleAmount;

        final ArrayList<Location> locations = new ArrayList<>();

        for (int i = 0; i < particleAmount; i++) {
            final double angle = i * increment;
            final double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
            final double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
            locations.add(new Location(world, x, centerPoint.getY(), z));
        }
        return locations;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();
        broadcastExecuteText(hero);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        hero.addEffect(new FlameshieldEffect(this, player, duration));

        final ArrayList<Location> locations = circle(player.getLocation(), 72, 1.5);
        for (final Location location : locations) {
            //player.getWorld().spigot().playEffect(locations.get(i), org.bukkit.Effect.FLAME, 0, 0, 0, 1.2F, 0, 0, 6, 16);
            player.getWorld().spawnParticle(Particle.FLAME, location, 6, 0, 1.2, 0, 0);
        }

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.4F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class FlameshieldEffect extends ExpirableEffect {

        public FlameshieldEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, "Flameshield", applier, duration);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.RESIST_FIRE);
            types.add(EffectType.MAGIC);

            addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, (int) ((duration * 20) / 1000), 1), false);
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
}
