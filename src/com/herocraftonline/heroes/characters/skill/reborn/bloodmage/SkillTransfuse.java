package com.herocraftonline.heroes.characters.skill.reborn.bloodmage;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SkillTransfuse extends ActiveSkill {
    private static Color MANA_BLUE = Color.fromRGB(0, 191, 255);

    public SkillTransfuse(Heroes plugin) {
        super(plugin, "Transfuse");
        setDescription("You sacrifice some blood in exchange for mana. " +
                "Converts $1 health to $2 mana.");
        setUsage("/skill transfuse");
        setIdentifiers("skill transfuse");
        setArgumentRange(0, 0);
        setTypes(SkillType.MANA_INCREASING, SkillType.ABILITY_PROPERTY_DARK);
    }

    @Override
    public String getDescription(Hero hero) {
        double healthCost = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALTH_COST, 100.0, false);
        int manaGain = SkillConfigManager.getScaledUseSettingInt(hero, this, "mana-gain", 150, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(healthCost))
                .replace("$2", manaGain + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.HEALTH_COST.node(), 100.0);
        config.set("mana-gain", 150);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        if (hero.getMana() >= hero.getMaxMana()) {
            hero.getPlayer().sendMessage("    " + ChatComponents.GENERIC_SKILL + "You are already at full mana.");
            return SkillResult.FAIL;
        }

        int manaGain = SkillConfigManager.getUseSetting(hero, this, "mana-gain", 150, false);
        if (!hero.tryRestoreMana(hero, this, manaGain)) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Player player = hero.getPlayer();
        broadcastExecuteText(hero);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.0F);
        applyVisualsEffects(player);

        return SkillResult.NORMAL;
    }

    private void applyVisualsEffects(Player player) {
        final World world = player.getWorld();
        final Location loc = player.getLocation();

        final EffectManager effectManager = new EffectManager(plugin);
        final CylinderEffect redSwirl = buildSwirlyEffect(player, effectManager, Color.RED);
        final CylinderEffect blueSwirl = buildSwirlyEffect(player, effectManager, MANA_BLUE);

        effectManager.start(redSwirl);
        effectManager.start(blueSwirl);
        effectManager.disposeOnTermination();
    }

    @NotNull
    private CylinderEffect buildSwirlyEffect(Player player, EffectManager effectManager, Color color) {
        final CylinderEffect swirl = new CylinderEffect(effectManager);

        final int durationTicks = 20;   // 1 second
        final int displayPeriod = 1;
        DynamicLocation dynamicLoc = new DynamicLocation(player);
        swirl.setDynamicOrigin(dynamicLoc);
        swirl.disappearWithOriginEntity = true;
        swirl.particle = Particle.REDSTONE;
        swirl.color = color;
        swirl.particles = 25;
        swirl.radius = 1.75F;
        swirl.height = 2.0F;
        swirl.solid = false;
        swirl.enableRotation = true;
        swirl.angularVelocityX = 0;
        swirl.angularVelocityY = 2.5;
        swirl.angularVelocityZ = 0;
        swirl.period = displayPeriod;
        swirl.iterations = durationTicks / displayPeriod;
        return swirl;
    }
}
