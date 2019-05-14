package com.herocraftonline.heroes.characters.skill.reborn.pathfinder;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
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

    private String stunReadyText;
    private String stunExpireText;

    private Map<Arrow, Long> stunArrows = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    public SkillRetreat(Heroes plugin) {
        super(plugin, "Retreat");
        setDescription("You retreat away from the direction you are facing. The next arrow fired within $1 seconds will stun your enemy for $2 seconds");
        setUsage("/skill retreat");
        setIdentifiers("skill retreat");
        setArgumentRange(0, 0);
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.AGGRESSIVE, SkillType.STUNNING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        int stunDuration = SkillConfigManager.getUseSetting(hero, this, "stun-duration", 1500, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000.0))
                .replace("$2", Util.decFormat.format(stunDuration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 2500);
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
                .replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        Location playerLoc = player.getLocation();
        Material belowMat = playerLoc.getBlock().getRelative(BlockFace.DOWN).getType();

        performBackflip(hero, player, belowMat);

        long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        hero.addEffect(new RetreatBuff(this, player, duration));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERDRAGON_FLAP, 4.0F, 1.0F);

        return SkillResult.NORMAL;
    }

    private static final Set<Material> requiredMaterials;

    static {
        requiredMaterials = new HashSet<>();
        requiredMaterials.add(Material.WATER);
        requiredMaterials.add(Material.LAVA);
        requiredMaterials.add(Material.AIR);
        requiredMaterials.add(Material.LEAVES);
        requiredMaterials.add(Material.SOUL_SAND);
    }

    private void performBackflip(Hero hero, Player player, Material belowMat) {
        // Calculate backflip values
        float pitch = player.getEyeLocation().getPitch();
        if (pitch > 0) {
            pitch = -pitch;
        }
        float multiplier = (90f + pitch) / 50f;

        boolean weakenVelocity = false;
        switch (belowMat) {
            case STATIONARY_LAVA:
            case STATIONARY_WATER:
            case WATER:
            case LAVA:
            case SOUL_SAND:
                weakenVelocity = true;
                break;
            default:
                break;
        }


        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.5, false);

        if (vPower > 2.0)
            vPower = 2.0;

        if (weakenVelocity)
            vPower *= 0.75;

        final Vector velocity = player.getVelocity().setY(vPower);

        Vector directionVector = player.getLocation().getDirection();
        directionVector.setY(0);
        directionVector.normalize();
        directionVector.multiply(multiplier);

        velocity.add(directionVector);
        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 0.5, false);

        if (weakenVelocity)
            hPower *= 0.75;

        velocity.multiply(new Vector(-hPower, 1, -hPower));

        // Let's bypass the nocheat issues...
        NCPUtils.applyExemptions(player, new NCPFunction() {

            @Override
            public void execute() {
                // Backflip!
                player.setVelocity(velocity);
                player.setFallDistance(-8f);
            }
        }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1000, false));
    }

    public class RetreatBuff extends ExpirableEffect {

        RetreatBuff(Skill skill, Player applier, long duration) {
            super(skill, "RetreatBuff", applier, duration);

            types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

        }
    }

    public class SkillDamageListener implements Listener {

        private final Skill skill;

        public SkillDamageListener(Skill skill) {
            this.skill = skill;
        }


        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityShootBow(EntityShootBowEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)) {
                return;
            }

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("RetreatBuff")) {
                stunArrows.put((Arrow) event.getProjectile(), System.currentTimeMillis());
                hero.removeEffect(hero.getEffect("RetreatBuff"));
            }
        }


        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity))
                return;

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            if (!(subEvent.getDamager() instanceof Arrow))
                return;

            Arrow arrow = (Arrow) subEvent.getDamager();
            if (!(arrow.getShooter() instanceof Player) || !stunArrows.containsKey(arrow))
                return;

            stunArrows.remove(arrow);

            Player player = (Player) arrow.getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);

            // Stun the target
            long duration = SkillConfigManager.getUseSetting(hero, skill, "stun-duration", 1500, false);
            StunEffect retreatStunEffect = new StunEffect(skill, player, duration, stunReadyText, stunExpireText);
            LivingEntity target = (LivingEntity) event.getEntity();
            plugin.getCharacterManager().getCharacter(target).addEffect(retreatStunEffect);
            playParticleEffect(target);
        }

        private void playParticleEffect(LivingEntity target) {

            Location location = target.getEyeLocation().clone();
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

