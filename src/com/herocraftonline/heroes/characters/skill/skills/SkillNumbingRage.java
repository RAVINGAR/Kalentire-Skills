package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillNumbingRage
        extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillNumbingRage(Heroes plugin) {
        super(plugin, "NumbingRage");
        setDescription("You ignore all fire and bleed damage for $1 seconds. This also exstinguises fire ticks.");
        setUsage("/skill numbingrage");
        setArgumentRange(0, 0);
        setIdentifiers(new String[]{"skill numbingrage"});
        setTypes(new SkillType[]{SkillType.BUFFING, SkillType.SILENCEABLE});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(10000));
        node.set("apply-text", "%hero%'s rage numbs them to pain!");
        node.set("expire-text", "%hero%'s numbing rage subsides.");
        return node;
    }

    public void init() {
        super.init();
        SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero%'s rage numbs them to pain!").replace("%hero%", "$1");
        SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero%'s numbing rage subsides.").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        hero.addEffect(new noPainEffect(this, hero.getPlayer(), duration, this.applyText, this.expireText));
        return SkillResult.NORMAL;
    }

    public class noPainEffect
            extends ExpirableEffect {
        private String applyText;
        private String expireText;


        public noPainEffect(Skill skill, Player applier, long duration, String applyText, String expireText) {
            super(skill, "noPain", applier, duration);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.RESIST_FIRE);
            this.types.add(EffectType.RESIST_POISON);
            this.applyText = applyText;
            this.expireText = expireText;


        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), this.applyText, new Object[]{player.getDisplayName()});
        }


        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), this.expireText, new Object[]{player.getDisplayName()});
        }


    }

    public class SkillListener
            implements Listener {
        public SkillListener(Skill skill) {
        }

        @EventHandler
        public void onEntityDamage(EntityDamageEvent event) {
            if (!(event.getEntity() instanceof Player))
                return;
            Player p = (Player) event.getEntity();
            Hero hero = SkillNumbingRage.this.plugin.getCharacterManager().getHero(p);
            if (!(hero.hasEffect("noPain")))
                return;

            if (hero.hasEffectType(EffectType.BLEED)) {
                for (Effect e : hero.getEffects()) {
                    if (e.isType(EffectType.BLEED))
                        hero.removeEffect(e);
                }
            }

        }

    }

    @Override
    public String getDescription(Hero hero) {
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        return getDescription().replace("$1", (duration / 1000) + "");
    }
}
