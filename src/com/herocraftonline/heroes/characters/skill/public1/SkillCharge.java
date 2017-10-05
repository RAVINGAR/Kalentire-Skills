package com.herocraftonline.heroes.characters.skill.public1;

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

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class SkillCharge extends TargettedSkill {

    private final Set<String> chargingPlayers = new HashSet<String>();

    public SkillCharge(Heroes plugin) {
        super(plugin, "Charge");
        this.setDescription("You charge toward your target!");
        this.setUsage("/skill charge");
        this.setArgumentRange(0, 1);
        this.setIdentifiers("skill charge");
        this.setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.MOVEMENT_INCREASING, SkillType.AGGRESSIVE);
        Bukkit.getServer().getPluginManager().registerEvents(new ChargeEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection section = super.getDefaultConfig();
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

        final Location playerLoc = player.getLocation();
        final Location targetLoc = target.getLocation();

        final double xDir = (targetLoc.getX() - playerLoc.getX());
        final double zDir = (targetLoc.getZ() - playerLoc.getZ());
        final Vector v = new Vector(xDir / 3, .5, zDir / 3);
        player.setVelocity(v);

        this.chargingPlayers.add(hero.getName());
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                player.setFallDistance(8f);
            }
        }, 2);
        this.broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public class ChargeEntityListener implements Listener {

        private final Skill skill;

        public ChargeEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onEntityDamage(EntityDamageEvent event) {
            if (!event.getCause().equals(DamageCause.FALL) || !(event.getEntity() instanceof Player) || !SkillCharge.this.chargingPlayers.contains(event.getEntity())) {
                return;
            }

            final Player player = (Player) event.getEntity();
            final Hero hero = SkillCharge.this.plugin.getCharacterManager().getHero(player);
            SkillCharge.this.chargingPlayers.remove(hero.getName());
            Heroes.log(Level.INFO, "Player landed!");
            event.setDamage(0);
            event.setCancelled(true);

            final int radius = SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.RADIUS.node(), 2, false);
            final long stunDuration = SkillConfigManager.getUseSetting(hero, this.skill, "stun-duration", 5000, false);
            final long slowDuration = SkillConfigManager.getUseSetting(hero, this.skill, "slow-duration", 0, false);
            final long rootDuration = SkillConfigManager.getUseSetting(hero, this.skill, "root-duration", 0, false);
            final int rootPeriod = SkillConfigManager.getUseSetting(hero, this.skill, "root-period", 100, false);
            final long silenceDuration = SkillConfigManager.getUseSetting(hero, this.skill, "silence-duration", 0, false);
            double damage = SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.DAMAGE.node(), 0D, false);
            damage += (SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.DAMAGE_INCREASE, 0, false) * hero.getHeroLevel(this.skill));
            for (final Entity e : player.getNearbyEntities(radius, radius, radius)) {
                if (!(e instanceof LivingEntity)) {
                    continue;
                }
                final LivingEntity le = (LivingEntity) e;

                if (!damageCheck(player, le)) {
                    continue;
                }

                if (e instanceof Player) {
                    final Player p = (Player) e;
                    final Hero tHero = SkillCharge.this.plugin.getCharacterManager().getHero(p);
                    if (stunDuration > 0) {
                        tHero.addEffect(new StunEffect(this.skill, hero.getPlayer(), stunDuration));
                    }
                    if (slowDuration > 0) {
                        tHero.addEffect(new SlowEffect(this.skill, hero.getPlayer(), slowDuration, 2, p.getDisplayName() + " has been slowed by " + player.getDisplayName(), p.getDisplayName() + " is no longer slowed by " + player.getDisplayName()));
                    }
                    if (rootDuration > 0) {
                        tHero.addEffect(new RootEffect(this.skill, hero.getPlayer(), rootPeriod, rootDuration));
                    }
                    if (silenceDuration > 0) {
                        tHero.addEffect(new SilenceEffect(this.skill, hero.getPlayer(), silenceDuration));
                    }
                    if (damage > 0) {
                        SkillCharge.this.addSpellTarget(le, hero);
                        damageEntity(le, player, damage, DamageCause.ENTITY_ATTACK);
                    }
                } else if (e instanceof LivingEntity) {
                    final Monster monster = SkillCharge.this.plugin.getCharacterManager().getMonster((LivingEntity) e);
                    if (slowDuration > 0) {
                        monster.addEffect(new SlowEffect(this.skill, hero.getPlayer(), slowDuration, 2, CustomNameManager.getName(le) + " has been slowed by " + player.getDisplayName(), CustomNameManager.getName(le) + " is no longer slowed by " + player.getDisplayName()));
                    }
                    if (rootDuration > 0) {
                        monster.addEffect(new RootEffect(this.skill, hero.getPlayer(), rootPeriod, rootDuration));
                    }
                }

                if (damage > 0) {
                    damageEntity(le, player, damage, DamageCause.ENTITY_ATTACK);
                }
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return this.getDescription();
    }
}
