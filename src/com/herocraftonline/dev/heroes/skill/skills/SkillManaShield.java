package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillDamageEvent;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.api.WeaponDamageEvent;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.ExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillManaShield extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillManaShield(Heroes plugin) {
        super(plugin, "ManaShield");
        setDescription("Uses your mana as a shield for $1 seconds.");
        setUsage("/skill manashield");
        setArgumentRange(0, 0);
        setIdentifiers("skill manashield", "skill mshield");
        setTypes(SkillType.BUFF, SkillType.SILENCABLE, SkillType.MANA);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("mana-amount", 20);
        node.set(Setting.DURATION.node(), 20000);
        node.set(Setting.APPLY_TEXT.node(), "%hero% was surrounded by a mana shield!");
        node.set(Setting.EXPIRE_TEXT.node(), "%hero% lost his mana shield!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%hero% was surrounded by a mana shield!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%hero% lost his mana shield!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        hero.addEffect(new ManaShieldEffect(this, duration));

        return SkillResult.NORMAL;
    }

    public class ManaShieldEffect extends ExpirableEffect {

        public ManaShieldEffect(Skill skill, long duration) {
            super(skill, "ManaShield", duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
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
        
        @EventHandler(priority = EventPriority.HIGH)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
                return;
            }
            
            event.setDamage(getAdjustment((Player) event.getEntity(), event.getDamage()));
        }

        @EventHandler(priority = EventPriority.HIGH)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
                return;
            }
            
            event.setDamage(getAdjustment((Player) event.getEntity(), event.getDamage()));
        }

        private int getAdjustment(Player player, int damage) {
            Hero hero = plugin.getHeroManager().getHero(player);
            if (hero.hasEffect(getName())) {
                int absorbamount = SkillConfigManager.getUseSetting(hero, skill, "mana-amount", 20, false);
                damage = damage / 2;
                int mana = hero.getMana();
                if (mana < absorbamount) {
                    hero.removeEffect(hero.getEffect("ManaShield"));
                } else {
                    mana -= absorbamount;
                    hero.setMana(mana);
                    if (mana != 100 && hero.isVerbose()) {
                        Messaging.send(player, ChatColor.BLUE + "MANA " + Messaging.createManaBar(hero.getMana(), hero.getMaxMana()));
                    }
                }
            }
            return damage;
        }
    }
    
    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
