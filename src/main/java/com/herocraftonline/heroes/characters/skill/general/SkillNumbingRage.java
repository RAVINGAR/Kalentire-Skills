package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillNumbingRage
        extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillNumbingRage(final Heroes plugin) {
        super(plugin, "NumbingRage");
        setDescription("You ignore all fire and bleed damage for $1 second(s). This also exstinguises fire ticks.");
        setUsage("/skill numbingrage");
        setArgumentRange(0, 0);
        setIdentifiers(new String[]{"skill numbingrage"});
        setTypes(new SkillType[]{SkillType.BUFFING, SkillType.SILENCEABLE});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set("apply-text", "%hero%'s rage numbs them to pain!");
        node.set("expire-text", "%hero%'s numbing rage subsides.");
        return node;
    }

    @Override
    public void init() {
        super.init();
        SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero%'s rage numbs them to pain!").replace("%hero%", "$1").replace("$hero$", "$1");
        SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero%'s numbing rage subsides.").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        broadcastExecuteText(hero);

        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        hero.addEffect(new noPainEffect(this, hero.getPlayer(), duration, this.applyText, this.expireText));
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(final Hero hero) {
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        return getDescription().replace("$1", (duration / 1000) + "");
    }

    public static class noPainEffect
            extends ExpirableEffect {
        private final String applyText;
        private final String expireText;


        public noPainEffect(final Skill skill, final Player applier, final long duration, final String applyText, final String expireText) {
            super(skill, "noPain", applier, duration);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.RESIST_FIRE);
            this.types.add(EffectType.RESIST_POISON);
            this.applyText = applyText;
            this.expireText = expireText;


        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), this.applyText, new Object[]{player.getDisplayName()});
        }


        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), this.expireText, new Object[]{player.getDisplayName()});
        }


    }

    public class SkillListener
            implements Listener {
        public SkillListener(final Skill skill) {
        }

        @EventHandler
        public void onEntityDamage(final EntityDamageEvent event) {
            if (!(event.getEntity() instanceof Player)) {
                return;
            }
            final Player p = (Player) event.getEntity();
            final Hero hero = SkillNumbingRage.this.plugin.getCharacterManager().getHero(p);
            if (!(hero.hasEffect("noPain"))) {
                return;
            }

            if (hero.hasEffectType(EffectType.BLEED)) {
                for (final Effect e : hero.getEffects()) {
                    if (e.isType(EffectType.BLEED)) {
                        hero.removeEffect(e);
                    }
                }
            }

        }

    }
}
