package com.herocraftonline.heroes.characters.skill.unusedskills;
/*
package com.herocraftonline.heroes.characters.skill.oldskills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillAbsorb extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillAbsorb(Heroes plugin) {
        super(plugin, "Absorb");
        setDescription("You absorb half the damage you take as mana.");
        setUsage("/skill absorb");
        setArgumentRange(0, 0);
        setIdentifiers("skill absorb");
        setTypes(SkillType.SILENCABLE, SkillType.BUFFING, SkillType.MANA_INCREASING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
    	ConfigurationSection node = super.getDefaultConfig();
        node.set("mana-amount", 20);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% is absorbing damage!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "Absorb faded from %target%!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% is absorbing damage!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "Absorb faded from %target%!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        hero.addEffect(new AbsorbEffect(this));
        return SkillResult.NORMAL;
    }

    public class AbsorbEffect extends Effect {

        public AbsorbEffect(Skill skill) {
            super(skill, "Absorb");
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.MAGIC);
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

    public class SkillHeroListener implements Listener {

        private final Skill skill;
        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }
        
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
                return;
            }
            event.setDamage(getAdjustment((Player) event.getEntity(), event.getDamage()));
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
                return;
            }
            event.setDamage(getAdjustment((Player) event.getEntity(), event.getDamage()));
        }
        
        private double getAdjustment(Player player, double d) {
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect("Absorb")) {
                int absorbAmount = SkillConfigManager.getUseSetting(hero, skill, "mana-amount", 20, false);
                d = ((int) (d * 0.50));
                int mana = hero.getMana();
                if (mana + absorbAmount > hero.getMaxMana()) {
                    hero.removeEffect(hero.getEffect("Absorb"));
                } else {
                    hero.setMana(mana + absorbAmount);
                    if (hero.isVerbose()) {
                        Messaging.send(player, ChatColor.BLUE + "MANA " + Messaging.createManaBar(mana + absorbAmount, hero.getMaxMana()));
                    }
                }
            }
            return d;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
*/