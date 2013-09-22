package com.herocraftonline.heroes.characters.skill.skills;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillIceVolley extends ActiveSkill {

    private boolean ncpEnabled = false;

    private Map<Arrow, Long> iceVolleyShots = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 1L;

        protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
            return (size() > 60) || (((Long) eldest.getValue()).longValue() + 5000L <= System.currentTimeMillis());
        }
    };

    private String applyText;
    private String expireText;
    private String shotText;
    private String slowApplyText;
    private String slowExpireText;

    public SkillIceVolley(Heroes plugin) {
        super(plugin, "IceVolley");
        setDescription("Prepare an array of ice infused arrows. After use, your next next shot fired within $1 seconds will fire up to $2 arrows at once! Any target hit these arrows will be slowed for $3 seconds.");
        setUsage("/skill icevolley");
        setArgumentRange(0, 0);
        setIdentifiers("skill icevolley");
        setTypes(SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.AGGRESSIVE, SkillType.DEBUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
    }

    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(4000), false);
        int slowDuration = SkillConfigManager.getUseSetting(hero, this, "slow-duration", Integer.valueOf(2500), false);
        int arrowsPerShot = SkillConfigManager.getUseSetting(hero, this, "max-arrows-per-shot", Integer.valueOf(5), false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedSlowDuration = Util.decFormat.format(slowDuration / 1000.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", arrowsPerShot + "").replace("$3", formattedSlowDuration);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), Integer.valueOf(4000));
        node.set("max-arrows-per-shot", Integer.valueOf(5));
        node.set("degrees", Double.valueOf(10.0));
        node.set("velocity-multiplier", Double.valueOf(2.0));
        node.set("slow-multiplier", Integer.valueOf(1));
        node.set("slow-duration", Integer.valueOf(2500));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% has loaded an array of Ice Arrows!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is no longer firing a volley of arrows.");
        node.set("slow-apply-text", Messaging.getSkillDenoter() + "%target% has been slowed by %hero%'s Ice Volley!");
        node.set("slow-expire-text", Messaging.getSkillDenoter() + "%target% is no longer slowed!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% has loaded an " + ChatColor.WHITE + ChatColor.BOLD + "Ice Volley" + ChatColor.RESET + "!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% is no longer firing a volley of arrows.").replace("%hero%", "$1");
        shotText = SkillConfigManager.getRaw(this, "shot-text", Messaging.getSkillDenoter() + "%hero% has unleashed an " + ChatColor.WHITE + ChatColor.BOLD + "Ice Volley" + ChatColor.RESET + "!").replace("%hero%", "$1");

        slowApplyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%target% has been slowed by %hero%'s Ice Volley!").replace("%target%", "$1").replace("%hero%", "$2");
        slowExpireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%target% is no longer slowed!").replace("%target%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int maxArrowsPerShot = SkillConfigManager.getUseSetting(hero, this, "max-arrows-per-shot", Integer.valueOf(5), false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);

        hero.addEffect(new IceVolleyShotEffect(this, player, duration, maxArrowsPerShot));

        //target.getWorld().playSound(target.getLocation(), Sound.d, 0.7F, 2.0F);

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @SuppressWarnings("deprecation")
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

            Player player = hero.getPlayer();

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

            double velocityMultiplier = SkillConfigManager.getUseSetting(hero, skill, "velocity-multiplier", Double.valueOf(1.6), false);
            velocityMultiplier = force * velocityMultiplier;    // Reduce the velocity based on how far back they pulled their bow.

            // Create arrow spread
            double degrees = SkillConfigManager.getUseSetting(hero, skill, "degrees", Double.valueOf(65.0), false);
            double degreesRad = degrees * (Math.PI / 180);      // Convert degrees to radians
            double diff = degreesRad / (arrowsToShoot - 1);           // Create our difference for the spread

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
                    Messaging.send(player, shotText, player.getDisplayName());
                else
                    broadcast(player.getLocation(), shotText, player.getDisplayName());
            }

            // Let's bypass the nocheat issues...
            if (ncpEnabled) {
                if (!player.isOp()) {
                    NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(skill);
                    hero.addEffect(ncpExemptEffect);
                }
            }

            // Create a multiplier that lowers velocity based on how high or low the player is looking
            double pitchMultiplier = Math.abs(Math.sin(Math.abs(pitch) - 90));

            // Fire arrows from the center and move clockwise towards the end.
            ItemStack bow = event.getBow();
            for (double a = actualCenterDegreesRad; a <= degreesRad; a += diff) {
                shootIceVolleyArrow(player, yaw + a, pitchMultiplier, velocityMultiplier);
            }

            // Fire arrows from the start and move clockwise towards the center
            for (double a = 0; a < actualCenterDegreesRad; a += diff) {
                shootIceVolleyArrow(player, yaw + a, pitchMultiplier, velocityMultiplier);
            }

            // Let's bypass the nocheat issues...
            if (ncpEnabled) {
                if (!player.isOp()) {
                    if (hero.hasEffect("NCPExemptionEffect_FIGHT"))
                        hero.removeEffect(hero.getEffect("NCPExemptionEffect_FIGHT"));
                }
            }

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
                }
                else {
                    inventory.getItem(entry.getKey()).setAmount(amount - remove);
                }

                if (removedArrows >= (arrowsToShoot - 1)) {          // Offset by 1 due to the arrow removed by the initial event.
                    break;
                }
            }
            player.updateInventory();

            hero.removeEffect(msEffect);
        }

        private void shootIceVolleyArrow(Player player, double yaw, double pitchMultiplier, double velocityMultiplier) {

            Arrow arrow = player.launchProjectile(Arrow.class);

            // Create our velocity direction based on where the player is facing.
            Vector vel = new Vector(Math.cos(yaw), 0, Math.sin(yaw));
            vel.multiply(pitchMultiplier * velocityMultiplier);
            vel.setY(arrow.getVelocity().getY() * velocityMultiplier);

            arrow.setVelocity(vel);    // Apply multiplier so it goes farther.

            arrow.setShooter(player);
            iceVolleyShots.put(arrow, Long.valueOf(System.currentTimeMillis()));
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if ((!(event instanceof EntityDamageByEntityEvent)) || (!(event.getEntity() instanceof LivingEntity)))
                return;

            Entity projectile = ((EntityDamageByEntityEvent) event).getDamager();
            if ((!(projectile instanceof Arrow)) || (!(((Projectile) projectile).getShooter() instanceof Player)))
                return;

            if (!(iceVolleyShots.containsKey((Arrow) projectile)))
                return;

            final Arrow iceArrow = (Arrow) projectile;
            Player player = (Player) iceArrow.getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);

            long duration = SkillConfigManager.getUseSetting(hero, skill, "slow-duration", Integer.valueOf(2000), false);
            int amplifier = SkillConfigManager.getUseSetting(hero, skill, "slow-multiplier", Integer.valueOf(1), false);

            SlowEffect iceSlowEffect = new SlowEffect(skill, player, duration, amplifier, slowApplyText, slowExpireText);
            iceSlowEffect.types.add(EffectType.DISPELLABLE);
            iceSlowEffect.types.add(EffectType.ICE);

            LivingEntity target = (LivingEntity) event.getEntity();
            plugin.getCharacterManager().getCharacter(target).addEffect(iceSlowEffect);

            iceVolleyShots.remove(iceArrow);
        }
    }

    // Buff effect used to keep track of icevolley functionality
    public class IceVolleyShotEffect extends ExpirableEffect {

        private int maxArrowsPerShot;

        public IceVolleyShotEffect(Skill skill, Player applier, long duration, int maxArrowsPerShot) {
            super(skill, "IceVolleyShot", applier, duration, slowApplyText, slowExpireText);

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

            Player player = hero.getPlayer();

            if (applyText != null && applyText.length() > 0) {
                if (hero.hasEffectType(EffectType.SILENT_ACTIONS))
                    Messaging.send(player, applyText, player.getDisplayName());
                else
                    broadcast(player.getLocation(), applyText, player.getDisplayName());
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();

            if (expireText != null && expireText.length() > 0) {
                if (hero.hasEffectType(EffectType.SILENT_ACTIONS))
                    Messaging.send(player, expireText, player.getDisplayName());
                else
                    broadcast(player.getLocation(), expireText, player.getDisplayName());
            }
        }

        public int getMaxArrowsPerShot() {
            return maxArrowsPerShot;
        }

        public void setMaxArrowsPerShot(int maxArrowsPerShot) {
            this.maxArrowsPerShot = maxArrowsPerShot;
        }
    }

    private class NCPExemptionEffect extends Effect {

        public NCPExemptionEffect(Skill skill) {
            super(skill, "NCPExemptionEffect_FIGHT");
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.FIGHT);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.FIGHT);
        }
    }
}
