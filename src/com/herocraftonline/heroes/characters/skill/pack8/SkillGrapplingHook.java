package com.herocraftonline.heroes.characters.skill.pack8;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
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
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;

public class SkillGrapplingHook extends ActiveSkill {

    private Map<Arrow, Long> grapplingHooks = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 1L;

        protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
            return (size() > 60) || (eldest.getValue() + 5000L <= System.currentTimeMillis());
        }
    };

    private Map<Arrow, Long> grapplingHooksAttachedToPlayers = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 1L;

        protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
            return (size() > 60) || (eldest.getValue() + 5000L <= System.currentTimeMillis());
        }
    };

    private String applyText;
    private String expireText;

    public SkillGrapplingHook(Heroes plugin) {
        super(plugin, "GrapplingHook");
        setDescription("Apply a grappling hook to $1 of your arrows. Once attached, your $2 fired within the next $3 seconds will grapple you to the targeted location! Hitting a target with the grappling hook will pull them to you instead however.");
        setUsage("/skill grapplinghook");
        setArgumentRange(0, 0);
        setIdentifiers("skill grapplinghook");
        setTypes(SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.VELOCITY_INCREASING, SkillType.FORCE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        int numShots = SkillConfigManager.getUseSetting(hero, this, "num-shots", 1, false);

        String numShotsString = "";
        if (numShots > 1)
            numShotsString = "next " + numShots + " shots";
        else
            numShotsString = "next shot";

        //double velocityMultiplier = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 0.5D, false));
        //velocityMultiplier = Util.formatDouble((1.0 - velocityMultiplier) * 100.0);
        double duration = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false) / 1000.0);

        return getDescription().replace("$1", numShots + "").replace("$2", numShotsString + "").replace("$3", duration + "")/*.replace("$4", velocityMultiplier + "")*/;
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("num-shots", 1);
        //node.set("velocity-multiplier", 0.5D);
        node.set("max-distance", -1);
        node.set("safe-fall-duration", 4500);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set("horizontal-divider", 6);
        node.set("vertical-divider", 8);
        node.set("multiplier", 1.0);
        node.set("grapple-delay", 0.5);
        node.set("ncp-exemption-duration", 3000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% readies his grappling hook!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% drops his grappling hook.");
        node.set(SkillSetting.REAGENT.node(), 287);
        node.set(SkillSetting.REAGENT_COST.node(), 2);

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% readies his grappling hook!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% drops his grappling hook.").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 12000, false);
        int numShots = SkillConfigManager.getUseSetting(hero, this, "num-shots", 1, false);
        hero.addEffect(new GrapplingHookBuffEffect(this, hero.getPlayer(), duration, numShots));

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
                //double velocityMultiplier = SkillConfigManager.getUseSetting(hero, skill, "velocity-multiplier", 0.5D, false);
                Arrow grapplingHook = (Arrow) event.getProjectile();
                //grapplingHook.setVelocity(grapplingHook.getVelocity().normalize().multiply(velocityMultiplier));

                // Put it on the hashmap so we can check it in another event.
                grapplingHooks.put(grapplingHook, System.currentTimeMillis());
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

            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {
                    if (!(grapplingHooks.containsKey(grapplingHook)) || grapplingHooksAttachedToPlayers.containsKey(grapplingHook))
                        return;

                    // Grapple!
                    grapplingHooks.remove(grapplingHook);
                    grappleToLocation(hero, grapplingHook.getLocation());
                }
            }, (long) (grappleDelay * 20));

        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if ((!(event instanceof EntityDamageByEntityEvent)) || (!(event.getEntity() instanceof LivingEntity)))
                return;

            Entity projectile = ((EntityDamageByEntityEvent) event).getDamager();
            if ((!(projectile instanceof Arrow)) || (!(((Projectile) projectile).getShooter() instanceof Player)))
                return;

            if (!(grapplingHooks.containsKey(projectile)))
                return;

            final Arrow grapplingHook = (Arrow) projectile;
            Player player = (Player) grapplingHook.getShooter();
            final Hero hero = plugin.getCharacterManager().getHero(player);
            final LivingEntity targetLE = (LivingEntity) event.getEntity();

            // Switch from the normal hook to the player hook.
            grapplingHooksAttachedToPlayers.put(grapplingHook, System.currentTimeMillis());

            double grappleDelay = SkillConfigManager.getUseSetting(hero, skill, "grapple-delay", 0.5, false);

            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {
                    // Grapple
                    grappleTargetToPlayer(hero, targetLE);
                    grapplingHooks.remove(grapplingHook);
                    grapplingHooksAttachedToPlayers.remove(grapplingHook);

                }
            }, (long) (grappleDelay * 20));
        }
    }

    private void grappleToLocation(Hero hero, Location targetLoc) {

        final Player player = hero.getPlayer();

        Location playerLoc = player.getLocation();
        if (!(playerLoc.getWorld().equals(targetLoc.getWorld())))
            return;

        Vector playerLocVec = player.getLocation().toVector();
        Vector locVec = targetLoc.toVector();

        double distance = (int) playerLocVec.distance(locVec);

        int maxDistance = SkillConfigManager.getUseSetting(hero, this, "max-distance", 35, false);
        if (maxDistance > 0) {
            if (distance > maxDistance) {
                player.sendMessage("You threw your hook to far and lost your grip!");
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
        final Vector vec = new Vector(xDir, yDir, zDir).multiply(multiplier);

        // Prevent y velocity increase if told to.
        if (noY) {
            vec.multiply(0.5).setY(0.5);	// Half the power of the grapple, and eliminate the y power
        }
        else {
            // As long as we have Y, give them safefall
            int safeFallDuration = SkillConfigManager.getUseSetting(hero, this, "safe-fall-duration", 5000, false);
            hero.addEffect(new JumpSafeFallEffect(this, player, safeFallDuration));
        }

        // Grapple!
        player.getWorld().playSound(playerLoc, Sound.ENTITY_MAGMA_CUBE_JUMP, 0.8F, 1.0F);
        // Let's bypass the nocheat issues...
        NCPUtils.applyExemptions(player, new NCPFunction() {
            
            @Override
            public void execute()
            {
                player.setVelocity(vec);                
            }
        }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 3000, false));
    }

    private void grappleTargetToPlayer(Hero hero, final LivingEntity target) {

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
                player.sendMessage("You threw your hook to far and lost your grip!");
                return;
            }
        }

        double horizontalDivider = SkillConfigManager.getUseSetting(hero, this, "horizontal-divider", 6, false);
        double xDir = (playerLoc.getX() - targetLoc.getX()) / horizontalDivider;
        double zDir = (playerLoc.getZ() - targetLoc.getZ()) / horizontalDivider;
        double multiplier = SkillConfigManager.getUseSetting(hero, this, "multiplier", 1.2, false);
        final Vector vec = new Vector(xDir, 0, zDir).multiply(multiplier).setY(0.7);

        // Grapple!
        player.getWorld().playSound(playerLoc, Sound.ENTITY_MAGMA_CUBE_JUMP, 0.8F, 1.0F);
        // Let's bypass the nocheat issues...
        NCPUtils.applyExemptions(player, new NCPFunction() {
            
            @Override
            public void execute()
            {
                target.setVelocity(vec);
            }
        }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 3000, false));
    }

    // Buff effect used to keep track of grappling hook uses
    public class GrapplingHookBuffEffect extends ExpirableEffect {

        private int shotsLeft = 1;
        private boolean showExpireText = true;

        public GrapplingHookBuffEffect(Skill skill, Player applier, long duration, int numShots) {
            super(skill, "GrapplingHookBuffEffect", applier, duration);
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
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();

            if (showExpireText)
                broadcast(player.getLocation(), "    " + expireText, player.getName());
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

    private class JumpSafeFallEffect extends SafeFallEffect {

        public JumpSafeFallEffect(Skill skill, Player applier, int duration) {
            super(skill, "GrappleJumpSafeFall", applier, duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.JUMP_BOOST);

            addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration / 1000 * 20, 5), false);
        }
    }
}