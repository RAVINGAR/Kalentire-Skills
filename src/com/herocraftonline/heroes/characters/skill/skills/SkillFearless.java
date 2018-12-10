package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.FormEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;

public class SkillFearless extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillFearless(Heroes plugin) {
        super(plugin, "Fearless");
        setDescription("You become fearless, gaining $1% increased damage. However, your wreckless behavior causes you to take an additional $2% damage from all sources!");
        setUsage("/skill fearless");
        setArgumentRange(0, 0);
        setIdentifiers("skill fearless");
        setTypes(SkillType.FORM_ALTERING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.BUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    public String getDescription(Hero hero) {
        double outgoingIncrease = SkillConfigManager.getUseSetting(hero, this, "outgoing-damage-increase-percent", 0.25, false);
        double incomingIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase-percent", 0.35, false);

        String formattedOutgoingIncrease = Util.decFormat.format(outgoingIncrease * 100);
        String formattedIncomingIncrease = Util.decFormat.format(incomingIncrease * 100);

        return getDescription().replace("$1", formattedIncomingIncrease).replace("$2", formattedOutgoingIncrease);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.USE_TEXT.node(), "");
        node.set("outgoing-damage-increase-percent", 0.25);
        node.set("incoming-damage-increase-percent", 0.35);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is fearless!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer fearless!");

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is fearless!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer fearless!").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        if (hero.hasEffect("Fearless")) {
            hero.removeEffect(hero.getEffect("Fearless"));
            return SkillResult.REMOVED_EFFECT;
        }
        else {
            Player player = hero.getPlayer();

            broadcastExecuteText(hero);

            double incomingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase-percent", 0.25, false);
            double outgoingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "outgoing-damage-increase-percent", 0.25, false);
            hero.addEffect(new FearlessEffect(this, incomingDamageIncrease, outgoingDamageIncrease));

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5F, 0.1F);
        }

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        public SkillHeroListener() {}

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {

            // Handle outgoing
            CharacterTemplate attackerCT = event.getDamager();
            if (attackerCT.hasEffect("Fearless")) {
                FearlessEffect fEffect = (FearlessEffect) attackerCT.getEffect("Fearless");

                double damageIncreasePercent = 1 + fEffect.getOutgoingDamageIncrease();
                double newDamage = damageIncreasePercent * event.getDamage();
                event.setDamage(newDamage);
            }

            // Handle incoming
            if (event.getEntity() instanceof LivingEntity) {
                CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
                if (defenderCT.hasEffect("Fearless")) {
                    FearlessEffect fEffect = (FearlessEffect) defenderCT.getEffect("Fearless");

                    double damageIncreasePercent = 1 + fEffect.getIncomingDamageIncrease();
                    double newDamage = damageIncreasePercent * event.getDamage();
                    event.setDamage(newDamage);
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {

            // Handle outgoing
            CharacterTemplate attackerCT = event.getDamager();
            if (attackerCT.hasEffect("Fearless")) {
                FearlessEffect fEffect = (FearlessEffect) attackerCT.getEffect("Fearless");

                double damageIncreasePercent = 1 + fEffect.getOutgoingDamageIncrease();
                double newDamage = damageIncreasePercent * event.getDamage();
                event.setDamage(newDamage);
            }

            // Handle incoming
            if (event.getEntity() instanceof LivingEntity) {
                CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
                if (defenderCT.hasEffect("Fearless")) {
                    FearlessEffect fEffect = (FearlessEffect) defenderCT.getEffect("Fearless");

                    double damageIncreasePercent = 1 + fEffect.getIncomingDamageIncrease();
                    double newDamage = damageIncreasePercent * event.getDamage();
                    event.setDamage(newDamage);
                }
            }
        }
    }

    public class FearlessEffect extends FormEffect {
        private double incomingDamageIncrease;
        private double outgoingDamageIncrease;

        public FearlessEffect(Skill skill, double incomingDamageIncrease, double outgoingDamageIncrease) {
            super(skill, "Fearless", applyText, expireText);

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);

            this.incomingDamageIncrease = incomingDamageIncrease;
            this.outgoingDamageIncrease = outgoingDamageIncrease;
        }

        public double getIncomingDamageIncrease() {
            return incomingDamageIncrease;
        }

        public void setIncomingDamageIncrease(double incomingDamageIncrease) {
            this.incomingDamageIncrease = incomingDamageIncrease;
        }

        public double getOutgoingDamageIncrease() {
            return outgoingDamageIncrease;
        }

        public void setOutgoingDamageIncrease(double outgoingDamageIncrease) {
            this.outgoingDamageIncrease = outgoingDamageIncrease;
        }
    }
}
