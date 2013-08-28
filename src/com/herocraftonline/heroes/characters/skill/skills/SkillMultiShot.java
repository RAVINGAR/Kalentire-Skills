package com.herocraftonline.heroes.characters.skill.skills;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillMultiShot extends ActiveSkill {

    private boolean ncpEnabled = false;
    private String expireText;

    private Map<Arrow, Long> multiShots = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 1L;

        protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
            return (size() > 60) || (((Long) eldest.getValue()).longValue() + 5000L <= System.currentTimeMillis());
        }
    };

    public SkillMultiShot(Heroes plugin) {
        super(plugin, "MultiShot");
        setDescription("Enter Multi Shot Mode. While in Multi Shot Mode, you will your bow will launch up to $1 arrows at once! Multi Shot arrows are less powerful than normal ones however.");
        setUsage("/skill multishot");
        setArgumentRange(0, 0);
        setIdentifiers("skill multishot");
        setTypes(SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.BUFFING, SkillType.AGGRESSIVE, SkillType.DAMAGING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
    }

    public String getDescription(Hero hero) {

        int arrowsPerShot = SkillConfigManager.getUseSetting(hero, this, "arrows-per-shot", Integer.valueOf(5), false);

        return getDescription().replace("$1", arrowsPerShot + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("max-arrows-per-shot", Integer.valueOf(6));
        node.set("degrees", Double.valueOf(10.0));
        node.set("velocity-multiplier", Double.valueOf(3.0));

        return node;
    }

    public SkillResult use(Hero hero, String[] args) {

        Player player = hero.getPlayer();
        PlayerInventory inventory = player.getInventory();

        Map<Integer, ? extends ItemStack> arrowSlots = inventory.all(Material.ARROW);

        int numHeldArrows = 0;
        for (Map.Entry<Integer, ? extends ItemStack> entry : arrowSlots.entrySet()) {
            numHeldArrows += entry.getValue().getAmount();
        }

        if (numHeldArrows == 0) {
            Messaging.send(player, "You don't have any arrows!");
            return new SkillResult(ResultType.MISSING_REAGENT, false);
        }

        int arrowIncrement = SkillConfigManager.getUseSetting(hero, this, "arrows-loaded-per-use", 6, false);

        // Check to make sure they haven't already hit their max number of arrows before proceeding
        int currentlyLoadedArrows = 0;
        boolean hasEffectAlready = false;
        MultiShotEffect msEffect = null;
        if (hero.hasEffect("Multishot")) {
            msEffect = (MultiShotEffect) hero.getEffect("MultiShot");
            currentlyLoadedArrows = msEffect.getCurrentlyLoadedArrows();

            if (msEffect.getCurrentlyLoadedArrows() == msEffect.getMaxTotalArrows()) {
                Messaging.send(player, "You've already loaded your maximum number of arrows!");
                return SkillResult.FAIL;
            }
            else if (arrowIncrement + msEffect.getCurrentlyLoadedArrows() > msEffect.getMaxTotalArrows()) {
                // Put them at max if they're offset by a couple of arrows for some reason.
                arrowIncrement = msEffect.getMaxTotalArrows() - msEffect.getCurrentlyLoadedArrows();
            }

            hasEffectAlready = true;
        }

        // If they don't have enough arrows, cancel.
        if (numHeldArrows < currentlyLoadedArrows + arrowIncrement) {
            Messaging.send(player, "You don't have enough arrows to load!");
            return new SkillResult(ResultType.MISSING_REAGENT, false);
        }

        // They haven't loaded any arrows before. Let's add the effect before we load them.
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 12000, false);
        int maxArrowsPerShot = SkillConfigManager.getUseSetting(hero, this, "max-arrows-per-shot", 6, false);
        int maxTotalArrows = SkillConfigManager.getUseSetting(hero, this, "max-total-arrows", 30, false);

        if (hasEffectAlready) {
            // Remove the effect if they actually have it already, but don't show expire text when we do it this way.
            msEffect.showExpireText = false;
            hero.removeEffect(msEffect);
        }

        hero.addEffect(new MultiShotEffect(this, duration, currentlyLoadedArrows, maxArrowsPerShot, maxTotalArrows));

        // "Load" the arrows
        msEffect = (MultiShotEffect) hero.getEffect("MultiShot");
        msEffect.addArrows(arrowIncrement, player);

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
            if (!hero.hasEffect("MultiShot"))
                return;

            // Get the effect from the player, and fire "up to" the set max arrows per shot.
            MultiShotEffect msEffect = (MultiShotEffect) hero.getEffect("MultiShot");

            // We're in the middle of shooting a multishot arrow, we don't want to listen to this event.
            if (!msEffect.shouldListenToBowShootEvents())
                return;

            // Don't proceed if we don't actually have any arrows loaded.
            if (msEffect.getCurrentlyLoadedArrows() < 2)
                return;

            int maxArrowsToShoot = 0;
            if (msEffect.getCurrentlyLoadedArrows() > msEffect.getMaxArrowsPerShot())
                maxArrowsToShoot = msEffect.getMaxArrowsPerShot();
            else
                maxArrowsToShoot = msEffect.getCurrentlyLoadedArrows();

            // Ensure the player still has enough arrows in his inventory.
            Player player = hero.getPlayer();
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
            velocityMultiplier = force * velocityMultiplier;    // Reduce the velocity based on how far back they pulled their bow.

            // Create arrow spread
            double degrees = SkillConfigManager.getUseSetting(hero, skill, "degrees", 65.0, false);
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
            msEffect.setListenToBowShootEvents(false);      // Prevent the following bow events to be picked up by this method.
            for (double a = actualCenterDegreesRad; a <= degreesRad; a += diff) {
                shootMultiShotArrow(player, bow, force, yaw + a, pitchMultiplier, velocityMultiplier);
            }

            // Fire arrows from the start and move clockwise towards the center
            for (double a = 0; a < actualCenterDegreesRad; a += diff) {
                shootMultiShotArrow(player, bow, force, yaw + a, pitchMultiplier, velocityMultiplier);
            }

            // Let's bypass the nocheat issues...
            if (ncpEnabled) {
                if (!player.isOp()) {
                    if (hero.hasEffect("NCPExemptionEffect_FIGHT"))
                        hero.removeEffect(hero.getEffect("NCPExemptionEffect_FIGHT"));
                }
            }

            // Allow further bow events to be listened to.
            msEffect.setListenToBowShootEvents(true);

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

            // Remove the arrows from the buff. If the buff is out of arrows, remove it.
            msEffect.removeArrows(arrowsToShoot, player);
            if (msEffect.getCurrentlyLoadedArrows() == 0)
                hero.removeEffect(msEffect);
            else {
                // Refresh the buff after ever shot.
                int duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 6000, false);
                int maxArrowsPerShot = SkillConfigManager.getUseSetting(hero, skill, "max-arrows-per-shot", 5, false);
                int maxTotalArrows = SkillConfigManager.getUseSetting(hero, skill, "max-total-arrows", 30, false);
                int currentlyLoadedArrows = msEffect.getCurrentlyLoadedArrows();

                msEffect.showExpireText = false;
                hero.removeEffect(msEffect);

                hero.addEffect(new MultiShotEffect(skill, duration, currentlyLoadedArrows, maxArrowsPerShot, maxTotalArrows));
            }
        }

        private void shootMultiShotArrow(Player player, ItemStack bow, float force, double yaw, double pitchMultiplier, double velocityMultiplier) {

            Arrow arrow = player.launchProjectile(Arrow.class);
            double newYValue = arrow.getVelocity().getY();

            // Create our velocity direction based on where the player is facing.
            Vector vel = new Vector(Math.cos(yaw), 0, Math.sin(yaw));
            vel.multiply(pitchMultiplier * velocityMultiplier);
            vel.setY(newYValue * velocityMultiplier);

            arrow.setVelocity(vel);    // Apply multiplier so it goes farther.

            arrow.setShooter(player);
            multiShots.put(arrow, Long.valueOf(System.currentTimeMillis()));
            final EntityShootBowEvent fakeShootBowEvent = new EntityShootBowEvent(player, bow, arrow, force);
            plugin.getServer().getPluginManager().callEvent(fakeShootBowEvent);
        }
    }

    // Buff effect used to keep track of multishot functionality
    public class MultiShotEffect extends ExpirableEffect {

        private int maxArrowsPerShot;
        private int maxTotalArrows;
        private int currentlyLoadedArrows;
        private boolean listenToBowShootEvents;
        private boolean showExpireText;

        public MultiShotEffect(Skill skill, long duration, int maxArrowsPerShot, int maxTotalArrows) {
            super(skill, "MultiShot", duration);

            currentlyLoadedArrows = 0;
            setShowExpireText(true);

            setListenToBowShootEvents(true);
            setMaxArrowsPerShot(maxArrowsPerShot);
            setMaxTotalArrows(maxTotalArrows);

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);
        }

        public MultiShotEffect(Skill skill, long duration, int currentlyLoadedArrows, int maxArrowsPerShot, int maxTotalArrows) {
            super(skill, "MultiShot", duration);

            this.currentlyLoadedArrows = currentlyLoadedArrows;
            setShowExpireText(true);

            setListenToBowShootEvents(true);
            setMaxArrowsPerShot(maxArrowsPerShot);
            setMaxTotalArrows(maxTotalArrows);

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();

            if (showExpireText)
                Messaging.send(player, expireText);
        }

        public int getCurrentlyLoadedArrows() {
            return currentlyLoadedArrows;
        }

        public void addArrows(int numArrows, Player player) {
            if (currentlyLoadedArrows + numArrows <= maxTotalArrows)
                this.currentlyLoadedArrows += numArrows;
            else
                this.currentlyLoadedArrows = maxTotalArrows;

            Messaging.send(player, "MultiShot Arrows: " + this.currentlyLoadedArrows);
        }

        public void removeArrows(int numArrows, Player player) {
            if (currentlyLoadedArrows - numArrows >= 0)
                this.currentlyLoadedArrows -= numArrows;
            else
                this.currentlyLoadedArrows = 0;

            Messaging.send(player, "MultiShot Arrows: " + this.currentlyLoadedArrows);
        }

        public int getMaxTotalArrows() {
            return maxTotalArrows;
        }

        public void setMaxTotalArrows(int maxTotalArrows) {
            this.maxTotalArrows = maxTotalArrows;
        }

        public int getMaxArrowsPerShot() {
            return maxArrowsPerShot;
        }

        public void setMaxArrowsPerShot(int maxArrowsPerShot) {
            this.maxArrowsPerShot = maxArrowsPerShot;
        }

        public boolean shouldListenToBowShootEvents() {
            return listenToBowShootEvents;
        }

        public void setListenToBowShootEvents(boolean listenToBowShootEvents) {
            this.listenToBowShootEvents = listenToBowShootEvents;
        }

        public boolean getShowExpireText() {
            return showExpireText;
        }

        public void setShowExpireText(boolean showExpireText) {
            this.showExpireText = showExpireText;
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
