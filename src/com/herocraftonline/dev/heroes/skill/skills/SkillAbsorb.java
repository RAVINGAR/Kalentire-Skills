package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.HeroesEventListener;
import com.herocraftonline.dev.heroes.api.SkillDamageEvent;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.api.WeaponDamageEvent;
import com.herocraftonline.dev.heroes.effects.Effect;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;

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

        registerEvent(Type.CUSTOM_EVENT, new SkillHeroListener(this), Priority.Highest);
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

    public class SkillHeroListener extends HeroesEventListener {

        private final Skill skill;
        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }
        
        @Override
        public void onSkillDamage(SkillDamageEvent event) {
            Heroes.debug.startTask("HeroesSkillListener");
            if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }
            event.setDamage(getAdjustment((Player) event.getEntity(), event.getDamage()));
            Heroes.debug.stopTask("HeroesSkillListener");
        }

        @Override
        public void onWeaponDamage(WeaponDamageEvent event) {
            Heroes.debug.startTask("HeroesSkillListener");
            if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }
            event.setDamage(getAdjustment((Player) event.getEntity(), event.getDamage()));
            Heroes.debug.stopTask("HeroesSkillListener");
        }
        
        private int getAdjustment(Player player, int damage) {
            Hero hero = plugin.getHeroManager().getHero(player);
            if (hero.hasEffect("Absorb")) {
                int absorbAmount = SkillConfigManager.getUseSetting(hero, skill, "mana-amount", 20, false);
                damage = ((int) (damage * 0.50));
                int mana = hero.getMana();
                if (mana + absorbAmount > 100) {
                    hero.removeEffect(hero.getEffect("Absorb"));
                } else {
                    hero.setMana(mana + absorbAmount);
                    if (hero.isVerbose()) {
                        Messaging.send(player, ChatColor.BLUE + "MANA " + Messaging.createManaBar(mana + absorbAmount));
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
