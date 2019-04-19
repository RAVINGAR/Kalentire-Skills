package com.herocraftonline.heroes.characters.skill.pack8;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.scaling.ExpressionScaling;
import com.herocraftonline.heroes.characters.classes.scaling.Scaling;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class SkillFistOfJin extends PassiveSkill {

    public SkillFistOfJin(Heroes plugin) {
        super(plugin, "FistOfJin");
        setDescription("Each of your melee strikes restore $1 health to you, and $2 health to party members within $3 blocks. You cannot heal more than once per $4 second(s).");
        setTypes(SkillType.SILENCEABLE, SkillType.HEALING, SkillType.AREA_OF_EFFECT);
        setEffectTypes(EffectType.HEALING, EffectType.BENEFICIAL, EffectType.MAGIC);

        Bukkit.getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        double cdDuration = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, "healing-internal-cooldown", 1000.0, false) / 1000.0);

        double selfHeal = SkillConfigManager.getUseSetting(hero, this, "heal-per-hit-self", 8, false);
        selfHeal = getScaledHealing(hero, selfHeal);
        double partyHeal = SkillConfigManager.getUseSetting(hero, this, "heal-per-hit-party", 3, false);
        partyHeal = getScaledHealing(hero, partyHeal);

        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.15, false);
        double calculatedIncrease = (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);
        selfHeal += calculatedIncrease;
        partyHeal += calculatedIncrease;

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 8, false);
        double radiusIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE_PER_WISDOM, 0.1, false);
        radius += (int) (radiusIncrease * hero.getAttributeValue(AttributeType.WISDOM));

        return getDescription().replace("$1", ((int)selfHeal) + "").replace("$2", ((int)partyHeal) + "").replace("$3", radius + "").replace("$4", cdDuration + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set("healing-internal-cooldown", 1000);
        node.set("heal-per-hit-self", 8);
        node.set("heal-per-hit-party", 3);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.15);
        node.set(SkillSetting.RADIUS.node(), 6);
        node.set(SkillSetting.RADIUS_INCREASE_PER_WISDOM.node(), 0.1);

        // heal scaling parameters for use in children skills
        node.set(SkillSetting.IS_SCALED_HEALING.node(), false);
        node.set(SkillSetting.HEALING_SCALE_EXPRESSION.node(), "1");

        List<String> weaponList = new ArrayList<String>(5);
        weaponList.add("AIR");
        weaponList.add("STICK");
        weaponList.add("FISHING_ROD");
        weaponList.add("RAW_FISH");
        weaponList.add("BLAZE_ROD");

        node.set("weapons", weaponList);

        return node;
    }

    private class SkillHeroListener implements Listener {
        private Skill skill;

        public SkillHeroListener(Skill skill) {
            this.skill = skill;

        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (!(event.getDamager() instanceof Hero) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            Hero hero = (Hero) event.getDamager();
            Player player = hero.getPlayer();

            if (!hero.canUseSkill(skill))
                return;

            if (hero.hasEffect("FistOfJinCooldownEffect"))      // On cooldown, don't heal.
                return;

            // Make sure they are actually dealing damage to the target.
            if (!damageCheck(player, (LivingEntity) event.getEntity())) {
                return;
            }

            if (!(event.getAttackerEntity() instanceof Arrow)) {
                Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
                if (!SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.tools).contains(item.name()))
                    return;
            }

            int wisdom = hero.getAttributeValue(AttributeType.WISDOM);

            double selfHeal = SkillConfigManager.getUseSetting(hero, skill, "heal-per-hit-self", 8, false);
            selfHeal = getScaledHealing(hero, selfHeal);
            double partyHeal = SkillConfigManager.getUseSetting(hero, skill, "heal-per-hit-party", 3, false);
            partyHeal = getScaledHealing(hero, partyHeal);

            double healingIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.15, false);
            double calculatedIncrease = wisdom * healingIncrease;
            selfHeal += calculatedIncrease;
            partyHeal += calculatedIncrease;

            int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 8, false);
            double radiusIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS_INCREASE_PER_WISDOM, 0.1, false);
            radius += (int) Math.floor(radiusIncrease * wisdom);
            int radiusSquared = radius * radius;

            int cdDuration = SkillConfigManager.getUseSetting(hero, skill, "healing-internal-cooldown", 1000, false);

            // Check if the hero has a party
            if (hero.hasParty()) {
                Location playerLocation = player.getLocation();
                // Loop through the player's party members and heal as necessary
                for (Hero member : hero.getParty().getMembers()) {
                    Location memberLocation = member.getPlayer().getLocation();

                    // Ensure the party member is in the same world.
                    if (memberLocation.getWorld().equals(playerLocation.getWorld())) {

                        // Check to see if we're dealing with the hero himself.
                        if (member.equals(hero)) {

                            HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(hero, selfHeal, skill);     // Bypass self-heal nerf.
                            Bukkit.getPluginManager().callEvent(healEvent);
                            if (!healEvent.isCancelled()) {
                                hero.heal(healEvent.getDelta());
                                CooldownEffect cdEffect = new CooldownEffect(skill, player, cdDuration);
                                hero.addEffect(cdEffect);
                            }
                        }
                        else if (memberLocation.distanceSquared(playerLocation) <= radiusSquared) {
                            // Check to see if they are close enough to the player to receive healing

                            HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(member, partyHeal, skill, hero);
                            Bukkit.getPluginManager().callEvent(healEvent);
                            if (!healEvent.isCancelled()) {
                                member.heal(healEvent.getDelta());
                                CooldownEffect cdEffect = new CooldownEffect(skill, player, cdDuration);
                                hero.addEffect(cdEffect);
                            }
                        }
                    }
                }
            }
            else {
                HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(hero, selfHeal, skill);     // Bypass self-heal nerf
                Bukkit.getPluginManager().callEvent(healEvent);
                if (!healEvent.isCancelled()) {
                    hero.heal(healEvent.getDelta());
                    CooldownEffect cdEffect = new CooldownEffect(skill, player, cdDuration);
                    hero.addEffect(cdEffect);
                }
            }
        }
    }

    // Effect required for implementing an internal cooldown on healing
    private class CooldownEffect extends ExpirableEffect {
        public CooldownEffect(Skill skill, Player applier, long duration) {
            super(skill, "FistOfJinCooldownEffect", applier, duration);
        }
    }

    /**
     * @param hero the {@link Hero} using the skill
     * @param healing the base healing rate to scale
     * @return if health scaling enabled with a scaling expression return healing * expression scaling, otherwise return base healing.
     */
    public double getScaledHealing(Hero hero, double healing) {
        boolean scaledHealing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.IS_SCALED_HEALING.node(), false);
        if (scaledHealing){
            String expression = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_SCALE_EXPRESSION.node(),"1");
            if (!expression.equals("1")) {
                Scaling healingScaling = new ExpressionScaling(hero.getHeroClass(), expression);
                healing = healing * healingScaling.getScaled(hero);
            }
        }
        return healing;
    }
}