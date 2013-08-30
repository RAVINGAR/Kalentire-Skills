package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

public class SkillFistOfJin extends PassiveSkill {

    public SkillFistOfJin(Heroes plugin) {
        super(plugin, "FistOfJin");
        setDescription("Passive: Each of your melee strikes restore $1 health to you, and $2 health to party members within $3 blocks. You cannot heal more than once per $4 seconds.");
        setTypes(SkillType.SILENCABLE, SkillType.HEALING, SkillType.AREA_OF_EFFECT);
        setEffectTypes(EffectType.HEALING, EffectType.BENEFICIAL, EffectType.MAGIC);

        Bukkit.getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        double cdDuration = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, "healing-internal-cooldown", Double.valueOf(1000.0), false) / 1000.0);

        double selfHeal = SkillConfigManager.getUseSetting(hero, this, "heal-per-hit-self", Integer.valueOf(8), false);
        double partyHeal = SkillConfigManager.getUseSetting(hero, this, "heal-per-hit-party", Integer.valueOf(3), false);

        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.15, false);
        double calculatedIncrease = (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);
        selfHeal += calculatedIncrease;
        partyHeal += calculatedIncrease;

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(8), false);
        double radiusIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE_PER_WISDOM, Double.valueOf(0.1), false);
        radius += (int) (radiusIncrease * hero.getAttributeValue(AttributeType.WISDOM));

        return getDescription().replace("$1", selfHeal + "").replace("$2", partyHeal + "").replace("$3", radius + "").replace("$4", cdDuration + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set("healing-internal-cooldown", Integer.valueOf(1000));
        node.set("heal-per-hit-self", Integer.valueOf(8));
        node.set("heal-per-hit-party", Integer.valueOf(3));
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.15);
        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(6));
        node.set(SkillSetting.RADIUS_INCREASE_PER_WISDOM.node(), Double.valueOf(0.1));

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

            // Make sure they are actually dealing damage to the target.
            if (!damageCheck(player, (LivingEntity) event.getEntity())) {
                return;
            }

            if (!hero.canUseSkill(skill))
                return;

            if (hero.hasEffect("FistOfJinCooldownEffect"))      // On cooldown, don't heal.
                return;

            Material item = player.getItemInHand().getType();

            if (!SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.tools).contains(item.name()))
                return;

            double selfHeal = SkillConfigManager.getUseSetting(hero, skill, "heal-per-hit-self", Integer.valueOf(8), false);
            double partyHeal = SkillConfigManager.getUseSetting(hero, skill, "heal-per-hit-party", Integer.valueOf(3), false);

            double healingIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.15, false);
            double calculatedIncrease = (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);
            selfHeal += calculatedIncrease;
            partyHeal += calculatedIncrease;

            int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, Integer.valueOf(8), false);
            double radiusIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS_INCREASE_PER_WISDOM, Double.valueOf(0.1), false);
            radius += (int) (radiusIncrease * hero.getAttributeValue(AttributeType.WISDOM));
            int radiusSquared = radius * radius;

            int cdDuration = SkillConfigManager.getUseSetting(hero, skill, "healing-internal-cooldown", Integer.valueOf(1000), false);

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
                                hero.heal(healEvent.getAmount());
                                CooldownEffect cdEffect = new CooldownEffect(skill, player, cdDuration);
                                hero.addEffect(cdEffect);
                            }
                        }
                        else if (memberLocation.distanceSquared(playerLocation) <= radiusSquared) {
                            // Check to see if they are close enough to the player to receive healing

                            HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(member, partyHeal, skill, hero);
                            Bukkit.getPluginManager().callEvent(healEvent);
                            if (!healEvent.isCancelled()) {
                                member.heal(healEvent.getAmount());
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
                    hero.heal(healEvent.getAmount());
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
}