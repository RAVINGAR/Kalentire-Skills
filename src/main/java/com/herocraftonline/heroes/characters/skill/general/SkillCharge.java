package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
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
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SkillCharge extends TargettedSkill implements Listenable {

    private final Set<UUID> chargingPlayers = new HashSet<>();

    private final Listener listener;

    public SkillCharge(final Heroes plugin) {
        super(plugin, "Charge");
        this.setDescription("You charge toward your target!");
        this.setUsage("/skill charge");
        this.setArgumentRange(0, 1);
        this.setIdentifiers("skill charge");
        this.setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.MOVEMENT_INCREASING, SkillType.AGGRESSIVE);
        this.listener = new ChargeEntityListener();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection section = super.getDefaultConfig();
        section.set("stun-duration", 5000);
        section.set("slow-duration", 0);
        section.set("root-duration", 0);
        section.set("silence-duration", 0);
        section.set(SkillSetting.DAMAGE.node(), 0);
        section.set(SkillSetting.RADIUS.node(), 4);
        return section;
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final Player player = hero.getPlayer();

        final Location playerLoc = player.getLocation();
        final Location targetLoc = target.getLocation();

        final double xDir = (targetLoc.getX() - playerLoc.getX());
        final double zDir = (targetLoc.getZ() - playerLoc.getZ());
        final Vector v = new Vector(xDir / 3, .5, zDir / 3);
        player.setVelocity(v);

        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 2, false);
        final long stunDuration = SkillConfigManager.getUseSetting(hero, this, "stun-duration", 5000, false);
        final long slowDuration = SkillConfigManager.getUseSetting(hero, this, "slow-duration", 0, false);
        final long rootDuration = SkillConfigManager.getUseSetting(hero, this, "root-duration", 0, false);
        final int rootPeriod = SkillConfigManager.getUseSetting(hero, this, "root-period", 100, false);
        final long silenceDuration = SkillConfigManager.getUseSetting(hero, this, "silence-duration", 0, false);
        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE.node(), 4D, false);

        this.chargingPlayers.add(player.getUniqueId());
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
            for (final Entity e : target.getNearbyEntities(radius, radius, radius)) {
                if (!(e instanceof LivingEntity)) {
                    continue;
                }
                final LivingEntity le = (LivingEntity) e;

                if (!damageCheck(player, le)) {
                    continue;
                }

                if (e instanceof Player) {
                    final Player p = (Player) e;
                    final Hero tHero = this.plugin.getCharacterManager().getHero(p);
                    if (stunDuration > 0) {
                        tHero.addEffect(new StunEffect(this, hero.getPlayer(), stunDuration));
                    }
                    if (slowDuration > 0) {
                        tHero.addEffect(new SlowEffect(this, hero.getPlayer(), slowDuration, 2, p.getDisplayName() + " has been slowed by " + player.getDisplayName(), p.getDisplayName() + " is no longer slowed by " + player.getDisplayName()));
                    }
                    if (rootDuration > 0) {
                        tHero.addEffect(new RootEffect(this, hero.getPlayer(), rootPeriod, rootDuration));
                    }
                    if (silenceDuration > 0) {
                        tHero.addEffect(new SilenceEffect(this, hero.getPlayer(), silenceDuration));
                    }
                } else {
                    final Monster monster = plugin.getCharacterManager().getMonster((LivingEntity) e);
                    if (slowDuration > 0) {
                        monster.addEffect(new SlowEffect(this, hero.getPlayer(), slowDuration, 2, CustomNameManager.getName(le) + " has been slowed by " + player.getDisplayName(), CustomNameManager.getName(le) + " is no longer slowed by " + player.getDisplayName()));
                    }
                    if (rootDuration > 0) {
                        monster.addEffect(new RootEffect(this, hero.getPlayer(), rootPeriod, rootDuration));
                    }
                }
                if (damage > 0) {
                    addSpellTarget(le, hero);
                    damageEntity(le, player, damage, DamageCause.ENTITY_ATTACK, 0.75f);
                }
            }
            chargingPlayers.remove(player.getUniqueId());
        }, 5L);
        this.broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @NotNull
    @Override
    public Listener getListener() {
        return listener;
    }

    @Override
    public String getDescription(final Hero hero) {
        return this.getDescription();
    }

    public class ChargeEntityListener implements Listener {

        public ChargeEntityListener() {
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onEntityDamage(final EntityDamageEvent event) {
            if (!event.getCause().equals(DamageCause.FALL) || !(event.getEntity() instanceof Player)) {
                return;
            }
            if (SkillCharge.this.chargingPlayers.remove(event.getEntity().getUniqueId())) {
                event.setDamage(0);
                event.setCancelled(true);
            }
        }
    }
}
