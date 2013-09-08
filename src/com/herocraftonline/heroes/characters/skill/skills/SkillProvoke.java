package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillProvoke extends TargettedSkill {
    private String applyText;
    private String expireText;

    public SkillProvoke(Heroes plugin) {
        super(plugin, "Provoke");
        setDescription("Provoke your target for $1 seconds. Provoked targets gain $2% increased melee damage against you, but also take an additional $3% damage from all incoming melee attacks.");
        setUsage("/skill provoke");
        setArgumentRange(0, 0);
        setIdentifiers("skill provoke");
        setTypes(SkillType.DEBUFFING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.BUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(30000), false);

        double outgoingIncrease = SkillConfigManager.getUseSetting(hero, this, "outgoing-damage-increase-percent", Double.valueOf(0.25), false);
        double incomingIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase-percent", Double.valueOf(0.35), false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedOutgoingIncrease = Util.decFormat.format(outgoingIncrease * 100);
        String formattedIncomingIncrease = Util.decFormat.format(incomingIncrease * 100);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedIncomingIncrease).replace("$3", formattedOutgoingIncrease);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(4));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(6000));
        node.set("outgoing-damage-increase-percent", Double.valueOf(0.35));
        node.set("incoming-damage-increase-percent", Double.valueOf(0.20));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% was provoked by %hero%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% is no longer provoked!");

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%target% was provoked by %hero%!").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%target% is no longer provoked!").replace("%target%", "$1").replace("%hero%", "$2");
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(30000), false);
        double incomingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase-percent", Double.valueOf(0.25), false);
        double outgoingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "outgoing-damage-increase-percent", Double.valueOf(0.25), false);

        ProvokeEffect effect = new ProvokeEffect(this, player, duration, incomingDamageIncrease, outgoingDamageIncrease);

        plugin.getCharacterManager().getHero((Player) target).addEffect(effect);

        player.getWorld().playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 0.5F, 0.1F);

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        public SkillHeroListener() {}

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

    public class ProvokeEffect extends ExpirableEffect {
        private double incomingDamageIncrease;
        private double outgoingDamageIncrease;

        public ProvokeEffect(Skill skill, Player applier, long duration, double incomingDamageIncrease, double outgoingDamageIncrease) {
            super(skill, "Provoked", applier, duration, applyText, expireText);

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.HARMFUL);

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
