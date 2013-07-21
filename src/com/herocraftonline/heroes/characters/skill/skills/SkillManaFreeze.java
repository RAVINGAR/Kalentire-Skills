package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillManaFreeze extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillManaFreeze(Heroes plugin) {
        super(plugin, "ManaFreeze");
        setDescription("Prevents your target from regenerating mana for $1 seconds.");
        setUsage("/skill manafreeze");
        setArgumentRange(0, 0);
        setIdentifiers("skill manafreeze", "skill mfreeze");
        setTypes(SkillType.SILENCABLE, SkillType.DEBUFF, SkillType.MANA, SkillType.HARMFUL);
        Bukkit.getServer().getPluginManager().registerEvents(new HeroListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% has stopped regenerating mana!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% is once again regenerating mana!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%target% has stopped regenerating mana!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "%target% is once again regenerating mana!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player))
        	return SkillResult.INVALID_TARGET;

        broadcastExecuteText(hero, target);
        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        targetHero.addEffect(new ManaFreezeEffect(this, duration));
        return SkillResult.NORMAL;

    }

    public class HeroListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onHeroRegainMana(HeroRegainManaEvent event) {
            if (event.isCancelled()) {
                return;
            }

            if (event.getHero().hasEffect("ManaFreeze")) {
                event.setCancelled(true);
            }
        }
    }

    public class ManaFreezeEffect extends ExpirableEffect {

        public ManaFreezeEffect(Skill skill, long duration) {
            super(skill, "ManaFreeze", duration);

            types.add(EffectType.HARMFUL);
            types.add(EffectType.MAGIC);
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
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
