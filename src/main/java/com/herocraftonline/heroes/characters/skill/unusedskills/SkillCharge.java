package com.herocraftonline.heroes.characters.skill.unusedskills;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillCharge extends TargettedSkill {

    private final Set<String> chargingPlayers = new HashSet<>();

    public SkillCharge(Heroes plugin) {
        super(plugin, "Charge");
        setDescription("You charge toward your target!");
        setUsage("/skill charge");
        setArgumentRange(0, 0);
        setIdentifiers("skill charge");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.MOVEMENT_INCREASING, SkillType.AGGRESSIVE);
        Bukkit.getServer().getPluginManager().registerEvents(new ChargeEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection section = super.getDefaultConfig();
        section.set("stun-duration", 5000);
        section.set("slow-duration", 0);
        section.set("root-duration", 0);
        section.set("silence-duration", 0);
        section.set(SkillSetting.DAMAGE.node(), 0);
        section.set(SkillSetting.DAMAGE_INCREASE.node(), 0);
        section.set(SkillSetting.RADIUS.node(), 2);
        return section;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        final Player player = hero.getPlayer();

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        double xDir = (targetLoc.getX() - playerLoc.getX());
        double zDir = (targetLoc.getZ() - playerLoc.getZ());
        Vector v = new Vector(xDir / 3, .5, zDir / 3);
        player.setVelocity(v);

        chargingPlayers.add(hero.getName());
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> player.setFallDistance(8f), 2);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public class ChargeEntityListener implements Listener {
        private final Skill skill;

        public ChargeEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onEntityDamage(EntityDamageEvent event) {
            if (!event.getCause().equals(DamageCause.FALL) || !(event.getEntity() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);

            if (!chargingPlayers.contains(hero.getName())) {
                return;
            }

            chargingPlayers.remove(hero.getName());
            Heroes.log(Level.INFO, "Player landed!");
            event.setDamage(0);
            event.setCancelled(true);

            int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS.node(), 2, false);
            long stunDuration = SkillConfigManager.getUseSetting(hero, skill, "stun-duration", 5000, false);
            long slowDuration = SkillConfigManager.getUseSetting(hero, skill, "slow-duration", 0, false);
            long rootDuration = SkillConfigManager.getUseSetting(hero, skill, "root-duration", 0, false);
            int rootPeriod = SkillConfigManager.getUseSetting(hero, skill, "root-period", 100, false);
            
            long silenceDuration = SkillConfigManager.getUseSetting(hero, skill, "silence-duration", 0, false);
            double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE.node(), 0, false);
            damage += (SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE, 0, false) * hero.getHeroLevel(skill));
            for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
                if (!(e instanceof LivingEntity)) {
                    continue;
                }
                LivingEntity le = (LivingEntity) e;

                if (!damageCheck(player, le)) {
                    continue;
                }

                if (e instanceof Player) {
                    Player p = (Player) e;
                    Hero tHero = plugin.getCharacterManager().getHero(p);
                    if (stunDuration > 0)
                        tHero.addEffect(new StunEffect(skill, hero.getPlayer(), stunDuration));
                    if (slowDuration > 0)
                        tHero.addEffect(new SlowEffect(skill, hero.getPlayer(), slowDuration, 2, p.getName() + " has been slowed by " + player.getName(),
                                p.getName() + " is no longer slowed by " + player.getName()));
                    if (rootDuration > 0)
                        tHero.addEffect(new RootEffect(skill, hero.getPlayer(), rootPeriod, rootDuration));
                    if (silenceDuration > 0)
                        tHero.addEffect(new SilenceEffect(skill, hero.getPlayer(), silenceDuration));
                    if (damage > 0) {
                        addSpellTarget(le, hero);
                        damageEntity(le, player, damage, DamageCause.ENTITY_ATTACK);
                    }
                } else if (e instanceof LivingEntity) {
                    Monster monster = plugin.getCharacterManager().getMonster((LivingEntity) e);
                    if (slowDuration > 0)
                        monster.addEffect(new SlowEffect(skill, hero.getPlayer(), slowDuration, 2, CustomNameManager.getName(le) + " has been slowed by " + player.getName(),
                                CustomNameManager.getName(le) + " is no longer slowed by " + player.getName()));
                    if (rootDuration > 0)
                       monster.addEffect(new RootEffect(skill, hero.getPlayer(), rootPeriod, rootDuration));
                }

                if (damage > 0) {
                    damageEntity(le, player, damage, DamageCause.ENTITY_ATTACK);
                }
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
