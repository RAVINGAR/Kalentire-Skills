package com.herocraftonline.heroes.characters.skill.skills;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.ProjectileSource;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

public class SkillHolyWater extends ActiveSkill {

    public SkillHolyWater(Heroes plugin) {
        super(plugin, "HolyWater");
        setDescription("You throw a potion of Holy Water at your target location. Allies affected by the potion are healed for $1 health. Undead affected by the potion will be dealt $2 damage instead.");
        setUsage("/skill holywater");
        setArgumentRange(0, 0);
        setIdentifiers("skill holywater");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.DAMAGING, SkillType.MULTI_GRESSIVE, SkillType.AREA_OF_EFFECT, SkillType.HEALING, SkillType.SILENCABLE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING.node(), Integer.valueOf(8), false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), Double.valueOf(1.0), false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

        double undeadDamage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", Integer.valueOf(45), false);
        double undeadDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "undead-damage-increase-per-wisdom", Double.valueOf(0.625), false);
        undeadDamage += (hero.getAttributeValue(AttributeType.WISDOM) * undeadDamageIncrease);

        String formattedHealing = Util.decFormat.format(healing);
        String formattedDamage = Util.decFormat.format(undeadDamage);

        return getDescription().replace("$1", formattedHealing).replace("$2", formattedDamage);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.HEALING.node(), Integer.valueOf(75));
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), Double.valueOf(1.875));
        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(7));
        node.set("undead-damage", Integer.valueOf(45));
        node.set("undead-damage-increase-per-wisdom", Double.valueOf(0.625));
        node.set("velocity-multiplier", Double.valueOf(1.5));

        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        ThrownPotion pot = (ThrownPotion) player.launchProjectile(ThrownPotion.class);
        pot.setMetadata("SkillAmpul", new FixedMetadataValue(plugin, (Boolean) true));
        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", Double.valueOf(1.5), false);
        pot.setVelocity(pot.getVelocity().multiply(mult));

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {
        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPotionSplash(PotionSplashEvent event) {

            if (!event.getPotion().hasMetadata("SkillAmpul"))
                return;

            List<MetadataValue> meta = event.getPotion().getMetadata("SkillAmpul");
            if (meta.size() != 1) {
                Heroes.log(Level.WARNING, "Heroes Skill \"Ampul\" encountered an error with metadata - has something else been manipulating it?");
                event.getPotion().removeMetadata("SkillAmpul", plugin);
                return;
            }
            else {
                if (meta.get(0).asBoolean() != true) {
                    event.getPotion().removeMetadata("SkillAmpul", plugin);
                    return;
                }

            }
            event.getPotion().removeMetadata("SkillAmpul", plugin);

            ProjectileSource source = ((Projectile) event.getDamager()).getShooter();
            if (!(source instanceof Entity))
                return;
            Entity shooter = (LivingEntity) source;
            if (!(shooter instanceof Player))
                return;

            Player player = (Player) shooter;
            Hero hero = plugin.getCharacterManager().getHero(player);

            int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, Integer.valueOf(7), false);

            double healing = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.HEALING.node(), Integer.valueOf(85), false);
            double healingIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), Double.valueOf(1.0), false);
            healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

            double undeadDamage = SkillConfigManager.getUseSetting(hero, skill, "undead-damage", Integer.valueOf(45), false);
            double undeadDamageIncrease = SkillConfigManager.getUseSetting(hero, skill, "undead-damage-increase-per-wisdom", Double.valueOf(0.625), false);
            undeadDamage += (hero.getAttributeValue(AttributeType.WISDOM) * undeadDamageIncrease);

            Set<Hero> partyMembers = null;
            if (hero.hasParty())
                partyMembers = hero.getParty().getMembers();

            // We could just grab the affected entities by the splash, but the radius is reeeaally small, so we're just gonna use our own radius.
            List<Entity> entities = event.getEntity().getNearbyEntities(radius, radius, radius);
            for (Entity entity : entities) {
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }

                if (!(entity instanceof Player)) {
                    // If not, we need to check to see if they are undead
                    if (!(Util.isUndead(plugin, (LivingEntity) entity)))
                        continue;
                    else {
                        // If they are undead, damage them.
                        addSpellTarget((LivingEntity) entity, hero);
                        Skill.damageEntity((LivingEntity) entity, shooter, undeadDamage, DamageCause.MAGIC);
                    }
                }
                else {
                    // If we found a player, check to see if they are in the shooter's party.
                    Hero targetHero = plugin.getCharacterManager().getHero(((Player) entity));
                    if (partyMembers == null) {
                        // They do not have a party. Check to see if they hit themselves with the potion.
                        if (hero.equals(targetHero)) {
                            // Self heal
                            HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(targetHero, healing, skill, hero);
                            Bukkit.getPluginManager().callEvent(healEvent);
                            if (!healEvent.isCancelled())
                                hero.heal(healEvent.getAmount());
                        }
                    }
                    else {
                        // Hero has a party. Check to see if the current entity matches any of them.
                        // They do not have a party. Check to see if they hit themselves with the potion.
                        if (partyMembers.contains(targetHero)) {
                            // Heal party member.
                            HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(targetHero, healing, skill, hero);
                            Bukkit.getPluginManager().callEvent(healEvent);
                            if (!healEvent.isCancelled())
                                targetHero.heal(healEvent.getAmount());
                        }
                    }
                }
            }
        }
    }
}