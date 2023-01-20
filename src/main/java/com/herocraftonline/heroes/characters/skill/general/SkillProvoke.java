package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillProvoke extends TargettedSkill {

    private String applyText;
    private String expireText;
    private String provokeText;

    public SkillProvoke(final Heroes plugin) {
        super(plugin, "Provoke");
        setDescription("Provoke your target for $1 second(s). Provoked targets gain $2% increased damage against you, but also take an additional $3% damage from all incoming physical attacks.");
        setUsage("/skill provoke");
        setArgumentRange(0, 0);
        setIdentifiers("skill provoke");
        setTypes(SkillType.DEBUFFING, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_PHYSICAL);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);

        final double outgoingIncrease = SkillConfigManager.getUseSetting(hero, this, "outgoing-damage-increase-percent", 0.25, false);
        final double incomingIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase-percent", 0.35, false);

        final String formattedDuration = Util.decFormat.format(duration / 1000.0);
        final String formattedOutgoingIncrease = Util.decFormat.format(outgoingIncrease * 100);
        final String formattedIncomingIncrease = Util.decFormat.format(incomingIncrease * 100);

        return getDescription().replace("$1", formattedDuration).replace("$3", formattedOutgoingIncrease).replace("$2", formattedIncomingIncrease);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set(SkillSetting.DURATION.node(), 6000);
        node.set("outgoing-damage-increase-percent", 0.35);
        node.set("incoming-damage-increase-percent", 0.20);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% was provoked by %hero%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer provoked!");
        node.set("provoke-message-speed", 1000);
        node.set("provoke-text", "%hero% is provoking you!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% was provoked by %hero%!").replace("%target%", "$1").replace("$target$", "$1").replace("%hero%", "$2").replace("$hero$", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% is no longer provoked!").replace("%target%", "$1").replace("$target$", "$1").replace("%hero%", "$2").replace("$hero$", "$2");
        provokeText = SkillConfigManager.getRaw(this, "provoke-text", "%hero% is provoking you!");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {

        final Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, "provoke-message-speed", 1000, false);
        final double incomingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase-percent", 0.25, false);
        final double outgoingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "outgoing-damage-increase-percent", 0.25, false);

        final ProvokeEffect effect = new ProvokeEffect(this, player, period, duration, incomingDamageIncrease, outgoingDamageIncrease);

        plugin.getCharacterManager().getCharacter(target).addEffect(effect);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5F, 0.1F);

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        public SkillHeroListener() {
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(final SkillDamageEvent event) {

            // Handle outgoing
            final CharacterTemplate attackerCT = event.getDamager();
            if (attackerCT.hasEffect("Provoked")) {
                if (event.getEntity() instanceof Player) {
                    final Player defenderPlayer = (Player) event.getEntity();

                    final ProvokeEffect pEffect = (ProvokeEffect) attackerCT.getEffect("Provoked");

                    if (pEffect.getApplier().equals(defenderPlayer)) {
                        final double damageIncreasePercent = 1 + pEffect.getOutgoingDamageIncrease();
                        final double newDamage = damageIncreasePercent * event.getDamage();
                        event.setDamage(newDamage);
                    }
                }
            }

            // Handle incoming
            if (event.getSkill().isType(SkillType.ABILITY_PROPERTY_PHYSICAL) && event.getSkill().isType(SkillType.DAMAGING)) {
                final CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
                if (defenderCT.hasEffect("Provoked")) {
                    final ProvokeEffect fEffect = (ProvokeEffect) defenderCT.getEffect("Provoked");

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
            if (attackerCT.hasEffect("Provoked")) {
                if (event.getEntity() instanceof Player) {
                    final Player defenderPlayer = (Player) event.getEntity();

                    final ProvokeEffect pEffect = (ProvokeEffect) attackerCT.getEffect("Provoked");

                    if (pEffect.getApplier().equals(defenderPlayer)) {
                        final double damageIncreasePercent = 1 + pEffect.getOutgoingDamageIncrease();
                        final double newDamage = damageIncreasePercent * event.getDamage();
                        event.setDamage(newDamage);
                    }
                }
            }

            if (!(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            // Handle incoming
            final CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            if (defenderCT.hasEffect("Provoked")) {
                final ProvokeEffect pEffect = (ProvokeEffect) defenderCT.getEffect("Provoked");

                final double damageIncreasePercent = 1 + pEffect.getIncomingDamageIncrease();
                final double newDamage = damageIncreasePercent * event.getDamage();
                event.setDamage(newDamage);
            }
        }
    }

    public class ProvokeEffect extends PeriodicExpirableEffect {
        private double incomingDamageIncrease;
        private double outgoingDamageIncrease;

        public ProvokeEffect(final Skill skill, final Player applier, final long period, final long duration, final double incomingDamageIncrease, final double outgoingDamageIncrease) {
            super(skill, "Provoked", applier, period, duration, applyText, expireText); //TODO Implicit broadcast() call - may need changes?

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.HARMFUL);

            this.incomingDamageIncrease = incomingDamageIncrease;
            this.outgoingDamageIncrease = outgoingDamageIncrease;
        }

        @Override
        public void tickHero(final Hero hero) {
            final Player player = hero.getPlayer();

            player.sendMessage(provokeText.replace("%hero%", ChatColor.BOLD + applier.getName() + ChatColor.RESET));
        }

        @Override
        public void tickMonster(final Monster monster) {
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
