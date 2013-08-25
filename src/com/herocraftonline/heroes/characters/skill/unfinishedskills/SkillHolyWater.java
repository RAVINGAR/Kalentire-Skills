package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillHolyWater extends ActiveSkill {

    public SkillHolyWater(Heroes plugin) {
        super(plugin, "HolyWater");
        setDescription("You throw a potion of Holy Water at your target location. Allies affected by the potion are healed for $1 health. Undead affected by the potion will be dealt $2 damage instead.");
        setUsage("/skill holywater");
        setArgumentRange(0, 0);
        setIdentifiers("skill holywater");
        setTypes(SkillType.LIGHT, SkillType.HEAL, SkillType.SILENCABLE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        int healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH, 100, false);
        double undeadDamage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", 50, false);

        return getDescription().replace("$1", healing + "").replace("$2", undeadDamage + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.HEALTH.node(), 100);
        node.set("undead-damage", 50);
        node.set("velocity-multiplier", 1.5D);
        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        ThrownPotion pot = (ThrownPotion) player.launchProjectile(ThrownPotion.class);
        pot.setMetadata("SkillAmpul", new FixedMetadataValue(plugin, (Boolean) true));
        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 2.5D, false);
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

            LivingEntity shooter = event.getPotion().getShooter();
            if (!(shooter instanceof Player))
                return;

            Player player = (Player) shooter;
            Hero hero = plugin.getCharacterManager().getHero(player);

            double healing = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.HEALTH, 100, false);
            double undeadDamage = SkillConfigManager.getUseSetting(hero, skill, "undead-damage", 50, false);

            Set<Hero> partyMembers = null;
            if (hero.hasParty())
                partyMembers = hero.getParty().getMembers();

            Collection<LivingEntity> entities = event.getAffectedEntities();
            for (LivingEntity lEntity : entities) {
                // Check to see if entity is a player
                if (!(lEntity instanceof Player)) {
                    // If not, we need to check to see if they are undead
                    if (!(isUndead(lEntity)))
                        continue;
                    else {
                        // If they are undead, damage them.
                        addSpellTarget(lEntity, hero);
                        Skill.damageEntity(lEntity, shooter, undeadDamage, DamageCause.MAGIC);
                    }
                }
                else {
                    // If we found a player, check to see if they are in the shooter's party.
                    Hero targetHero = plugin.getCharacterManager().getHero(((Player) lEntity));
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

    private boolean isUndead(Entity entity) {
        return entity instanceof Zombie || entity instanceof Skeleton || entity instanceof PigZombie || entity instanceof Ghast;
    }
}