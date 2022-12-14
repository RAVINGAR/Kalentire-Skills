package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class SkillActiveLifeSteal extends ActiveSkill implements Listenable {
    private final Listener listener;

    public SkillActiveLifeSteal(Heroes plugin) {
        super(plugin, "ActiveLifeSteal");
        setDescription("Your attacks restore $1 health for a duration of $2 seconds.");
        setUsage("/skill activelifesteal");
        setArgumentRange(0, 1);
        setIdentifiers("skill activelifesteal");
        setTypes(SkillType.BUFFING);
        this.listener = new SkillHeroListener(this);
    }

    @Override
    public String getDescription(Hero hero) {
        double health = SkillConfigManager.getUseSetting(hero, this, "health-per-attack", 1, false);
        double duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 8000, false);
        return getDescription().replace("$1", "" + health).replace("$2", "" + duration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 8000);
        node.set("health-per-attack", 1);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 8000, false);
        hero.addEffect(new LifeStealEffect( this, hero.getPlayer(), duration));
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @NotNull
    @Override
    public Listener getListener() {
        return listener;
    }

    public class LifeStealEffect extends ExpirableEffect {
        public LifeStealEffect(SkillActiveLifeSteal skill, Player applier, long duration) {
            super(skill, "Lifesteal", applier, duration);
        }
    }

    public class SkillHeroListener implements Listener {
        private final Skill skill;

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(WeaponDamageEvent event) {
            if(event.isCancelled()) {
                return;
            }
            if (event.getAttacker() instanceof Hero hero) {
                if (!hero.hasEffect("Lifesteal"))
                    return;

                double health = SkillConfigManager.getUseSetting(hero, skill, "health-per-attack", 1, false);

                HeroRegainHealthEvent hrEvent = new HeroRegainHealthEvent(hero, health, skill, hero);
                plugin.getServer().getPluginManager().callEvent(hrEvent);
                if(!hrEvent.isCancelled()) {
                    hero.heal(hrEvent.getDelta());
                }
            }
        }
    }
}
