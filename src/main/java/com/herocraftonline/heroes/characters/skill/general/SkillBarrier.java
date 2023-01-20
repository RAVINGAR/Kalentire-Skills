package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.DisarmEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class SkillBarrier extends ActiveSkill implements Listenable {

    private final Listener listener;
    private String applyText;
    private String expireText;

    public SkillBarrier(final Heroes plugin) {
        super(plugin, "Barrier");
        setDescription("Create a protective barrier around yourself for $1 second(s). " +
                "The barrier allows you to retaliate against all incoming melee attacks, disarming them for $2 seconds," +
                " and dealing $3% of your weapon damage to them.");
        setUsage("/skill barrier");
        setIdentifiers("skill barrier");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.DAMAGING, SkillType.BUFFING, SkillType.AGGRESSIVE);

        listener = new SkillEntityListener(this);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        final int disarmDuration = SkillConfigManager.getUseSetting(hero, this, "disarm-duration", 3000, false);
        final double damageMultiplier = SkillConfigManager.getScaledUseSettingDouble(hero, this, "damage-multiplier", false);

        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000.0))
                .replace("$2", Util.decFormat.format(disarmDuration / 1000.0))
                .replace("$3", Util.decFormat.format(damageMultiplier * 100));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set("damage-multiplier", 0.4);
        config.set("damage-multiplier-increase-per-intellect", 0.00875);
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set(SkillSetting.DURATION_INCREASE_PER_INTELLECT.node(), 75);
        config.set("slow-amplifier", 35);
        config.set("disarm-duration", 3000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has created a Barrier!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s Barrier has faded.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has created a Barrier!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s Barrier has faded.").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        final int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        final int disarmDuration = SkillConfigManager.getUseSetting(hero, this, "disarm-duration", 3000, false);
        final int slowAmplifier = SkillConfigManager.getUseSetting(hero, this, "slow-amplifier", 3, false);

        hero.addEffect(new BarrierEffect(this, player, duration, slowAmplifier, disarmDuration));

        final List<Location> circle = GeometryUtil.circle(player.getLocation(), 36, 1.5);
        for (final Location location : circle) {
            //player.getWorld().spigot().playEffect(circle(player.getLocation(), 36, 1.5).get(i), org.bukkit.Effect.TILE_BREAK, Material.STONE.getId(), 0, 0.2F, 1.5F, 0.2F, 0, 4, 16);
            player.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 4, 0.2, 1.5, 0.2, 0, Bukkit.createBlockData(Material.STONE));
        }

        player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.SMOKE, 3);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7F, 2.0F);

        return SkillResult.NORMAL;
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    public class SkillEntityListener implements Listener {

        private final Skill skill;

        SkillEntityListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(final WeaponDamageEvent event) {
            if (event.getDamage() == 0) {
                return;
            }

            // Handle outgoing
            if (event.getDamager() instanceof Hero && event.getEntity() instanceof Player && !(event.getAttackerEntity() instanceof Projectile)) {

                final Player defenderPlayer = (Player) event.getEntity();
                final Hero defenderHero = plugin.getCharacterManager().getHero(defenderPlayer);
                final Player damagerPlayer = ((Hero) event.getDamager()).getPlayer();
                final Hero damagerHero = plugin.getCharacterManager().getHero(damagerPlayer);

                if ((defenderPlayer.getNoDamageTicks() > 10) || defenderPlayer.isDead() || defenderPlayer.getHealth() <= 0) {
                    return;
                }

                // Check if they are under the effects of Barrier
                if (defenderHero.hasEffect("Barrier")) {
                    final BarrierEffect bgEffect = (BarrierEffect) defenderHero.getEffect("Barrier");

                    if (!(damageCheck(defenderPlayer, (LivingEntity) damagerPlayer) && damageCheck(damagerPlayer, (LivingEntity) defenderPlayer))) {
                        return;
                    }

                    for (final Effect effect : defenderHero.getEffects()) {
                        if (effect.isType(EffectType.STUN) || effect.isType(EffectType.DISABLE)) {
                            defenderHero.removeEffect(bgEffect);
                            return;
                        }
                    }

                    if (damagerHero.hasEffect("Barrier")) {
                        return;
                    }

                    // Cancel the attack
                    event.setCancelled(true);

                    // Make them have invuln ticks so attackers dont get machine-gunned from attacking the buffed player.
                    defenderPlayer.setNoDamageTicks(defenderPlayer.getMaximumNoDamageTicks());

                    final double damageMultiplier = SkillConfigManager.getScaledUseSettingDouble(defenderHero, skill, "damage-multiplier", false);

                    final Material item = defenderPlayer.getInventory().getItemInMainHand().getType();
                    final Double itemDamage = plugin.getDamageManager().getFlatItemDamage(defenderHero, item);


                    final double damage = itemDamage * damageMultiplier; // FIXME received null here, is this fix now using default damage?
                    addSpellTarget(damagerPlayer, defenderHero);
                    damageEntity(damagerPlayer, defenderPlayer, damage, DamageCause.ENTITY_ATTACK);

                    damagerPlayer.getWorld().playSound(damagerPlayer.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8F, 1.0F);

                    // Disarm checks
                    final Material heldItem = damagerPlayer.getInventory().getItemInMainHand().getType();
                    if (!Util.isWeapon(heldItem) && !Util.isAwkwardWeapon(heldItem)) {
                        return;
                    }
                    if (damagerHero.hasEffectType(EffectType.DISARM)) {
                        return;
                    }

                    // Disarm attacker
                    final long disarmDuration = bgEffect.getDisarmDuration();
                    damagerHero.addEffect(new DisarmEffect(skill, defenderPlayer, disarmDuration));

                    //damagerPlayer.getWorld().playSound(damagerPlayer.getLocation(), Sound.HURT, 0.8F, 0.5F);
                }
            }
        }
    }

    public class BarrierEffect extends ExpirableEffect {
        private long disarmDuration;

        public BarrierEffect(final Skill skill, final Player applier, final long duration, final int slowAmplifier, final long disarmDuration) {
            super(skill, "Barrier", applier, duration, applyText, expireText);

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);

            this.disarmDuration = disarmDuration;

            final int tickDuration = (int) ((duration / 1000) * 20);
            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, tickDuration, slowAmplifier), false);
            //addMobEffect(8, tickDuration, 254, false);
        }

        public long getDisarmDuration() {
            return disarmDuration;
        }

        public void setDisarmDuration(final long disarmDuration) {
            this.disarmDuration = disarmDuration;
        }
    }
}
