package com.herocraftonline.heroes.characters.skill.pack2;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

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
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;

public class SkillProvoke extends TargettedSkill {

    private String applyText;
    private String expireText;
    private String provokeText;

    public SkillProvoke(Heroes plugin) {
        super(plugin, "Provoke");
        setDescription("Provoke your target for $1 seconds. Provoked targets gain $2% increased damage against you, but also take an additional $3% damage from all incoming physical attacks.");
        setUsage("/skill provoke");
        setArgumentRange(0, 0);
        setIdentifiers("skill provoke");
        setTypes(SkillType.DEBUFFING, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_PHYSICAL);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);

        double outgoingIncrease = SkillConfigManager.getUseSetting(hero, this, "outgoing-damage-increase-percent", 0.25, false);
        double incomingIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase-percent", 0.35, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedOutgoingIncrease = Util.decFormat.format(outgoingIncrease * 100);
        String formattedIncomingIncrease = Util.decFormat.format(incomingIncrease * 100);

        return getDescription().replace("$1", formattedDuration).replace("$3", formattedOutgoingIncrease).replace("$2", formattedIncomingIncrease);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

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

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% was provoked by %hero%!").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% is no longer provoked!").replace("%target%", "$1").replace("%hero%", "$2");
        provokeText = SkillConfigManager.getRaw(this, "provoke-text", "%hero% is provoking you!");
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, "provoke-message-speed", 1000, false);
        double incomingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase-percent", 0.25, false);
        double outgoingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "outgoing-damage-increase-percent", 0.25, false);

        ProvokeEffect effect = new ProvokeEffect(this, player, period, duration, incomingDamageIncrease, outgoingDamageIncrease);

        plugin.getCharacterManager().getCharacter(target).addEffect(effect);

        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_ENDERDRAGON_GROWL.value(), 0.5F, 0.1F);

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        public SkillHeroListener() {}

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {

            // Handle outgoing
            CharacterTemplate attackerCT = event.getDamager();
            if (attackerCT.hasEffect("Provoked")) {
                if (event.getEntity() instanceof Player) {
                    Player defenderPlayer = (Player) event.getEntity();

                    ProvokeEffect pEffect = (ProvokeEffect) attackerCT.getEffect("Provoked");

                    if (pEffect.getApplier().equals(defenderPlayer)) {
                        double damageIncreasePercent = 1 + pEffect.getOutgoingDamageIncrease();
                        double newDamage = damageIncreasePercent * event.getDamage();
                        event.setDamage(newDamage);
                    }
                }
            }

            // Handle incoming
            if (event.getSkill().isType(SkillType.ABILITY_PROPERTY_PHYSICAL) && event.getSkill().isType(SkillType.DAMAGING)) {
                CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
                if (defenderCT.hasEffect("Provoked")) {
                    ProvokeEffect fEffect = (ProvokeEffect) defenderCT.getEffect("Provoked");

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
            if (attackerCT.hasEffect("Provoked")) {
                if (event.getEntity() instanceof Player) {
                    Player defenderPlayer = (Player) event.getEntity();

                    ProvokeEffect pEffect = (ProvokeEffect) attackerCT.getEffect("Provoked");

                    if (pEffect.getApplier().equals(defenderPlayer)) {
                        double damageIncreasePercent = 1 + pEffect.getOutgoingDamageIncrease();
                        double newDamage = damageIncreasePercent * event.getDamage();
                        event.setDamage(newDamage);
                    }
                }
            }

            if (!(event.getEntity() instanceof LivingEntity))
                return;

            // Handle incoming
            CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            if (defenderCT.hasEffect("Provoked")) {
                ProvokeEffect pEffect = (ProvokeEffect) defenderCT.getEffect("Provoked");

                double damageIncreasePercent = 1 + pEffect.getIncomingDamageIncrease();
                double newDamage = damageIncreasePercent * event.getDamage();
                event.setDamage(newDamage);
            }
        }
    }

    public class ProvokeEffect extends PeriodicExpirableEffect {
        private double incomingDamageIncrease;
        private double outgoingDamageIncrease;

        public ProvokeEffect(Skill skill, Player applier, long period, long duration, double incomingDamageIncrease, double outgoingDamageIncrease) {
            super(skill, "Provoked", applier, period, duration, applyText, expireText); //TODO Implicit broadcast() call - may need changes?

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.HARMFUL);

            this.incomingDamageIncrease = incomingDamageIncrease;
            this.outgoingDamageIncrease = outgoingDamageIncrease;
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();

            player.sendMessage(provokeText.replace("%hero%", ChatColor.BOLD + applier.getName() + ChatColor.RESET));
        }

        @Override
        public void tickMonster(Monster monster) {}

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
