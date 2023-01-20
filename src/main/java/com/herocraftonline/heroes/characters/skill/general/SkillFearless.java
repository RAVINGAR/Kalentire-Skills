package com.herocraftonline.heroes.characters.skill.general;

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
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillFearless extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillFearless(final Heroes plugin) {
        super(plugin, "Fearless");
        setDescription("You become fearless, gaining $1% increased damage. However, your wreckless behavior causes you to take an additional $2% damage from all sources!");
        setUsage("/skill fearless");
        setArgumentRange(0, 0);
        setIdentifiers("skill fearless");
        setTypes(SkillType.FORM_ALTERING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.BUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double outgoingIncrease = SkillConfigManager.getUseSetting(hero, this, "outgoing-damage-increase-percent", 0.25, false);
        final double incomingIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase-percent", 0.35, false);

        final String formattedOutgoingIncrease = Util.decFormat.format(outgoingIncrease * 100);
        final String formattedIncomingIncrease = Util.decFormat.format(incomingIncrease * 100);

        return getDescription().replace("$1", formattedIncomingIncrease).replace("$2", formattedOutgoingIncrease);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.USE_TEXT.node(), "");
        node.set("outgoing-damage-increase-percent", 0.25);
        node.set("incoming-damage-increase-percent", 0.35);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is fearless!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer fearless!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is fearless!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer fearless!").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        if (hero.hasEffect("Fearless")) {
            hero.removeEffect(hero.getEffect("Fearless"));
            return SkillResult.REMOVED_EFFECT;
        } else {
            final Player player = hero.getPlayer();

            broadcastExecuteText(hero);

            final double incomingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase-percent", 0.25, false);
            final double outgoingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "outgoing-damage-increase-percent", 0.25, false);
            hero.addEffect(new FearlessEffect(this, incomingDamageIncrease, outgoingDamageIncrease));

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5F, 0.1F);
        }

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        public SkillHeroListener() {
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onSkillDamage(final SkillDamageEvent event) {

            // Handle outgoing
            final CharacterTemplate attackerCT = event.getDamager();
            if (attackerCT.hasEffect("Fearless")) {
                final FearlessEffect fEffect = (FearlessEffect) attackerCT.getEffect("Fearless");

                final double damageIncreasePercent = 1 + fEffect.getOutgoingDamageIncrease();
                final double newDamage = damageIncreasePercent * event.getDamage();
                event.setDamage(newDamage);
            }

            // Handle incoming
            if (event.getEntity() instanceof LivingEntity) {
                final CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
                if (defenderCT.hasEffect("Fearless")) {
                    final FearlessEffect fEffect = (FearlessEffect) defenderCT.getEffect("Fearless");

                    final double damageIncreasePercent = 1 + fEffect.getIncomingDamageIncrease();
                    final double newDamage = damageIncreasePercent * event.getDamage();
                    event.setDamage(newDamage);
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(final WeaponDamageEvent event) {

            // Handle outgoing
            final CharacterTemplate attackerCT = event.getDamager();
            if (attackerCT.hasEffect("Fearless")) {
                final FearlessEffect fEffect = (FearlessEffect) attackerCT.getEffect("Fearless");

                final double damageIncreasePercent = 1 + fEffect.getOutgoingDamageIncrease();
                final double newDamage = damageIncreasePercent * event.getDamage();
                event.setDamage(newDamage);
            }

            // Handle incoming
            if (event.getEntity() instanceof LivingEntity) {
                final CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
                if (defenderCT.hasEffect("Fearless")) {
                    final FearlessEffect fEffect = (FearlessEffect) defenderCT.getEffect("Fearless");

                    final double damageIncreasePercent = 1 + fEffect.getIncomingDamageIncrease();
                    final double newDamage = damageIncreasePercent * event.getDamage();
                    event.setDamage(newDamage);
                }
            }
        }
    }

    public class FearlessEffect extends FormEffect {
        private double incomingDamageIncrease;
        private double outgoingDamageIncrease;

        public FearlessEffect(final Skill skill, final double incomingDamageIncrease, final double outgoingDamageIncrease) {
            super(skill, "Fearless", applyText, expireText);

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);

            this.incomingDamageIncrease = incomingDamageIncrease;
            this.outgoingDamageIncrease = outgoingDamageIncrease;
        }

        public double getIncomingDamageIncrease() {
            return incomingDamageIncrease;
        }

        public void setIncomingDamageIncrease(final double incomingDamageIncrease) {
            this.incomingDamageIncrease = incomingDamageIncrease;
        }

        public double getOutgoingDamageIncrease() {
            return outgoingDamageIncrease;
        }

        public void setOutgoingDamageIncrease(final double outgoingDamageIncrease) {
            this.outgoingDamageIncrease = outgoingDamageIncrease;
        }
    }
}
