package com.herocraftonline.heroes.characters.skill.reborn;

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
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
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
        setDescription("Retreat from your enemies, your next arrow will stun your enemy. ");
        setUsage("/skill retreat");
        setArgumentRange(0, 0);
        setIdentifiers("skill retreat");
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.AGGRESSIVE, SkillType.STUNNING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        return node;
    }

    @Override
    public void init() {
        super.init();
        setUseText("%hero% retreats and his next arrow will stun his target".replace("%hero%", "$1"));
        stunReadyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%target% is stunned!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();
        if (hero.hasEffect("RetreatBuff")) {
            hero.removeEffect(hero.getEffect("RetreatBuff"));
            return SkillResult.SKIP_POST_USAGE;
        }

        Location playerLoc = player.getLocation();
        Material belowMat = playerLoc.getBlock().getRelative(BlockFace.DOWN).getType();

        performBackflip(hero, player, belowMat);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
        hero.addEffect(new RetreatBuff(this, player, duration));

        broadcastExecuteText(hero);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 4.0F, 1.0F);

        return SkillResult.NORMAL;
    }

    private static final Set<Material> requiredMaterials;

    static {
        requiredMaterials = new HashSet<>();
        requiredMaterials.add(Material.LEGACY_STATIONARY_WATER);
        requiredMaterials.add(Material.LEGACY_STATIONARY_LAVA);
        requiredMaterials.add(Material.WATER);
        requiredMaterials.add(Material.LAVA);
        requiredMaterials.add(Material.AIR);
        requiredMaterials.add(Material.LEGACY_LEAVES);  //TODO: Add every other leaf type maybe?
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

        int dexterity = hero.getAttributeValue(AttributeType.DEXTERITY);

        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.5, false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-dexterity", 0.0125, false);
        vPower += dexterity * vPowerIncrease;

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
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-dexterity", 0.0125, false);
        hPower += dexterity * hPowerIncrease;

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

        public RetreatBuff(Skill skill, Player applier, long duration) {
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
            long duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 5000, false);
            StunEffect retreatStunEffect = new StunEffect(skill, player, duration, stunReadyText, stunExpireText);
            LivingEntity target = (LivingEntity) event.getEntity();
            plugin.getCharacterManager().getCharacter(target).addEffect(retreatStunEffect);
            particleEffect(target);
        }

        public void particleEffect(LivingEntity target) {

            EffectManager em = new EffectManager(plugin);
            Effect visualEffect = new Effect(em) {
                Particle particle = Particle.FIREWORKS_SPARK;



                int radius = 2;

                @Override
                public void onRun() {
                    for (double z = -radius; z <= radius; z += 0.33) {
                        for (double x = -radius; x <= radius; x += 0.33) {
                            if (x * x + z * z <= radius * radius) {
                                display(particle, getLocation().clone().add(x, 0, z));
                            }
                        }
                    }
                }
            };

            visualEffect.type = de.slikey.effectlib.EffectType.REPEATING;
            visualEffect.period = 10;
            visualEffect.iterations = 8;

            Location location = target.getLocation().clone();
            visualEffect.asynchronous = true;
            visualEffect.setLocation(location);

            visualEffect.start();
            em.disposeOnTermination();

            target.getWorld().playSound(location, Sound.ENTITY_GENERIC_BURN, 0.15f, 0.0001f);
        }

    }
}

