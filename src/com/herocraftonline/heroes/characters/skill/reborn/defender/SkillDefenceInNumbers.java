package com.herocraftonline.heroes.characters.skill.reborn.defender;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroJoinPartyEvent;
import com.herocraftonline.heroes.api.events.HeroLeavePartyEvent;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillDefenceInNumbers extends PassiveSkill {

    final static String allyEffectName = "DefenceInNumbersAllyEffect";

    public SkillDefenceInNumbers(Heroes plugin) {
        super(plugin, "DefenceInNumbers");
        setDescription("While in a party reduces the incoming damage to the party by $1% per ally$2$3.$4$5 Allies must be within $6 blocks to be effected. The effect is reapplied every $7s.");
        setTypes(SkillType.BUFFING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.ABILITY_PROPERTY_MAGICAL);
        Bukkit.getServer().getPluginManager().registerEvents(new DefenceInNumbersListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.PERIOD.node(), 5000);
        config.set(SkillSetting.RADIUS.node(), 20);
        config.set("incoming-multiplier-base", .9);
        config.set("incoming-multiplier-per-ally", .05);
        config.set("incoming-multiplier-improvement-when-buffed", .05);
        config.set("required-ally-number", 0);
        config.set("outgoing-multiplier", 1.0);
        config.set(SkillSetting.APPLY_TEXT.node(), "You have renewed your protection to allies.");
        config.set("renew-text", "You have renewed your protection to $1 allies.");
        config.set(SkillSetting.EXPIRE_TEXT.node(), "Your protection to allies has expired.");
        config.set("ally-apply-text", "Your protection in numbers has been renewed by %hero%");
        config.set("ally-expire-text", "Your protection in numbers has expired.");
        return config;
    }

    @Override
    public String getDescription(Hero hero) {

        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD.node(), 5000,false);
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 20,false);
        double incomingMultiplierBase = SkillConfigManager.getUseSetting(hero, this,
                "incoming-multiplier-base", 1,true);
        double incomingMultiplierPerAlly = SkillConfigManager.getUseSetting(hero, this,
                "incoming-multiplier-per-ally", .05,false);
        double incomingMultiplierImprovementWhenBuffed = SkillConfigManager.getUseSetting(hero, this,
                "incoming-multiplier-improvement-when-buffed", .05,false);
        int requiredAllyNumber = SkillConfigManager.getUseSetting(hero, this,
                "required-ally-number", 0,true);
        double outgoingMultiplier = SkillConfigManager.getUseSetting(hero, this,
                "outgoing-multiplier", 1.0,false);

        String description = getDescription().replace("$1", incomingMultiplierPerAlly * 100 + "");
        description = description.replace("$2",
                incomingMultiplierBase != 1 ? (" with " + incomingMultiplierBase * 100 + "% base incoming damage") : "");
        description = description.replace("$3",
                incomingMultiplierImprovementWhenBuffed != 0 ? (" and " + incomingMultiplierImprovementWhenBuffed * 100 + "% when buffed") : "");
        description = description.replace("$4",
                requiredAllyNumber > 0 ? (" Requires " + requiredAllyNumber + " allies.") : "");
        description = description.replace("$5",
                outgoingMultiplier != 1 ? (" As a result outgoing damage is " + incomingMultiplierBase * 100 + "%.") : "");
        description = description.replace("$6", radius + "");
        description = description.replace("$7", Util.decFormat.format(period / 1000));

        return description;
    }

    @Override
    public void apply(Hero hero) {
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD.node(), 5000,false);

        String applyText = SkillConfigManager.getUseSetting(hero, this,SkillSetting.APPLY_TEXT.node(),
                "You have renewed your protection to allies.");
        String expireText = SkillConfigManager.getUseSetting(hero, this,SkillSetting.EXPIRE_TEXT.node(),
                "Your protection to allies has expired.");
        hero.addEffect(new DefenceInNumbersEffect(this, this.getName(), hero.getPlayer(), period, applyText, expireText));
    }

    public class DefenceInNumbersEffect extends PeriodicEffect {

        public DefenceInNumbersEffect(Skill skill, String name, Player applier, long period, String applyText, String removeText) {
            super(skill, name, applier, period, applyText, removeText);
            setPersistent(true);
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
            //TODO boost effect when buffing a ally

            final Player defendingPlayer = hero.getPlayer();
            if (!hero.hasParty()) {
                return;
            }

            // Check required ally number
            int requiredAllyNumber = SkillConfigManager.getUseSetting(hero, skill, "required-ally-number", 0, true);
            if (hero.getParty().getMembers().size() < requiredAllyNumber){
                return;
            }

            double radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS.node(), 20,false);
            String renewText = SkillConfigManager.getUseSetting(hero, skill, "renew-text",
                    "You have renewed your protection to $1 allies.");
            String allyApplyText = SkillConfigManager.getUseSetting(hero, skill,"ally-apply-text",
                    "Your protection in numbers has been renewed by %hero%").replace("%hero%", defendingPlayer.getName());
            String allyExpireText = SkillConfigManager.getUseSetting(hero, skill, "ally-expire-text",
                    "Your protection in numbers has expired.").replace("%hero%", defendingPlayer.getName());

            // Apply protection to allies in range of the hero with this passive
            int alliesProtected = 0;
            for (Hero ally : hero.getParty().getMembers()) {
                if (hero.equals(ally))
                    continue;

                // remove existing protection
                if (ally.hasEffect(allyEffectName)){
                    ally.removeEffect(ally.getEffect(allyEffectName));
                }

                // If in range reapply effect
                if (defendingPlayer.getLocation().distance(ally.getPlayer().getLocation()) <= radius) {
                    ally.addEffect(new DefenceInNumbersAllyEffect(skill, allyEffectName, defendingPlayer,
                            getIncomingMultiplier(hero), getPeriod(), allyApplyText, allyExpireText));
                    alliesProtected++;
                }
            }

            if (!renewText.isEmpty()) {
                defendingPlayer.sendMessage(renewText.replace("$1", alliesProtected + ""));
            }
        }
    }


    public class DefenceInNumbersListener implements Listener {

        private Skill skill;

        public DefenceInNumbersListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onLeaveParty(HeroLeavePartyEvent event){
            Hero joiningHero = event.getHero();
            if (joiningHero == null)
                return;

            // remove existing protection
            if (joiningHero.hasEffect(allyEffectName)) {
                joiningHero.removeEffect(joiningHero.getEffect(allyEffectName));
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onJoinParty(HeroJoinPartyEvent event){
            Hero joiningHero = event.getHero();
            HeroParty party = event.getParty();

            if (joiningHero == null || party == null)
                return;

            //Check if a party member possesses this passive
            Hero defendingHero = null;
            for (Hero partyMember : party.getMembers()) {
                if (partyMember.hasEffect(SkillDefenceInNumbers.this.getName())){
                    defendingHero = partyMember;
                }
            }

            if (defendingHero == null)
                return;

            Player defendingPlayer = defendingHero.getPlayer();

            int requiredAllyNumber = SkillConfigManager.getUseSetting(defendingHero, skill, "required-ally-number", 0,true);
            long period = SkillConfigManager.getUseSetting(defendingHero, skill, SkillSetting.PERIOD.node(), 5000,false);
            double radius = SkillConfigManager.getUseSetting(defendingHero, skill, SkillSetting.RADIUS.node(), 20,false);

            String allyApplyText = SkillConfigManager.getUseSetting(defendingHero, skill,"ally-apply-text",
                    "Your protection in numbers has been renewed by %hero%").replace("%hero%", defendingPlayer.getName());
            String allyExpireText = SkillConfigManager.getUseSetting(defendingHero, skill, "ally-expire-text",
                    "Your protection in numbers has expired.").replace("%hero%", defendingPlayer.getName());

            // remove existing protection
            if (joiningHero.hasEffect(allyEffectName)) {
                joiningHero.removeEffect(joiningHero.getEffect(allyEffectName));
            }

            // If in range apply effect
            if (defendingPlayer.getLocation().distance(joiningHero.getPlayer().getLocation()) <= radius){

                DefenceInNumbersAllyEffect effect = new DefenceInNumbersAllyEffect(
                        skill, allyEffectName, defendingPlayer, getIncomingMultiplier(defendingHero),
                        period, allyApplyText, allyExpireText);
                joiningHero.addEffect(effect);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.getDamage() == 0 || !(event.getEntity() instanceof Hero))
                return;

            Hero hero = (Hero) event.getEntity();
            int requiredAllyNumber = SkillConfigManager.getUseSetting(hero, skill,
                    "required-ally-number", 0,true);
            if (hero.hasEffect("DefenceInNumbers") && hero.getParty().getMembers().size() >= requiredAllyNumber){
                event.setDamage(event.getDamage() * getIncomingMultiplier(hero));
            } else if (hero.hasEffect(allyEffectName)){
                DefenceInNumbersAllyEffect allyEffect = (DefenceInNumbersAllyEffect) hero.getEffect(allyEffectName);
                event.setDamage(event.getDamage() * allyEffect.getIncomingMultiplier());
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if ((event.getDamage() == 0) || !(event.getEntity() instanceof Hero))
                return;

            Hero hero = (Hero) event.getEntity();
            int requiredAllyNumber = SkillConfigManager.getUseSetting(hero, skill,
                    "required-ally-number", 0,true);
            if (hero.hasEffect("DefenceInNumbers") && hero.getParty().getMembers().size() >= requiredAllyNumber){
                event.setDamage(event.getDamage() * getIncomingMultiplier(hero));
            } else if (hero.hasEffect(allyEffectName)){
                DefenceInNumbersAllyEffect allyEffect = (DefenceInNumbersAllyEffect) hero.getEffect(allyEffectName);
                event.setDamage(event.getDamage() * allyEffect.getIncomingMultiplier());
            }
        }

    }

    public double getIncomingMultiplier(Hero defendingHero){
        double incomingMultiplierBase = SkillConfigManager.getUseSetting(defendingHero, this,
                "incoming-multiplier-base", 1,true);
        double incomingMultiplierPerAlly = SkillConfigManager.getUseSetting(defendingHero, this,
                "incoming-multiplier-per-ally", .05,false);
        double incomingMultiplierImprovementWhenBuffed = SkillConfigManager.getUseSetting(defendingHero, this,
                "incoming-multiplier-improvement-when-buffed", .05,false);

        double incomingMultiplier = incomingMultiplierBase - defendingHero.getParty().getMembers().size() * incomingMultiplierPerAlly;
        if (incomingMultiplier < 0){
            incomingMultiplier = 0;
        }
        return incomingMultiplier;
    }

    public class DefenceInNumbersAllyEffect extends ExpirableEffect {

        private double incomingMultiplier;

        public DefenceInNumbersAllyEffect(Skill skill, String name, Player applier, double incomingMultiplier,
                                          long duration, String applyText, String expireText) {
            super(skill, name, applier, duration, applyText, expireText);
            this.incomingMultiplier = incomingMultiplier;

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.AREA_OF_EFFECT);
        }

        public double getIncomingMultiplier() {
            return incomingMultiplier;
        }
    }

}