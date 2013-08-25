package com.herocraftonline.heroes.characters.skill.oldskills;
/*
package com.herocraftonline.heroes.characters.skill.skills;

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

public class SkillChlorobon extends TargettedSkill {

    public VisualEffect fplayer = new VisualEffect();
    private String expireText;
    private String applyText;

    public SkillChlorobon(Heroes plugin) {
        super(plugin, "Chlorobon");
        setDescription("You restore $1 health to the target over $2 seconds.");
        setUsage("/skill chlorobon <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill chlorobon");
        setTypes(SkillType.BUFF, SkillType.HEAL, SkillType.SILENCABLE);
    }

    public String getDescription(Hero hero) {
        int heal = SkillConfigManager.getUseSetting(hero, this, "tick-heal", 1, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 21000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, false);

        heal = heal * duration / period;

        return getDescription().replace("$1", heal + "").replace("$2", duration / 1000 + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("tick-heal", Integer.valueOf(71));
        node.set(SkillSetting.PERIOD.node(), Integer.valueOf(3000));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(12000));
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% has been given the gift of Chlorobon!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has lost the gift of Chlorobon.");
        return node;
    }

    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% has been given the gift of Chlorobon!").replace("%target%", "$1");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has lost the gift of Chlorobon.").replace("%target%", "$1");
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if ((target instanceof Player)) {
            Hero targetHero = this.plugin.getCharacterManager().getHero((Player) target);

            if (target.getHealth() >= target.getMaxHealth()) {
                Messaging.send(player, "Target is already fully healed.", new Object[0]);
                return SkillResult.INVALID_TARGET_NO_MSG;
            }

            long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, true);
            long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 12000, false);
            int tickHealth = SkillConfigManager.getUseSetting(hero, this, "tick-heal", 71, false);
            ChlorobonEffect cbEffect = new ChlorobonEffect(this, period, duration, tickHealth, player);
            targetHero.addEffect(cbEffect);

            try {
                this.fplayer.playFirework(player.getWorld(), target.getLocation().add(0.0D, 1.5D, 0.0D), FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.BALL_LARGE).withColor(Color.OLIVE).withFade(Color.NAVY).build());
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

    public class ChlorobonEffect extends PeriodicHealEffect {
        public ChlorobonEffect(Skill skill, long period, long duration, double tickHealth, Player applier) {
            super(skill, "ChlorobonEffect", period, duration, tickHealth, applier);
            this.types.add(EffectType.DISPELLABLE);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), SkillChlorobon.this.applyText, new Object[] { player.getDisplayName() });
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), SkillChlorobon.this.expireText, new Object[] { player.getDisplayName() });
        }
    }
}
*/