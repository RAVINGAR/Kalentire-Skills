/*package com.herocraftonline.heroes.characters.skill.unusedskills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillSpiritPhase
        extends ActiveSkill
{
    private static final String SPIRIT_PHASE_BUFF_EFFECT = "SpiritPhaseBuffEFfect";
    private static final String SPIRIT_PHASE_DURATION_NODE = "heal-duration";
    private static final String DAMAGE_REDUCTION_NODE = "damage-reduction";
    private static final String HEALTH_BUFF_PERCENTAGE_NODE = "health-buff-percentage";
    private static final String HEALTH_BUFF_KEY = "SpiritPhaseHealthBuff";
    private VisualEffect fplayer = new VisualEffect();

    public SkillSpiritPhase(Heroes plugin)
    {
        super(plugin, "SpiritPhase");
        setDescription(" You call upon your link with the spirit world to partially enter it giving you a temporary protection boost.");
        setUsage("/skill spiritphase");
        setArgumentRange(0, 0);
        setIdentifiers("skill spiritphase");
        setTypes(SkillType.SILENCEABLE, SkillType.BUFFING, SkillType.INTERRUPTING);
    }

    public String getDescription(Hero hero)
    {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 4000, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0D);

        return getDescription().replace("$1", formattedDuration);
    }

    public ConfigurationSection getDefaultConfig()
    {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(8));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(2000));
        node.set("heal-duration", Integer.valueOf(500));
        node.set(SkillSetting.COOLDOWN.node(), Integer.valueOf(2000));
        node.set("health-buff-percentage", Double.valueOf(1.5D));
        node.set("damage-reduction", Double.valueOf(0.25D));

        return node;
    }

    public SkillResult use(Hero hero, String[] args)
    {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 4000, false);
        hero.addEffect(new SpiritPhaseEffect(this, duration));

        return SkillResult.NORMAL;
    }

    private class SkillEntityListener
            implements Listener
    {
        private SkillEntityListener() {}

        @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
        public void onDamage(EntityDamageEvent event)
        {
            if ((event.getEntity() instanceof Player))
            {
                Player player = (Player)event.getEntity();

                Hero hero = SkillSpiritPhase.this.plugin.getCharacterManager().getHero(player);
                if (hero.hasEffect("SpiritPhaseBuffEFfect"))
                {
                    double reduction = SkillConfigManager.getUseSetting(hero, SkillSpiritPhase.this, "damage-reduction", 0.25D, false);
                    event.setDamage(event.getDamage() * reduction);
                    player.getWorld().playSound(event.getEntity().getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 0.0F);
                }
            }
        }
    }

    public class SpiritPhaseEffect extends PeriodicExpirableEffect {
        private SpiritPhaseEffect(Skill skill, long duration) {
            super(skill, SpiritPhase"")
            super(skill, "SpiritPhaseBuffEffect", 5L, duration);
            this.types.add(EffectType.MAGIC);
            this.types.add(EffectType.BENEFICIAL);
        }

        public void applyToHero(Hero hero)
        {
            super.applyToHero(hero);

            Player player = hero.getPlayer();

            double healthBuff = SkillConfigManager.getUseSetting(hero, SkillSpiritPhase.this, "health-buff-percentage", 1.5D, false);
            double oldMaxHealth = hero.getPlayer().getMaxHealth();
            double newMaxHealth = oldMaxHealth * healthBuff;
            double difference = newMaxHealth - oldMaxHealth;
            hero.addMaxHealth("SpiritPhaseHealthBuff", difference);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 0.0F);
        }

        public void removeFromHero(Hero hero)
        {
            super.removeFromHero(hero);
            hero.removeMaxHealth("SpiritPhaseHealthBuff");
        }

        public void tickMonster(Monster monster) {}

        public void tickHero(Hero hero)
        {
            Player player = hero.getPlayer();

            player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 10);
    }
  }
}*/