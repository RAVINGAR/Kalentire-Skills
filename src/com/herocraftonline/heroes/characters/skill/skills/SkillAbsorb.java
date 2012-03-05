package com.herocraftonline.heroes.characters.skill.skills;

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
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillAbsorb extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillAbsorb(Heroes plugin) {
        super(plugin, "Absorb");
        setDescription("You absorb half the damage you take as mana.");
        setUsage("/skill absorb");
        setArgumentRange(0, 0);
        setIdentifiers("skill absorb");
        setTypes(SkillType.SILENCABLE, SkillType.BUFF, SkillType.MANA);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
    	ConfigurationSection node = super.getDefaultConfig();
        node.set("mana-amount", 20);
        node.set(Setting.APPLY_TEXT.node(), "%target% is absorbing damage!");
        node.set(Setting.EXPIRE_TEXT.node(), "Absorb faded from %target%!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%target% is absorbing damage!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "Absorb faded from %target%!").replace("%target%", "$1");
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
        }

        @Override
        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
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
        
        private int getAdjustment(Player player, int damage) {
            Hero hero = plugin.getHeroManager().getHero(player);
            if (hero.hasEffect("Absorb")) {
                int absorbAmount = SkillConfigManager.getUseSetting(hero, skill, "mana-amount", 20, false);
                damage = ((int) (damage * 0.50));
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
            return damage;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
