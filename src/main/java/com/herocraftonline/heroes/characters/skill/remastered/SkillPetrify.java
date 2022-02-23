package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class SkillPetrify extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillPetrify(Heroes plugin) {
        super(plugin, "Petrify");
        setDescription("Halt your target's mana and stamina regeneration for $1 second(s).");
        setUsage("/skill petrify");
        setArgumentRange(0, 0);
        setIdentifiers("skill petrify");
        setTypes(SkillType.SILENCEABLE, SkillType.DEBUFFING, SkillType.MANA_FREEZING, SkillType.STAMINA_FREEZING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 3);
        config.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.1);
        config.set(SkillSetting.DURATION.node(), 6000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has stopped regenerating mana and stamina!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is once again regenerating mana and stamina!");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(),
                ChatComponents.GENERIC_SKILL + "%target% has stopped regenerating mana!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(),
                ChatComponents.GENERIC_SKILL + "%target% is once again regenerating mana!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player))
        	return SkillResult.INVALID_TARGET;

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);

        broadcastExecuteText(hero, target);

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        targetHero.addEffect(new PetrifiedEffect(this, hero.getPlayer(), duration));

        final Player targetPlayer = targetHero.getPlayer();
        final World world = targetPlayer.getWorld();

        List<Location> particleLocations = GeometryUtil.circle(targetPlayer.getLocation().add(0, 0.5, 0), 36, 1.5);
		for (int i = 0; i < particleLocations.size(); i++) {
			//world.spigot().playEffect(particleLocations.get(i), Effect.TILE_BREAK, org.bukkit.Material.ICE.getId(), 0, 0, 0.1F, 0, 0.1F, 1, 16);
            world.spawnParticle(Particle.BLOCK_CRACK, particleLocations.get(i), 1, 0, 0.1, 0, 0.1, Bukkit.createBlockData(Material.ICE));
		}
		
		//world.spigot().playEffect(targetPlayer.getLocation(), Effect.WITCH_MAGIC, 0, 0, 0.5F, 1.0F, 0.5F, 0.1F, 35, 16);
        world.spawnParticle(Particle.SPELL_WITCH, targetPlayer.getLocation(), 35, 0.5, 1, 0.5, 0.1);

        return SkillResult.NORMAL;
    }

    public class PetrifiedEffect extends ExpirableEffect {

        public PetrifiedEffect(Skill skill, Player applier, long duration) {
            super(skill, "Petrified", applier, duration);

            types.add(EffectType.HARMFUL);
            types.add(EffectType.MAGIC);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MANA_FREEZING);
            types.add(EffectType.STAMINA_FREEZING);
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
}
