package com.herocraftonline.heroes.characters.skill.reborn;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SkillRetreat extends ActiveSkill {

    private static final Set<Material> requiredMaterials;

    static {
        requiredMaterials = new HashSet<>();
        requiredMaterials.add(Material.WATER);
        requiredMaterials.add(Material.LAVA);
        requiredMaterials.add(Material.AIR);
        requiredMaterials.add(Material.LEGACY_LEAVES);  //TODO: Add every other leaf type maybe?
        requiredMaterials.add(Material.SOUL_SAND);
    }

    private final Map<Arrow, Long> stunArrows = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        @Override
        protected boolean removeEldestEntry(final Map.Entry<Arrow, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };
    private String stunReadyText;
    private String stunExpireText;

    public SkillRetreat(final Heroes plugin) {
        super(plugin, "Retreat");
        setDescription("Retreat from your enemies, your next arrow will stun your enemy. ");
        setUsage("/skill retreat");
        setIdentifiers("skill retreat");
        setArgumentRange(0, 0);
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.AGGRESSIVE, SkillType.STUNNING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 6000);
        config.set("stun-duration", 1500);
        config.set("horizontal-power", 0.5);
        config.set("vertical-power", 0.5);
        config.set("ncp-exemption-duration", 0);
        return config;
    }

    @Override
    public void init() {
        super.init();

        stunReadyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(),
                        ChatComponents.GENERIC_SKILL + "%target% is stunned!")
                .replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        final Location playerLoc = player.getLocation();
        final Material belowMat = playerLoc.getBlock().getRelative(BlockFace.DOWN).getType();

        performBackflip(hero, player, belowMat);

        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 1500, false);
        hero.addEffect(new RetreatBuff(this, player, duration));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 4.0F, 1.0F);

        return SkillResult.NORMAL;
    }

    private void performBackflip(final Hero hero, final Player player, final Material belowMat) {
        // Calculate backflip values
        float pitch = player.getEyeLocation().getPitch();
        if (pitch > 0) {
            pitch = -pitch;
        }
        final float multiplier = (90f + pitch) / 50f;

        boolean weakenVelocity = false;
        switch (belowMat) {
            case LEGACY_WATER:
            case LEGACY_STATIONARY_LAVA:
            case WATER:
            case LAVA:
            case SOUL_SAND:
                weakenVelocity = true;
                break;
            default:
                break;
        }

        final int dexterity = hero.getAttributeValue(AttributeType.DEXTERITY);

        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.5, false);
        final double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-dexterity", 0.0125, false);
        vPower += dexterity * vPowerIncrease;

        if (vPower > 2.0) {
            vPower = 2.0;
        }

        if (weakenVelocity) {
            vPower *= 0.75;
        }

        final Vector velocity = player.getVelocity().setY(vPower);

        final Vector directionVector = player.getLocation().getDirection();
        directionVector.setY(0);
        directionVector.normalize();
        directionVector.multiply(multiplier);

        velocity.add(directionVector);
        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 0.5, false);
        final double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-dexterity", 0.0125, false);
        hPower += dexterity * hPowerIncrease;

        if (weakenVelocity) {
            hPower *= 0.75;
        }

        velocity.multiply(new Vector(-hPower, 1, -hPower));

        // Let's bypass the nocheat issues...
        NCPUtils.applyExemptions(player, () -> {
            // Backflip!
            player.setVelocity(velocity);
            player.setFallDistance(-8f);
        }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1000, false));
    }

    public static class RetreatBuff extends ExpirableEffect {

        RetreatBuff(final Skill skill, final Player applier, final long duration) {
            super(skill, "RetreatBuff", applier, duration);

            types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);

        }
    }

    public class SkillDamageListener implements Listener {

        private final Skill skill;

        public SkillDamageListener(final Skill skill) {
            this.skill = skill;
        }


        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityShootBow(final EntityShootBowEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)) {
                return;
            }

            final Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("RetreatBuff")) {
                stunArrows.put((Arrow) event.getProjectile(), System.currentTimeMillis());
                hero.removeEffect(hero.getEffect("RetreatBuff"));
            }
        }


        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(final EntityDamageEvent event) {
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            final EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            if (!(subEvent.getDamager() instanceof Arrow)) {
                return;
            }

            final Arrow arrow = (Arrow) subEvent.getDamager();
            if (!(arrow.getShooter() instanceof Player) || !stunArrows.containsKey(arrow)) {
                return;
            }

            stunArrows.remove(arrow);

            final Player player = (Player) arrow.getShooter();
            final Hero hero = plugin.getCharacterManager().getHero(player);

            // Stun the target
            final long duration = SkillConfigManager.getUseSetting(hero, skill, "stun-duration", 1500, false);
            final StunEffect retreatStunEffect = new StunEffect(skill, player, duration, stunReadyText, stunExpireText);
            final LivingEntity target = (LivingEntity) event.getEntity();
            plugin.getCharacterManager().getCharacter(target).addEffect(retreatStunEffect);
            playParticleEffect(target);
        }

        private void playParticleEffect(final LivingEntity target) {

            final Location location = target.getEyeLocation().clone();
            VisualEffect.playInstantFirework(FireworkEffect.builder()
                    .flicker(true)
                    .trail(false)
                    .with(FireworkEffect.Type.STAR)
                    .withColor(Color.WHITE)
                    .withFade(Color.YELLOW)
                    .build(), location.add(0, 2.0, 0));

            target.getWorld().playSound(location, Sound.ENTITY_GENERIC_BURN, 0.15f, 0.0001f);
        }

    }
}

