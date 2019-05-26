package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.effects.common.SpeedEffect;
import com.herocraftonline.heroes.characters.effects.common.SummonEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillConvocation extends ActiveSkill {

    public SkillConvocation(Heroes plugin) {
        super(plugin, "Convocation");
        setDescription("You summon all of your minions and clear their current targets. " +
                "Upon being summoned, you will buff each of them with Speed $1 for $2 seconds.");
        setUsage("/skill convocation");
        setIdentifiers("skill convocation");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        long duration = SkillConfigManager.getUseSetting(hero, this, "speed-duration", 3000, false);
        int speedAmplifier = SkillConfigManager.getUseSetting(hero, this, "speed-amplifier", 3, false);

        return getDescription()
                .replace("$1", (speedAmplifier + 1) + "")
                .replace("$2", Util.decFormat.format(duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("speed-duration", 3000);
        config.set("speed-amplifier", 3);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location playerLoc = player.getLocation();

        if (hero.getSummons().isEmpty())
            return SkillResult.INVALID_TARGET;

        broadcastExecuteText(hero);

        long duration = SkillConfigManager.getUseSetting(hero, this, "speed-duration", 3000, false);
        int speedAmplifier = SkillConfigManager.getUseSetting(hero, this, "speed-amplifier", 3, false);

        for (Monster summon : hero.getSummons()) {
            summon.setTargetIfAble(null, false);
            summon.getEntity().teleport(playerLoc);
            summon.addEffect(new SpeedEffect(this, "ConvocationSpeed", player, duration, speedAmplifier));
//            summon.addEffect(new PeriodicHealEffect(this, "ConvocationHealing", player, duration, speedAmplifier));   // TODO: Add this?
        }

//        player.getWorld().playEffect(player.getLocation(), Effect.ENDEREYE_LAUNCH, 3);
//        target.getWorld().spawnParticle(Particle.SPELL_WITCH, target.getLocation(), 55, 0, 1, 0, 10);
        return SkillResult.NORMAL;
    }
}