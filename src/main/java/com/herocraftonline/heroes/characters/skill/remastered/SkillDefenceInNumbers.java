package com.herocraftonline.heroes.characters.skill.remastered;

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
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillDefenceInNumbers extends PassiveSkill implements Listenable {
    final static String allyEffectName = "DefenceInNumbersAllyEffect";
    public static final String SKILL_MESSAGE_PREFIX_SPACES = "    ";
    private String applyText;
    private String expireText;
    private String renewText;
    private String allyApplyText;
    private String allyExpireText;
    private final Listener listener;

    public SkillDefenceInNumbers(Heroes plugin) {
        super(plugin, "DefenceInNumbers");
        setDescription("While in a party reduces the incoming damage to the party by $1% per ally$2$3." +
                "$4$5 Allies must be within $6 blocks to be effected. " +
                "The effect is reapplied every $7s.");
        setTypes(SkillType.BUFFING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.ABILITY_PROPERTY_MAGICAL);

        listener = new DefenceInNumbersListener(this);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.PERIOD.node(), 5000);
        config.set(SkillSetting.RADIUS.node(), 20);
        config.set("protect-allies", true);
        config.set("incoming-multiplier-base", .9);
        config.set("incoming-multiplier-per-ally", .05);
        config.set("incoming-multiplier-improvement-when-buffed", .05);
        config.set("required-ally-number", 0);
        config.set("outgoing-multiplier", 1.0);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "You have renewed your protection to allies.");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), null); //remove option -> using expire text instead
        config.set("renew-text", ChatComponents.GENERIC_SKILL + "You have renewed your protection to $1 allies.");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "Your protection to allies has expired.");
        config.set("ally-apply-text", ChatComponents.GENERIC_SKILL + "Your protection in numbers has been renewed by %hero%");
        config.set("ally-expire-text", ChatComponents.GENERIC_SKILL + "Your protection in numbers has expired.");
        return config;
    }

    @Override
    public String getDescription(Hero hero) {

        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD.node(), 5000, false);
        double radius = SkillConfigManager.getUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        double incomingMultiplierBase = SkillConfigManager.getUseSetting(hero, this,
                "incoming-multiplier-base", 1.0, true);
        double incomingMultiplierPerAlly = SkillConfigManager.getUseSetting(hero, this,
                "incoming-multiplier-per-ally", .05, false);
        double incomingMultiplierImprovementWhenBuffed = SkillConfigManager.getUseSetting(hero, this,
                "incoming-multiplier-improvement-when-buffed", .05, false);
        int requiredAllyNumber = SkillConfigManager.getUseSetting(hero, this,
                "required-ally-number", 0, true);
        double outgoingMultiplier = SkillConfigManager.getUseSetting(hero, this,
                "outgoing-multiplier", 1.0, false);

        String description = getDescription().replace("$1", Util.decFormat.format(incomingMultiplierPerAlly * 100) + "");
        description = description.replace("$2",
                incomingMultiplierBase != 1 ? (" with " + Util.decFormat.format(incomingMultiplierBase * 100) + "% base incoming damage") : "");
        description = description.replace("$3",
                incomingMultiplierImprovementWhenBuffed != 0 ? (" and "
                        + Util.decFormat.format(incomingMultiplierImprovementWhenBuffed * 100) + "% when buffed") : "");
        description = description.replace("$4",
                requiredAllyNumber > 0 ? (" Requires " + requiredAllyNumber + " allies.") : "");
        description = description.replace("$5",
                outgoingMultiplier != 1 ? (" As a result outgoing damage is " + Util.decFormat.format(incomingMultiplierBase * 100) + "%.") : "");
        description = description.replace("$6", Util.decFormat.format(radius) + "");
        description = description.replace("$7", Util.decFormat.format(period / 1000));

        return description;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(),
                ChatComponents.GENERIC_SKILL + "You have renewed your protection to allies.");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(),
                ChatComponents.GENERIC_SKILL + "Your protection to allies has expired.");
        renewText = SkillConfigManager.getRaw(this, "renew-text",
                ChatComponents.GENERIC_SKILL + "You have renewed your protection to $1 allies.");
        allyApplyText = SkillConfigManager.getRaw(this, "ally-apply-text",
                ChatComponents.GENERIC_SKILL + "Your protection in numbers has been renewed by %hero%");
        allyExpireText = SkillConfigManager.getRaw(this, "ally-expire-text",
                ChatComponents.GENERIC_SKILL + "Your protection in numbers has expired.");
    }

    @Override
    public void apply(Hero hero) {
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD.node(), 5000, false);
        boolean protectAllies = SkillConfigManager.getUseSettingBool(hero, this, "protect-allies");

        hero.addEffect(new DefenceInNumbersEffect(this, this.getName(), hero.getPlayer(), protectAllies, period, applyText, expireText));
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    public class DefenceInNumbersEffect extends PeriodicEffect {

        boolean protectAllies;

        public DefenceInNumbersEffect(Skill skill, String name, Player applier, boolean protectAllies, long period, String applyText, String removeText) {
            super(skill, name, applier, period, applyText, removeText);
            setEffectTypes(EffectType.INTERNAL, EffectType.SILENT_ACTIONS, EffectType.BENEFICIAL, EffectType.AREA_OF_EFFECT);
            setPersistent(true);
            this.protectAllies = protectAllies;
        }

        @Override
        public void applyToHero(Hero hero) {
            // Make more quiet when out of party (note as this has no potion effects this is fine)
            if (hasAllies(hero))
                super.applyToHero(hero);
        }

        @Override
        public void removeFromHero(Hero hero) {
            // Make more quiet when out of party (note as this has no potion effects this is fine)
            if (hasAllies(hero))
                super.removeFromHero(hero);
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
            //TODO boost effect when buffing a ally

            // Need to protect allies and atleast one ally
            if (!protectAllies || !hasAllies(hero))
                return;

            // Check required ally number
            int requiredAllyNumber = SkillConfigManager.getUseSetting(hero, skill, "required-ally-number", 0, true);
            if (!(hero.getParty().getMembers().size() - 1 >= requiredAllyNumber)) {
                return;
            }

            double radius = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.RADIUS, false);
            double radiusSquared = radius * radius;

            // Apply protection to allies in range of the hero with this passive
            final Player defendingPlayer = hero.getPlayer();
            int alliesProtected = 0;
            for (Hero ally : hero.getParty().getMembers()) {
                if (hero.equals(ally))
                    continue;

                // remove existing protection
                if (ally.hasEffect(allyEffectName)) {
                    ally.removeEffect(ally.getEffect(allyEffectName));
                }

                // If in range reapply effect
                Player allyPlayer = ally.getPlayer();
                if (defendingPlayer.getWorld().equals(allyPlayer.getWorld())
                        && defendingPlayer.getLocation().distanceSquared(allyPlayer.getLocation()) <= radiusSquared) {
                    ally.addEffect(new DefenceInNumbersAllyEffect(skill, allyEffectName, defendingPlayer,
                            getIncomingMultiplier(hero), getPeriod(),
                            allyApplyText.replace("%hero%", defendingPlayer.getName()),
                            allyExpireText.replace("%hero%", defendingPlayer.getName())));
                    alliesProtected++;
                }
            }

            if (!renewText.isEmpty()) {
                defendingPlayer.sendMessage(SKILL_MESSAGE_PREFIX_SPACES + renewText.replace("$1", alliesProtected + ""));
            }
        }
    }

    private static boolean hasAllies(Hero hero) {
        return hero.hasParty() && hero.getParty().getMembers().size() > 1;
    }


    public class DefenceInNumbersListener implements Listener {

        private Skill skill;

        public DefenceInNumbersListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onLeaveParty(HeroLeavePartyEvent event) {
            Hero joiningHero = event.getHero();
            if (joiningHero == null)
                return;

            // remove existing protection
            if (joiningHero.hasEffect(allyEffectName)) {
                joiningHero.removeEffect(joiningHero.getEffect(allyEffectName));
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onJoinParty(HeroJoinPartyEvent event) {
            Hero joiningHero = event.getHero();
            HeroParty party = event.getParty();

            if (joiningHero == null || party == null)
                return;

            //Check if a party member possesses this passive
            Hero defendingHero = null;
            for (Hero partyMember : party.getMembers()) {
                if (partyMember.hasEffect("DefenceInNumbers")) {
                    defendingHero = partyMember;
                }
            }

            if (defendingHero == null)
                return;

            Player defendingPlayer = defendingHero.getPlayer();

            int requiredAllyNumber = SkillConfigManager.getUseSetting(defendingHero, skill, "required-ally-number", 0, true);
            long period = SkillConfigManager.getUseSetting(defendingHero, skill, SkillSetting.PERIOD.node(), 5000, false);
            double radius = SkillConfigManager.getUseSetting(defendingHero, skill, SkillSetting.RADIUS.node(), 20, false);
            double radiusSquared = radius * radius;

            // remove existing protection
            if (joiningHero.hasEffect(allyEffectName)) {
                joiningHero.removeEffect(joiningHero.getEffect(allyEffectName));
            }

            // If in range apply effect
            Player joiningPlayer = joiningHero.getPlayer();
            if (defendingPlayer.getWorld().equals(joiningPlayer.getWorld())
                    && defendingPlayer.getLocation().distanceSquared(joiningPlayer.getLocation()) <= radiusSquared) {
                DefenceInNumbersAllyEffect effect = new DefenceInNumbersAllyEffect(
                        skill, allyEffectName, defendingPlayer, getIncomingMultiplier(defendingHero), period,
                        allyApplyText.replace("%hero%", defendingPlayer.getName()),
                        allyExpireText.replace("%hero%", defendingPlayer.getName()));
                joiningHero.addEffect(effect);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.getDamage() == 0 || !(event.getEntity() instanceof Player))
                return;

            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);

            int requiredAllyNumber = SkillConfigManager.getUseSetting(hero, skill,
                    "required-ally-number", 0, true);
//            if (hero.hasEffect("DefenceInNumbers") && hero.getParty().getMembers().size() >= requiredAllyNumber) {
            if (hero.hasEffect("DefenceInNumbers")) {
                event.setDamage(event.getDamage() * getIncomingMultiplier(hero));
            } else if (hero.hasEffect(allyEffectName)) {
                DefenceInNumbersAllyEffect allyEffect = (DefenceInNumbersAllyEffect) hero.getEffect(allyEffectName);
                event.setDamage(event.getDamage() * allyEffect.getIncomingMultiplier());
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if ((event.getDamage() == 0) || !(event.getEntity() instanceof Player))
                return;

            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);

            int requiredAllyNumber = SkillConfigManager.getUseSetting(hero, skill,
                    "required-ally-number", 0, true);
//            if (hero.hasEffect("DefenceInNumbers") && hero.getParty().getMembers().size() >= requiredAllyNumber) {
            if (hero.hasEffect("DefenceInNumbers")) {
                event.setDamage(event.getDamage() * getIncomingMultiplier(hero));
            } else if (hero.hasEffect(allyEffectName)) {
                DefenceInNumbersAllyEffect allyEffect = (DefenceInNumbersAllyEffect) hero.getEffect(allyEffectName);
                event.setDamage(event.getDamage() * allyEffect.getIncomingMultiplier());
            }
        }

    }

    public double getIncomingMultiplier(Hero defendingHero) {
        double incomingMultiplierBase = SkillConfigManager.getUseSetting(defendingHero, this,
                "incoming-multiplier-base", 1.0, true);
        double incomingMultiplierPerAlly = SkillConfigManager.getUseSetting(defendingHero, this,
                "incoming-multiplier-per-ally", .05, false);
        double incomingMultiplierImprovementWhenBuffed = SkillConfigManager.getUseSetting(defendingHero, this,
                "incoming-multiplier-improvement-when-buffed", .05, false);

        int numberOfAllies = defendingHero.hasParty() ? (defendingHero.getParty().getMembers().size()-1) : 0;
        double incomingMultiplier = incomingMultiplierBase - (numberOfAllies * incomingMultiplierPerAlly);
        if (incomingMultiplier < 0) {
            incomingMultiplier = 0;
        }
        return incomingMultiplier;
    }

    public class DefenceInNumbersAllyEffect extends ExpirableEffect {

        private double incomingMultiplier;

        public DefenceInNumbersAllyEffect(Skill skill, String name, Player applier, double incomingMultiplier,
                                          long duration, String applyText, String expireText) {
            super(skill, name, applier, duration, applyText, expireText);
            setEffectTypes(EffectType.BENEFICIAL);

            this.incomingMultiplier = incomingMultiplier;
        }

        public double getIncomingMultiplier() {
            return incomingMultiplier;
        }
    }

}