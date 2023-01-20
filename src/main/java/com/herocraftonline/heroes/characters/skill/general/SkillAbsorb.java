package com.herocraftonline.heroes.characters.skill.general;

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
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillAbsorb extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillAbsorb(final Heroes plugin) {
        super(plugin, "Absorb");
        setDescription("You absorb half the damage you take as mana.");
        setUsage("/skill absorb");
        setArgumentRange(0, 0);
        setIdentifiers("skill absorb");
        setTypes(SkillType.SILENCEABLE, SkillType.BUFFING, SkillType.MANA_INCREASING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("mana-amount", 20);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% is absorbing damage!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "Absorb faded from %target%!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% is absorbing damage!").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "Absorb faded from %target%!").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        broadcastExecuteText(hero);
        hero.addEffect(new AbsorbEffect(this, hero.getPlayer()));
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(final Hero hero) {
        return getDescription();
    }

    public class AbsorbEffect extends Effect {

        public AbsorbEffect(final Skill skill, final Player applier) {
            super(skill, "Absorb", applier);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

    }

    public class SkillHeroListener implements Listener {

        private final Skill skill;

        public SkillHeroListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onSkillDamage(final SkillDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
                return;
            }
            event.setDamage(getAdjustment((Player) event.getEntity(), event.getDamage()));
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onWeaponDamage(final WeaponDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
                return;
            }
            event.setDamage(getAdjustment((Player) event.getEntity(), event.getDamage()));
        }

        private double getAdjustment(final Player player, double d) {
            final Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect("Absorb")) {
                final int absorbAmount = SkillConfigManager.getUseSetting(hero, skill, "mana-amount", 20, false);
                d = ((int) (d * 0.50));
                final int mana = hero.getMana();
                if (mana + absorbAmount > hero.getMaxMana()) {
                    hero.removeEffect(hero.getEffect("Absorb"));
                } else {
                    hero.setMana(mana + absorbAmount);
                    if (hero.isVerboseMana()) {
                        player.sendMessage(ChatColor.BLUE + "MANA " + ChatComponents.Bars.mana(mana + absorbAmount, hero.getMaxMana(), false));
                    }
                }
            }
            return d;
        }
    }
}
