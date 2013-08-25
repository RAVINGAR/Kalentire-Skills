package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillRejuvenate extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    private String expireText;
    private String applyText;

    public SkillRejuvenate(Heroes plugin) {
        super(plugin, "Rejuvenate");
        setDescription("You restore $1 health to the target over $2 seconds.");
        setUsage("/skill rejuvenate <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill rejuvenate", "skill rejuv");
        setTypes(SkillType.BUFF, SkillType.HEAL, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("tick-heal", 1);
        node.set(SkillSetting.PERIOD.node(), 3000);
        node.set(SkillSetting.DURATION.node(), 21000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% is rejuvenating health!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has stopped rejuvenating health!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% is rejuvenating health!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has stopped rejuvenating health!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (target instanceof Player) {
            Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

            if (target.getHealth() >= target.getMaxHealth()) {
                Messaging.send(player, "Target is already fully healed.");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }

            long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, true);
            long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 21000, false);
            int tickHealth = SkillConfigManager.getUseSetting(hero, this, "tick-heal", 1, false);
            RejuvenateEffect rEffect = new RejuvenateEffect(this, period, duration, tickHealth, player);
            targetHero.addEffect(rEffect);
            // this is our fireworks shit
            try {
                fplayer.playFirework(player.getWorld(), target.getLocation(), FireworkEffect.builder().flicker(true).trail(false).with(FireworkEffect.Type.STAR).withColor(Color.FUCHSIA).withFade(Color.WHITE).build());
            }
            catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return SkillResult.NORMAL;
        }

        return SkillResult.INVALID_TARGET;
    }

    public class RejuvenateEffect extends PeriodicHealEffect {

        public RejuvenateEffect(Skill skill, long period, long duration, double tickHealth, Player applier) {
            super(skill, "Rejuvenate", period, duration, tickHealth, applier);

            types.add(EffectType.MAGIC);
            types.add(EffectType.HEAL);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DISPELLABLE);
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

    @Override
    public String getDescription(Hero hero) {
        int heal = SkillConfigManager.getUseSetting(hero, this, "tick-heal", 1, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 21000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, false);
        heal = heal * duration / period;
        return getDescription().replace("$1", heal + "").replace("$2", duration / 1000 + "");
    }

}
