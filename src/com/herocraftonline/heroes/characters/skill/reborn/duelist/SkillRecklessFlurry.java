package com.herocraftonline.heroes.characters.skill.reborn.duelist;

import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillRecklessFlurry extends ActiveSkill {
    private String effectName = "RecklessFlurry";
    private String applyText;
    private String expireText;

    public SkillRecklessFlurry(Heroes plugin) {
        super(plugin, "RecklessFlurry");
        setDescription("Enter a reckless flurry for $1 second(s). "
                + "While in your flurry, your melee attacks pierce their damage immunity duration by $2%, effectively increasing your maximum attack speed. "
                + "During the effect, you also take $3% more damage from all sources.");
        setUsage("/skill recklessflurry");
        setNotes("NOTE: This ability is less effective when multiple people are attacking the same target at the same time.");
        setArgumentRange(0, 0);
        setIdentifiers("skill recklessflurry");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_PHYSICAL);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        double incomingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase", 0.5, false);
        double invulnFrameReductionPercent = SkillConfigManager.getUseSetting(hero, this, "invuln-frame-reduction-percent", 0.35, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedIncomingDamageIncrease = Util.decFormat.format(incomingDamageIncrease * 100);
        String formattedInvulnPiercePercent = Util.decFormat.format(invulnFrameReductionPercent * 100);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedInvulnPiercePercent).replace("$3", formattedIncomingDamageIncrease);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection conbfig = super.getDefaultConfig();
        conbfig.set("incoming-damage-increase", 0.25);
        conbfig.set("invuln-frame-reduction-percent", 0.35);
        conbfig.set(SkillSetting.DURATION.node(), 8000);
        conbfig.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has entered a reckless flurry!");
        conbfig.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer in a reckless flurry!");
        return conbfig;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has entered a reckless flurry!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer in a reckless flurry!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7000, false);

        double incomingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase", 0.5, false);
        double invulnFrameReductionPercent = SkillConfigManager.getUseSetting(hero, this, "invuln-frame-reduction-percent", 0.35, false);

        hero.addEffect(new RecklessFlurryEffect(this, player, duration, incomingDamageIncrease, invulnFrameReductionPercent));

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class RecklessFlurryEffect extends ExpirableEffect {
        private final double invulnFrameReductionPercent;
        private double incomingDamageIncrease;

        public RecklessFlurryEffect(Skill skill, Player applier, long duration, double incomingDamageIncrease, double invulnFrameReductionPercent) {
            super(skill, effectName, applier, duration, applyText, expireText);
            this.invulnFrameReductionPercent = invulnFrameReductionPercent;

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);

            this.incomingDamageIncrease = incomingDamageIncrease;
        }

        public double getIncomingDamageIncrease() {
            return incomingDamageIncrease;
        }

        public void setIncomingDamageIncrease(double incomingDamageIncrease) {
            this.incomingDamageIncrease = incomingDamageIncrease;
        }

        public double getInvulnFrameReductionPercent() {
            return invulnFrameReductionPercent;
        }
    }

    public class SkillHeroListener implements Listener {

        private final Skill skill;

        public SkillHeroListener(Skill skill) {

            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {

            // Handle incoming
            CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            if (defenderCT.hasEffect(effectName)) {
                RecklessFlurryEffect fEffect = (RecklessFlurryEffect) defenderCT.getEffect(effectName);

                double damageIncreasePercent = 1 + fEffect.getIncomingDamageIncrease();
                double newDamage = damageIncreasePercent * event.getDamage();
                event.setDamage(newDamage);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            CharacterTemplate attackerCT = event.getDamager();
            LivingEntity defenderLE = (LivingEntity) event.getEntity();

            // Handle outgoing
            if (attackerCT.hasEffect(effectName)) {
                RecklessFlurryEffect fEffect = (RecklessFlurryEffect) attackerCT.getEffect(effectName);

                double damageIncreasePercent = 1.0 + fEffect.getInvulnFrameReductionPercent();
                final LivingEntity attackerLE = attackerCT.getEntity();
                int noDamageTicks = Heroes.properties.noDamageTicks;
                int newInvulnTickValue = (int) ((1.0 - fEffect.getInvulnFrameReductionPercent()) * noDamageTicks) - 1;      // -1 tick because we will be using this variable 1 tick later

                new BukkitRunnable() {

                    @Override
                    public void run() {
                        attackerLE.sendMessage("Flurry Invuln Ticks Before: " + defenderLE.getNoDamageTicks());
                        defenderLE.setNoDamageTicks(newInvulnTickValue);
                        attackerLE.sendMessage("Flurry Invuln Ticks After: " + defenderLE.getNoDamageTicks());
                    }
                }.runTaskLater(plugin, 1);
            }

            // Handle incoming
            CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter(defenderLE);
            if (defenderCT.hasEffect(effectName)) {
                RecklessFlurryEffect fEffect = (RecklessFlurryEffect) defenderCT.getEffect(effectName);

                double damageIncreasePercent = 1 + fEffect.getIncomingDamageIncrease();
                double newDamage = damageIncreasePercent * event.getDamage();
                event.setDamage(newDamage);
            }
        }
    }
}
