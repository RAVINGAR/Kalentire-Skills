package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillChainBelt extends PassiveSkill {

    public static String skillName = "ChainBelt";
    public static String effectName = "ChainBelt";

    public SkillChainBelt(Heroes plugin) {
        super(plugin, "ChainBelt");
        setDescription("You can hold your hooks and chains on your belt! Can hold up to $1 chains at once. " +
                "You ready a new chain once every $2 second(s). " +
                "If you somehow acquire more chains than you're able to hold, you will lose $3 chain(s) ever $1 second(s) instead.");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.PERIOD.node(), 8000);
        config.set("max-chain-count", 6);
        config.set("chains-lost-on-overcap", 2);
        return config;
    }

    @Override
    public String getDescription(Hero hero) {
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 8000, false);
        int maxChains = SkillConfigManager.getUseSetting(hero, this, "max-chain-count", 6, false);
        int chainLoss = SkillConfigManager.getUseSetting(hero, this, "chains-lost-on-overcap", 1, false);

        return getDescription()
                .replace("$1", maxChains + "")
                .replace("$2", Util.decFormat.format(period / 1000.0))
                .replace("$3", chainLoss + "");
    }

    @Override
    public void apply(Hero hero) {
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 8000, false);
        ChainBeltEffect effect = new ChainBeltEffect(this, hero.getPlayer(), period);
        hero.addEffect(effect);
    }

    public static int tryGetCurrentChainCount(Hero hero) {
        if (!hero.hasEffect(SkillChainBelt.effectName))
            return 0;

        ChainBeltEffect effect = (ChainBeltEffect) hero.getEffect(SkillChainBelt.effectName);
        if (effect == null)
            return 0;

        return effect.getCurrentChainCount();
    }

    public static boolean tryRemoveChain(Skill skill, Hero hero, boolean shouldBroadcast) {
        if (hero.hasEffect(SkillChainBelt.effectName)) {
            ChainBeltEffect chainBelt = (ChainBeltEffect) hero.getEffect(SkillChainBelt.effectName);
            if (!chainBelt.tryRemoveChain(shouldBroadcast)) {
                if (shouldBroadcast) {
                    hero.getPlayer().sendMessage("    " + ChatComponents.GENERIC_SKILL +  "Not enough chains available to use " + skill.getName() + "!");
                }
                return false;
            }
        }
        return true;
    }

    public static void showCurrentChainCount(Hero hero) {
        if (!hero.hasEffect(SkillChainBelt.effectName))
            return;

        ChainBeltEffect effect = (ChainBeltEffect) hero.getEffect(SkillChainBelt.effectName);
        if (effect == null)
            return;

        effect.displayChainCountMessage();
    }

    public class ChainBeltEffect extends PeriodicEffect {
        private int maxChains;
        private int chainCount;
        private int lossOnOvercap;

        ChainBeltEffect(Skill skill, Player player, long period) {
            super(skill, effectName, player, period);

            types.add(EffectType.INTERNAL);
            setPersistent(true);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            this.maxChains = SkillConfigManager.getUseSetting(hero, skill, "max-chain-count", 5, false);
            this.lossOnOvercap = SkillConfigManager.getUseSetting(hero, skill, "chains-lost-on-overcap", 1, false);
            this.chainCount = this.maxChains;

            displayChainCountMessage();
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
            tryAddChain();
            if (chainCount > maxChains) {
                for (int i = 0; i < lossOnOvercap; i++) {
                    tryRemoveChain();
                    if (chainCount <= maxChains)
                        break;
                }
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }

        public int getCurrentChainCount() {
            return this.chainCount;
        }

        public boolean tryRemoveChain() {
            return tryRemoveChain(true);
        }

        public boolean tryRemoveChain(boolean showMessage) {
            if (this.chainCount > 0) {
                this.chainCount--;
                if (showMessage)
                    displayChainCountMessage();
                return true;
            }
            return false;
        }

        public boolean tryAddChain() {
            if (this.chainCount < maxChains) {
                this.chainCount++;
                displayChainCountMessage();
                return true;
            }
            return false;
        }

        public void forceAddChains(int numChainsToAdd) {
            this.chainCount+= numChainsToAdd;

            displayChainCountMessage();
        }

        public void displayChainCountMessage() {
            ChatColor currentCountColor = this.chainCount > this.maxChains ? ChatColor.RED : ChatColor.GRAY;
            this.applier.sendMessage("    " + ChatComponents.GENERIC_SKILL + ChatColor.WHITE +
                    "<" + ChatColor.DARK_GRAY + "Chain Belt" + ChatColor.WHITE + "> " +
                    "Chain Count: " + currentCountColor + this.chainCount + ChatColor.WHITE + "/" + ChatColor.GRAY + this.maxChains);
        }
    }
}