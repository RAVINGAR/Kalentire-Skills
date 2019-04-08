package com.herocraftonline.heroes.characters.skill.reborn.chainwarden;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Properties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillChainBelt extends PassiveSkill {

    public static String effectName = "ChainBelt";

    public SkillChainBelt(Heroes plugin) {
        super(plugin, "ChainBelt");
        setDescription("You can hold your hooks and chains on your belt! Can hold up to $1 chains at once. You ready a new chain once every $2 secoonds.");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.PERIOD.node(), 8000);
        config.set("max-chain-count", 5);
        return config;
    }

    @Override
    public String getDescription(Hero hero) {
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 8000, false) / 1000;
        int maxChains = SkillConfigManager.getUseSetting(hero, this, "max-chain-count", 5, false);

        return getDescription()
                .replace("$1", maxChains + "")
                .replace("$2", period + "");
    }

    @Override
    public void apply(Hero hero) {
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 8000, false);
        ChainBeltEffect effect = new ChainBeltEffect(this, hero.getPlayer(), period);
        hero.addEffect(effect);
    }

    public static boolean tryRemoveChain(Skill skill, Hero hero, boolean shouldBroadCast) {
        if (hero.hasEffect(SkillChainBelt.effectName)) {
            ChainBeltEffect chainBelt = (ChainBeltEffect) hero.getEffect(SkillChainBelt.effectName);
            if (!chainBelt.tryRemoveChain()) {
                if (shouldBroadCast) {
                    hero.getPlayer().sendMessage("    " + ChatComponents.GENERIC_SKILL + ChatColor.WHITE + "Not enough chains available to use " + skill.getName() + "!");
                }
                return false;
            }
        }
        return true;
    }

    public class ChainBeltEffect extends PeriodicEffect {
        private int maxChains;
        private int chainCount;

        ChainBeltEffect(Skill skill, Player player, long period) {
            super(skill, effectName, player, period);

            types.add(EffectType.INTERNAL);
            types.add(EffectType.PHYSICAL);
            setPersistent(true);
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
            tryAddChain();
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            this.maxChains = SkillConfigManager.getUseSetting(hero, skill, "max-chain-count", 5, false);
            this.chainCount = 1;
        }

        public int getCurrentChainCount() {
            return this.chainCount;
        }

        public boolean tryRemoveChain() {
            if (this.chainCount > 0) {
                this.chainCount--;
                return true;
            }
            return false;
        }

        public boolean tryAddChain() {
            if (this.chainCount < maxChains) {
                this.chainCount++;
                this.applier.sendMessage("    " + ChatComponents.GENERIC_SKILL + ChatColor.WHITE +
                        " <" + ChatColor.DARK_GRAY + "Chain Belt" + ChatColor.WHITE + ">" +
                        " Current Chain Count: " + ChatColor.GRAY + this.chainCount);
                return true;
            }
            return false;
        }
    }
}