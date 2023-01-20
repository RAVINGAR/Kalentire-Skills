package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class SkillManaFreeze extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillManaFreeze(final Heroes plugin) {
        super(plugin, "ManaFreeze");
        setDescription("Prevents your target from regenerating mana for $1 second(s).");
        setUsage("/skill manafreeze");
        setArgumentRange(0, 0);
        setIdentifiers("skill manafreeze", "skill mfreeze");
        setTypes(SkillType.SILENCEABLE, SkillType.DEBUFFING, SkillType.MANA_FREEZING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 3);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.1);
        node.set(SkillSetting.DURATION.node(), 6000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has stopped regenerating mana!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is once again regenerating mana!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has stopped regenerating mana!").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is once again regenerating mana!").replace("%target%", "$1").replace("$target$", "$1");
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
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        broadcastExecuteText(hero, target);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);

        final Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        targetHero.addEffect(new ManaFreezeEffect(this, hero.getPlayer(), duration));

        final ArrayList<Location> particleLocations = circle(targetHero.getPlayer().getLocation().add(0, 0.5, 0), 36, 1.5);
        for (final Location particleLocation : particleLocations) {
            //targetHero.getPlayer().getWorld().spigot().playEffect(particleLocations.get(i), Effect.TILE_BREAK, org.bukkit.Material.ICE.getId(), 0, 0, 0.1F, 0, 0.1F, 1, 16);
            targetHero.getPlayer().getWorld().spawnParticle(Particle.BLOCK_CRACK, particleLocation, 1, 0, 0.1, 0, 0.1, Bukkit.createBlockData(Material.ICE));
        }

        //targetHero.getPlayer().getWorld().spigot().playEffect(targetHero.getPlayer().getLocation(), Effect.WITCH_MAGIC, 0, 0, 0.5F, 1.0F, 0.5F, 0.1F, 35, 16);
        targetHero.getPlayer().getWorld().spawnParticle(Particle.SPELL_WITCH, targetHero.getPlayer().getLocation(), 35, 0.5, 1, 0.5, 0.1);


        return SkillResult.NORMAL;

    }

    public class ManaFreezeEffect extends ExpirableEffect {

        public ManaFreezeEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, "ManaFreeze", applier, duration);

            types.add(EffectType.HARMFUL);
            types.add(EffectType.MAGIC);
            types.add(EffectType.DISPELLABLE);
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
