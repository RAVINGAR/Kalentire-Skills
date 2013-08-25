package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
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
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillGrapplingHook extends ActiveSkill {

    private boolean ncpEnabled = false;

    private Map<Arrow, Long> grapplingHooks = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 1L;

        protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
            return (size() > 60) || (((Long) eldest.getValue()).longValue() + 5000L <= System.currentTimeMillis());
        }
    };

    private Map<Arrow, Long> grapplingHooksAttachedToPlayers = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 1L;

        protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
            return (size() > 60) || (((Long) eldest.getValue()).longValue() + 5000L <= System.currentTimeMillis());
        }
    };

    private String applyText;
    private String expireText;

    public SkillGrapplingHook(Heroes plugin) {
        super(plugin, "GrapplingHook");
        setDescription("Apply a grappling hook to $1 of your arrows. Once attached, your $2 fired within the next $3 seconds will grapple you to the targeted location! Hitting a target with the grappling hook will pull them to you instead however. The grappling hook weighs down your arrows however and reduces their velocity by $4%.");
        setUsage("/skill grapplinghook");
        setArgumentRange(0, 0);
        setIdentifiers("skill grapplinghook");
        setTypes(SkillType.PHYSICAL, SkillType.BUFF, SkillType.FORCE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);

        try {
            if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
                ncpEnabled = true;
            }
        }
        catch (Exception e) {}
    }

    public String getDescription(Hero hero) {
        int numShots = SkillConfigManager.getUseSetting(hero, this, "num-shots", 1, false);

        String numShotsString = "";
        if (numShots > 1)
            numShotsString = "next " + numShots + " shots";
        else
            numShotsString = "next shot";

        double velocityMultiplier = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 0.5D, false));
        velocityMultiplier = Util.formatDouble((1.0 - velocityMultiplier) * 100.0);
        double duration = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false) / 1000.0);

        return getDescription().replace("$1", numShots + "").replace("$2", numShotsString + "").replace("$3", duration + "").replace("$4", velocityMultiplier + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("num-shots", Integer.valueOf(1));
        node.set("velocity-multiplier", Double.valueOf(0.5D));
        node.set("max-distance", Integer.valueOf(35));
        node.set("safe-fall-duration", Integer.valueOf(4000));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(5000));
        node.set("horizontal-divider", Integer.valueOf(6));
        node.set("vertical-divider", Integer.valueOf(8));
        node.set("multiplier", Double.valueOf(1.2));
        node.set("ncp-exemption-duration", 3000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% readies his grappling hook!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% drops his grappling hook.");

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% readies his grappling hook!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% drops his grappling hook.").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 12000, false);
        int numShots = SkillConfigManager.getUseSetting(hero, this, "num-shots", 1, false);
        hero.addEffect(new GrapplingHookBuffEffect(this, duration, numShots));

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityShootBow(EntityShootBowEvent event) {
            if ((!(event.getEntity() instanceof Player)) || (!(event.getProjectile() instanceof Arrow))) {
                return;
            }

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("GrapplingHookBuffEffect")) {

                // Lower the number of shots left on the buff
                GrapplingHookBuffEffect ghbEffect = (GrapplingHookBuffEffect) hero.getEffect("GrapplingHookBuffEffect");

                if (ghbEffect.getShotsLeft() < 1)
                    return;

                ghbEffect.setShotsLeft(ghbEffect.getShotsLeft() - 1);

                // If we're out of grapples, remove the buff.
                if (ghbEffect.getShotsLeft() < 1) {
                    ghbEffect.setShowExpireText(false);		// Don't show expire text if
                    hero.removeEffect(ghbEffect);
                }

                // Modify the projectile
                double velocityMultiplier = SkillConfigManager.getUseSetting(hero, skill, "velocity-multiplier", 0.5D, false);
                Arrow grapplingHook = (Arrow) event.getProjectile();
                grapplingHook.setVelocity(grapplingHook.getVelocity().multiply(velocityMultiplier));

                // Put it on the hashmap so we can check it in another event.
                grapplingHooks.put(grapplingHook, Long.valueOf(System.currentTimeMillis()));
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onProjectileHit(ProjectileHitEvent event) {
            if (!(event.getEntity() instanceof Arrow))
                return;

            final Arrow grapplingHook = (Arrow) event.getEntity();
            if ((!(grapplingHook.getShooter() instanceof Player)))
                return;

            if (!(grapplingHooks.containsKey(grapplingHook)) || grapplingHooksAttachedToPlayers.containsKey(grapplingHook))
                return;

            Player shooter = (Player) grapplingHook.getShooter();
            final Hero hero = plugin.getCharacterManager().getHero(shooter);

            double grappleDelay = SkillConfigManager.getUseSetting(hero, skill, "grapple-delay", 0.5, false);

            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
            {
                public void run()
                {
                    if (!(grapplingHooks.containsKey(grapplingHook)) || grapplingHooksAttachedToPlayers.containsKey(grapplingHook))
                        return;

                    // Grapple!
                    grapplingHooks.remove(grapplingHook);
                    grappleToLocation(hero, grapplingHook.getLocation());
                }
            }, (long) (grappleDelay * 20));

            return;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if ((!(event instanceof EntityDamageByEntityEvent)) || (!(event.getEntity() instanceof LivingEntity))) {
                return;
            }

            Entity projectile = ((EntityDamageByEntityEvent) event).getDamager();
            if ((!(projectile instanceof Arrow)) || (!(((Projectile) projectile).getShooter() instanceof Player))) {
                return;
            }

            if (!(grapplingHooks.containsKey((Arrow) projectile))) {
                return;
            }

            final Arrow grapplingHook = (Arrow) projectile;
            Player player = (Player) grapplingHook.getShooter();
            final Hero hero = plugin.getCharacterManager().getHero(player);
            final LivingEntity targetLE = (LivingEntity) event.getEntity();

            // Switch from the normal hook to the player hook.
            grapplingHooksAttachedToPlayers.put(grapplingHook, Long.valueOf(System.currentTimeMillis()));

            double grappleDelay = SkillConfigManager.getUseSetting(hero, skill, "grapple-delay", 0.5, false);

            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
            {
                public void run()
                {
                    // Grapple
                    grappleTargetToPlayer(hero, targetLE);
                    grapplingHooks.remove(grapplingHook);
                    grapplingHooksAttachedToPlayers.remove(grapplingHook);

                }
            }, (long) (grappleDelay * 20));
        }
    }

    private void grappleToLocation(Hero hero, Location targetLoc) {

        Player player = hero.getPlayer();

        Location playerLoc = player.getLocation();
        if (!(playerLoc.getWorld().equals(targetLoc.getWorld())))
            return;

        Vector playerLocVec = player.getLocation().toVector();
        Vector locVec = targetLoc.toVector();

        double distance = (int) playerLocVec.distance(locVec);

        int maxDistance = SkillConfigManager.getUseSetting(hero, this, "max-distance", 35, false);
        if (maxDistance > 0) {
            if (distance > maxDistance) {
                Messaging.send(player, "You threw your hook to far and lost your grip!", new Object[0]);
                return;
            }
        }

        // If the player is aiming downwards, don't let him increase his y.
        boolean noY = false;
        if (locVec.getY() < playerLoc.getY())
            noY = true;

        // Create our distance vector
        //        Vector dVector = locVec.subtract(playerLocVec);
        //
        //        // Store the block variables
        //        int dX = dVector.getBlockX();
        //        int dY = dVector.getBlockY();
        //        int dZ = dVector.getBlockZ();

        // Calculate pull vector
        //        int multiplier = (int) ((Math.abs(dX) + Math.abs(dY) + Math.abs(dZ)) / 6);
        //        int ymultiplier = (int) (Math.abs(dY) - (Math.abs(dX) + Math.abs(dZ)) / 30);
        //        Vector vec = new Vector(dX, Math.abs(dY) + ymultiplier, dZ).normalize().multiply(multiplier);

        double horizontalDivider = SkillConfigManager.getUseSetting(hero, this, "horizontal-divider", 6, false);
        double verticalDivider = SkillConfigManager.getUseSetting(hero, this, "vertical-divider", 8, false);
        double xDir = (targetLoc.getX() - playerLoc.getX()) / horizontalDivider;
        double yDir = (targetLoc.getY() - playerLoc.getY()) / verticalDivider;
        double zDir = (targetLoc.getZ() - playerLoc.getZ()) / horizontalDivider;
        double multiplier = SkillConfigManager.getUseSetting(hero, this, "multiplier", 1.2, false);
        Vector vec = new Vector(xDir, yDir, zDir).multiply(multiplier);

        // Prevent y velocity increase if told to.
        if (noY) {
            vec.multiply(0.5).setY(0.5);	// Half the power of the grapple, and eliminate the y power
        }
        else {
            // As long as we have Y, give them safefall
            int safeFallDuration = SkillConfigManager.getUseSetting(hero, this, "safe-fall-duration", 5000, false);
            hero.addEffect(new SafeFallEffect(this, safeFallDuration));
        }

        // Let's bypass the nocheat issues...
        if (ncpEnabled) {
            if (!player.isOp()) {
                long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 3000, false);
                if (duration > 0) {
                    NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, duration);
                    hero.addEffect(ncpExemptEffect);
                }
            }
        }

        // Grapple!
        player.getWorld().playSound(playerLoc, Sound.MAGMACUBE_JUMP, 0.8F, 1.0F);
        player.setVelocity(vec);
    }

    private void grappleTargetToPlayer(Hero hero, LivingEntity target) {

        Player player = hero.getPlayer();

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();
        if (!(playerLoc.getWorld().equals(targetLoc.getWorld())))
            return;

        Vector playerLocVec = player.getLocation().toVector();
        Vector locVec = targetLoc.toVector();

        double distance = (int) playerLocVec.distance(locVec);

        int maxDistance = SkillConfigManager.getUseSetting(hero, this, "max-distance", 35, false);
        if (maxDistance > 0) {
            if (distance > maxDistance) {
                Messaging.send(player, "You threw your hook to far and lost your grip!", new Object[0]);
                return;
            }
        }

        double horizontalDivider = SkillConfigManager.getUseSetting(hero, this, "horizontal-divider", 6, false);
        double xDir = (playerLoc.getX() - targetLoc.getX()) / horizontalDivider;
        double zDir = (playerLoc.getZ() - targetLoc.getZ()) / horizontalDivider;
        double multiplier = SkillConfigManager.getUseSetting(hero, this, "multiplier", 1.2, false);
        Vector vec = new Vector(xDir, 0, zDir).multiply(multiplier).setY(0.7);

        // Let's bypass the nocheat issues...
        if (ncpEnabled) {
            if (target instanceof Player) {
                Player targetPlayer = (Player) target;

                if (!targetPlayer.isOp()) {
                    long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 3000, false);
                    if (duration > 0) {
                        NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, duration);
                        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                        targetCT.addEffect(ncpExemptEffect);
                    }
                }
            }
        }

        // Grapple!
        player.getWorld().playSound(playerLoc, Sound.MAGMACUBE_JUMP, 0.8F, 1.0F);
        target.setVelocity(vec);
    }

    // Buff effect used to keep track of grappling hook uses
    public class GrapplingHookBuffEffect extends ExpirableEffect {

        private int shotsLeft = 1;
        private boolean showExpireText = true;

        public GrapplingHookBuffEffect(Skill skill, long duration, int numShots) {
            super(skill, "GrapplingHookBuffEffect", duration);
            this.shotsLeft = numShots;

            types.add(EffectType.IMBUE);
            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);
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
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();

            if (showExpireText)
                broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

        public int getShotsLeft() {
            return shotsLeft;
        }

        public void setShotsLeft(int shotsLeft) {
            this.shotsLeft = shotsLeft;
        }

        public void setShowExpireText(boolean showExpireText) {
            this.showExpireText = showExpireText;
        }
    }

    private class NCPExemptionEffect extends ExpirableEffect {

        public NCPExemptionEffect(Skill skill, long duration) {
            super(skill, "NCPExemptionEffect", duration);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.MOVING);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.MOVING);

        }
    }
}