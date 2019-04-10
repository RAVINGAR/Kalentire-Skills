package com.herocraftonline.heroes.characters.skill.reborn.pathfinder;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.Map;

public class SkillIceVolley extends ActiveSkill {

    private Map<Arrow, Long> iceVolleyShots = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 1L;

        protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
            return (size() > 60) || (eldest.getValue() + 5000L <= System.currentTimeMillis());
        }
    };

    private String applyText;
    private String expireText;
    private String shotText;
    private String slowApplyText;
    private String slowExpireText;

    public SkillIceVolley(Heroes plugin) {
        super(plugin, "IceVolley");
        setDescription("Prepare an array of ice infused arrows. After use, your next next shot fired within $1 seconds will fire up to $2 arrows at once! Any target hit these arrows will be slowed for $3 second(s).");
        setUsage("/skill icevolley");
        setArgumentRange(0, 0);
        setIdentifiers("skill icevolley");
        setTypes(SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.AGGRESSIVE, SkillType.DEBUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
        int slowDuration = SkillConfigManager.getUseSetting(hero, this, "slow-duration", 2500, false);
        int arrowsPerShot = SkillConfigManager.getUseSetting(hero, this, "max-arrows-per-shot", 5, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedSlowDuration = Util.decFormat.format(slowDuration / 1000.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", arrowsPerShot + "").replace("$3", formattedSlowDuration);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 4000);
        node.set("max-arrows-per-shot", 5);
        node.set("degrees", 10.0);
        node.set("velocity-multiplier", 2.0);
        node.set("slow-multiplier", 1);
        node.set("slow-duration", 2500);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has loaded an array of Ice Arrows!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer firing a volley of arrows.");
        node.set("slow-apply-text", ChatComponents.GENERIC_SKILL + "%target% has been slowed by %hero%'s Ice Volley!");
        node.set("slow-expire-text", ChatComponents.GENERIC_SKILL + "%target% is no longer slowed!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has loaded an " + ChatColor.WHITE + ChatColor.BOLD + "Ice Volley" + ChatColor.RESET + "!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer firing a volley of arrows.").replace("%hero%", "$1");
        shotText = SkillConfigManager.getRaw(this, "shot-text", ChatComponents.GENERIC_SKILL + "%hero% has unleashed an " + ChatColor.WHITE + ChatColor.BOLD + "Ice Volley" + ChatColor.RESET + "!");

        slowApplyText = SkillConfigManager.getRaw(this, "slow-apply-text", ChatComponents.GENERIC_SKILL + "%target% has been slowed by %hero%'s Ice Volley!").replace("%target%", "$1").replace("%hero%", "$2");
        slowExpireText = SkillConfigManager.getRaw(this, "slow-expire-text", ChatComponents.GENERIC_SKILL + "%target% is no longer slowed!").replace("%target%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int maxArrowsPerShot = SkillConfigManager.getUseSetting(hero, this, "max-arrows-per-shot", 5, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);

        hero.addEffect(new IceVolleyShotEffect(this, player, duration, maxArrowsPerShot));

        broadcastExecuteText(hero);

        //target.getWorld().playSound(target.getLocation(), Sound.d, 0.7F, 2.0F);

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onEntityShootBow(EntityShootBowEvent event) {
            if ((!(event.getEntity() instanceof Player)) || (!(event.getProjectile() instanceof Arrow))) {
                return;
            }

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (!hero.hasEffect("IceVolleyShot"))
                return;

            // Get the effect from the player, and fire "up to" the set max arrows per shot.
            IceVolleyShotEffect msEffect = (IceVolleyShotEffect) hero.getEffect("IceVolleyShot");

            final Player player = hero.getPlayer();

            int maxArrowsToShoot = msEffect.getMaxArrowsPerShot();

            // Ensure the player still has enough arrows in his inventory.
            PlayerInventory inventory = player.getInventory();

            Map<Integer, ? extends ItemStack> arrowSlots = inventory.all(Material.ARROW);

            int numHeldArrows = 0;
            for (Map.Entry<Integer, ? extends ItemStack> entry : arrowSlots.entrySet()) {
                numHeldArrows += entry.getValue().getAmount();
            }

            if (numHeldArrows < 2)
                return;

            // We're good. Remove the original event projectile and cancel the event.
            event.setCancelled(true);
            Arrow arrow = (Arrow) event.getProjectile();
            arrow.remove();

            // Convert yaw to radians
            double yaw = player.getLocation().getYaw();
            yaw = yaw * (Math.PI / 180);

            // Convert Pitch to radians
            double pitch = player.getEyeLocation().getPitch();
            pitch *= -1;    // Invert pitch
            pitch = pitch * (Math.PI / 180);

            // Alter variables based on event force.
            float force = event.getForce();
            int arrowsToShoot = (int) (force * maxArrowsToShoot);     // Reduce number of arrows based on how far back they pulled their bow.

            // Never shoot less than 2 arrows.
            if (arrowsToShoot < 2)
                arrowsToShoot = 2;

            // If we don't have enough arrows, return.
            if (numHeldArrows < maxArrowsToShoot)
                return;

            // We -should- have enough arrows here. Proceed.

            // Remove the actual projectile from the event
            Arrow actualArrow = (Arrow) event.getProjectile();
            actualArrow.remove();

            double velocityMultiplier = SkillConfigManager.getUseSetting(hero, skill, "velocity-multiplier", 1.6, false);
            final double adjustedVelocityMultiplier = force * velocityMultiplier;    // Reduce the velocity based on how far back they pulled their bow.

            // Create arrow spread
            double degrees = SkillConfigManager.getUseSetting(hero, skill, "degrees", 65.0, false);
            final double degreesRad = degrees * (Math.PI / 180);      // Convert degrees to radians
            final double diff = degreesRad / (arrowsToShoot - 1);           // Create our difference for the spread

            // Center the projectile direction based on yaw, and then convert it to radians.
            double degreeOffset = (90.0 - (degrees / 2.0));
            double degreeOffsetRad = degreeOffset * (Math.PI / 180);
            yaw = yaw + degreeOffsetRad;

            double centerDegreesRad = degreesRad / 2.0;

            // Our center degrees rad isn't very accurate in regards to the arrow spread. Let's get the closest start point.
            double actualCenterDegreesRad = 0;
            for (double a = 0; a <= centerDegreesRad; a += diff) {
                actualCenterDegreesRad = a;
            }

            if (shotText != null && shotText.length() > 0) {
                if (hero.hasEffectType(EffectType.SILENT_ACTIONS))
                    player.sendMessage(shotText.replace("%hero%", player.getName()));
                else
                    broadcast(player.getLocation(), shotText.replace("%hero%", player.getName()));
            }

            // Create a multiplier that lowers velocity based on how high or low the player is looking
            final double pitchMultiplier = Math.abs(Math.sin(Math.abs(pitch) - 90));

            // Let's bypass the nocheat issues...
            final double centerRadians = actualCenterDegreesRad;
            final double centerYaw = yaw;
            NCPUtils.applyExemptions(player, new NCPFunction() {

                @Override
                public void execute() {
                    // Fire arrows from the center and move clockwise towards the end.
                    for (double a = centerRadians; a <= degreesRad; a += diff) {
                        shootIceVolleyArrow(player, centerYaw + a, pitchMultiplier, adjustedVelocityMultiplier);
                    }

                    // Fire arrows from the start and move clockwise towards the center
                    for (double a = 0; a < centerRadians; a += diff) {
                        shootIceVolleyArrow(player, centerYaw + a, pitchMultiplier, adjustedVelocityMultiplier);
                    }
                }
            }, Lists.newArrayList("BLOCKPLACE_SPEED"), 0);

            // Remove the arrows from the players inventory
            int removedArrows = 0;
            for (Map.Entry<Integer, ? extends ItemStack> entry : arrowSlots.entrySet()) {
                int amount = entry.getValue().getAmount();
                int remove = amount;
                if (removedArrows + remove > (arrowsToShoot - 1)) {     // Offset by 1 due to the arrow removed by the initial event.
                    remove = arrowsToShoot - removedArrows;
                }
                removedArrows += remove;
                if (remove == amount) {
                    inventory.clear(entry.getKey());
                } else {
                    inventory.getItem(entry.getKey()).setAmount(amount - remove);
                }

                if (removedArrows >= (arrowsToShoot - 1)) {          // Offset by 1 due to the arrow removed by the initial event.
                    break;
                }
            }
            player.updateInventory();

            hero.removeEffect(msEffect);
        }

        public void addParticleEffect(final Projectile p) {
            new BukkitRunnable() {
                public void run() {
                    if (iceVolleyShots.containsKey(p)) {
                        Location loc = p.getLocation();
                        //p.getWorld().spigot().playEffect(loc, org.bukkit.Effect.INSTANT_SPELL, 0, 0, 0.0F, 0.1F, 0.0F, 0.0F, 1, 16);
                        p.getWorld().spawnParticle(Particle.SPELL_INSTANT, loc, 1, 0, 0.1, 0, 0);
                    } else {
                        this.cancel();
                        return;
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        }

        private void shootIceVolleyArrow(Player player, double yaw, double pitchMultiplier, double velocityMultiplier) {

            Arrow arrow = player.launchProjectile(Arrow.class);

            // Create our velocity direction based on where the player is facing.
            Vector vel = new Vector(Math.cos(yaw), 0, Math.sin(yaw));
            vel.multiply(pitchMultiplier * velocityMultiplier);
            vel.setY(arrow.getVelocity().getY() * velocityMultiplier);

            arrow.setVelocity(vel);    // Apply multiplier so it goes farther.

            arrow.setShooter(player);
            iceVolleyShots.put(arrow, System.currentTimeMillis());
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if ((!(event instanceof EntityDamageByEntityEvent)) || (!(event.getEntity() instanceof LivingEntity)))
                return;

            Entity projectile = ((EntityDamageByEntityEvent) event).getDamager();
            if ((!(projectile instanceof Arrow)) || (!(((Projectile) projectile).getShooter() instanceof Player)))
                return;

            if (!(iceVolleyShots.containsKey(projectile)))
                return;

            final Arrow iceArrow = (Arrow) projectile;
            Player player = (Player) iceArrow.getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);

            long duration = SkillConfigManager.getUseSetting(hero, skill, "slow-duration", 2000, false);
            int amplifier = SkillConfigManager.getUseSetting(hero, skill, "slow-multiplier", 1, false);

            SlowEffect iceSlowEffect = new SlowEffect(skill, player, duration, amplifier, slowApplyText, slowExpireText); //TODO Implicit broadcast() call - may need changes?
            iceSlowEffect.types.add(EffectType.DISPELLABLE);
            iceSlowEffect.types.add(EffectType.ICE);

            LivingEntity target = (LivingEntity) event.getEntity();
            plugin.getCharacterManager().getCharacter(target).addEffect(iceSlowEffect);

            //target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5F, 0), org.bukkit.Effect.TILE_BREAK, Material.ICE.getId(), 0, 0.4F, 0.2F, 0.4F, 0.3F, 50, 16);
            target.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0, 0.5, 0), 50, 0.4, 0.2, 0.4, 0.3, Bukkit.createBlockData(Material.ICE));
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 7.0F, 0.7F);

            iceVolleyShots.remove(iceArrow);
        }
    }

    // Buff effect used to keep track of icevolley functionality
    public class IceVolleyShotEffect extends ExpirableEffect {

        private int maxArrowsPerShot;

        public IceVolleyShotEffect(Skill skill, Player applier, long duration, int maxArrowsPerShot) {
            super(skill, "IceVolleyShot", applier, duration, applyText, expireText); //TODO Implicit broadcast() call - may need changes?

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);

            this.maxArrowsPerShot = maxArrowsPerShot;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            for (final Effect effect : hero.getEffects()) {
                if (effect.equals(this)) {
                    continue;
                }

                if (effect.isType(EffectType.IMBUE)) {
                    hero.removeEffect(effect);
                }
            }
        }

        public int getMaxArrowsPerShot() {
            return maxArrowsPerShot;
        }

        public void setMaxArrowsPerShot(int maxArrowsPerShot) {
            this.maxArrowsPerShot = maxArrowsPerShot;
        }
    }
}
