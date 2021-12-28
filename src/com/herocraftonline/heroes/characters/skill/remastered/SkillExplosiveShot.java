package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
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
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SkillExplosiveShot extends ActiveSkill {

    private boolean ncpEnabled = false;
    public VisualEffect fplayer = new VisualEffect();

    private Map<Arrow, Long> explosiveShots = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 1L;

        protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
            return (size() > 60) || (eldest.getValue() + 5000L <= System.currentTimeMillis());
        }
    };

    private String applyText;
    private String expireText;
    private String shotText;

    public SkillExplosiveShot(Heroes plugin) {
        super(plugin, "ExplosiveShot");
        setDescription("Apply a explosive charge to $1 of your arrows. Once attached, your $2 fired within the next $3 seconds will damage all targets within $4 blocks for $5 damage. Targets that are hit with the blast are also knocked away from the explosion.");
        setUsage("/skill explosiveshot");
        setArgumentRange(0, 0);
        setIdentifiers("skill explosiveshot");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_FIRE, SkillType.FORCE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null)
            ncpEnabled = true;
    }

    public String getDescription(Hero hero) {
        int numShots = SkillConfigManager.getUseSetting(hero, this, "num-shots", 1, false);

        String numShotsString;
        if (numShots > 1)
            numShotsString = "next " + numShots + " shots";
        else
            numShotsString = "next shot";

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 2, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDamage = Util.decFormat.format(damage);


        String directHitEffect = "";
        double altArrowDamage;
        if (SkillConfigManager.getUseSetting(hero, this, "cancel-arrow-damage", true)) {
            altArrowDamage = SkillConfigManager.getUseSetting(hero, this, "alt-arrow-damage", 75, false);
            if (altArrowDamage > 0) {
                directHitEffect = " The arrow deals " + altArrowDamage + " damage on hitting a target directly.";
            }
        } else {
            directHitEffect = " The arrow deals standard Bow damage on hitting a target directly.";
        }

        return getDescription().replace("$1", numShots + "").replace("$2", numShotsString + "").replace("$3", formattedDuration).replace("$4", radius + "").replace("$5", formattedDamage) + directHitEffect;
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.USE_TEXT.node(), "");
        config.set("num-shots", 1);
        config.set(SkillSetting.DURATION.node(), 4000);
        config.set(SkillSetting.RADIUS.node(), 4);
        config.set(SkillSetting.DAMAGE.node(), 80D);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 2.0D);
        config.set("horizontal-power", 1.1D);
        config.set("vertical-power", 0.5D);
        config.set("ncp-exemption-duration", 0);
        config.set("cancel-arrow-damage", true);
        config.set("alt-arrow-damage", 75D);
        config.set(SkillSetting.APPLY_TEXT.node(), String.valueOf(ChatComponents.GENERIC_SKILL + "%hero%'s arrows are " + ChatColor.WHITE + ChatColor.BOLD + "Explosive" + ChatColor.RESET + "!"));
        config.set(SkillSetting.EXPIRE_TEXT.node(), String.valueOf(ChatComponents.GENERIC_SKILL + "%hero%'s arrows are no longer Explosive."));

        return config;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero%'s arrows are " + ChatColor.WHITE + ChatColor.BOLD + "Explosive" + ChatColor.RESET + "!")
                .replace("%hero%", "$1");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero%'s arrows are no longer Explosive.")
                .replace("%hero%", "$1");

        shotText = SkillConfigManager.getRaw(this, "shot-text",
                ChatComponents.GENERIC_SKILL + "%hero% has unleashed an " + ChatColor.WHITE + ChatColor.BOLD + "Explosive Shot" + ChatColor.RESET + "!");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
        int numShots = SkillConfigManager.getUseSetting(hero, this, "num-shots", 1, false);

        hero.addEffect(new ExplosiveShotBuffEffect(this, player, duration, numShots));

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        public void addParticleEffect(final Projectile p) {
            new BukkitRunnable() {
                public void run() {
                    if (explosiveShots.containsKey(p)) {
                        Location loc = p.getLocation();
                        //p.getWorld().spigot().playEffect(loc, org.bukkit.Effect.SMOKE, 0, 0, 0.0F, 0.1F, 0.0F, 0.0F, 1, 16);
                        p.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 1, 0, 0.1, 0, 0);
                    } else {
                        this.cancel();
                        return;
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityShootBow(EntityShootBowEvent event) {
            if ((!(event.getEntity() instanceof Player)) || (!(event.getProjectile() instanceof Arrow))) {
                return;
            }

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (!hero.hasEffect("ExplosiveShotBuffEffect"))
                return;

            // Lower the number of shots left on the buff
            ExplosiveShotBuffEffect bEffect = (ExplosiveShotBuffEffect) hero.getEffect("ExplosiveShotBuffEffect");

            // If we're out of shots, remove the buff.
            if (bEffect.getShotsLeft() < 1)
                return;

            bEffect.setShotsLeft(bEffect.getShotsLeft() - 1);

            // If we're out of shots, remove the buff.
            if (bEffect.getShotsLeft() < 1) {
                hero.removeEffect(bEffect);
            }

            Player player = hero.getPlayer();
            broadcast(player.getLocation(), shotText.replace("%hero%", player.getDisplayName()));

            // Add the projectile to the hashlist
            Arrow explosiveShot = (Arrow) event.getProjectile();
            addParticleEffect(explosiveShot);
            //explosiveShot.setFireTicks(20); // Users are saying this Skill lights allies on fire, this is the only fire here.
            explosiveShots.put(explosiveShot, System.currentTimeMillis());
        }

        // Checks that it's an arrow shot by a player that's an ExplosiveShot, then waits a tick and does the exploding
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onProjectileHit(ProjectileHitEvent event) {
            if (!(event.getEntity() instanceof Arrow))
                return;

            final Arrow projectile = (Arrow) event.getEntity();
            if (!(projectile.getShooter() instanceof Player))
                return;

            if (!(explosiveShots.containsKey(projectile)))
                return;

            // Wait a tick to explode, otherwise we can't cancel the damage since the shot is removed from the Map
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    // Run the explosion logic
                    explode(projectile);
                    // Remove the shot from the hashlist
                    explosiveShots.remove(projectile);
                    // Remove the arrow from the world (because it can bounce)
                    projectile.remove();
                }
            });

        }

        // Without this, the arrow itself will do additional damage. May not want.
        @EventHandler(ignoreCancelled = true)
        public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
            if (!(event.getDamager() instanceof Arrow))
                return;

            Arrow projectile = (Arrow) event.getDamager();
            if (!(projectile.getShooter() instanceof Player))
                return;

            if (!(explosiveShots.containsKey(projectile)))
                return;

            Player shooter = (Player) projectile.getShooter();
            Hero hero = plugin.getCharacterManager().getHero(shooter);

            // If we're not cancelling the default arrow, we don't care
            if (SkillConfigManager.getUseSetting(hero, skill, "cancel-arrow-damage", true)) {
                // Cancel first, quick, before we do something crazy
                event.setCancelled(true);

                // Run our own, controlled damage on arrow hit, if set.
                double altArrowDamage = SkillConfigManager.getUseSetting(hero, skill, "alt-arrow-damage", 75, false);
                if (altArrowDamage > 0) {
                    // Grab entity from event, the rest is copied from explode()
                    Entity entity = event.getEntity();
                    if (!(entity instanceof LivingEntity) || !damageCheck(shooter, (LivingEntity) entity))
                        return;

                    // Damage target
                    LivingEntity target = (LivingEntity) entity;
                    addSpellTarget(target, hero);
                    damageEntity(target, shooter, altArrowDamage, DamageCause.MAGIC, false);
                }
            }

        }
    }

    // Runs the explosion effect and damage off the projectile that's hit something
    public void explode(Arrow projectile) {

        // Grab our shooter and hero from projectile info
        Player shooter = (Player) projectile.getShooter();
        Hero hero = plugin.getCharacterManager().getHero(shooter);

        // BOOM - for some reason the code in the try/catch block down there isn't happy about the whole "working" thing
        //projectile.getWorld().spigot().playEffect(projectile.getLocation(), org.bukkit.Effect.EXPLOSION_LARGE, 0, 0, 1.0F, 1.0F, 1.0F, 0.0F, 10, 16);
        projectile.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, projectile.getLocation(), 10, 1, 1, 1, 0);
        //projectile.getWorld().playSound(projectile.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 2.0, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        // Play an effect, but only if we actually hit a target.
        // Play explosion effect
        projectile.getWorld().playEffect(projectile.getLocation(), org.bukkit.Effect.SMOKE, 4);
        try {
            fplayer.playFirework(projectile.getWorld(), projectile.getLocation(), FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BURST).withColor(Color.ORANGE).withFade(Color.RED).build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }


        // Prep some variables
        Location arrowLoc = projectile.getLocation();
        List<Entity> targets = projectile.getNearbyEntities(radius, radius, radius);
        double horizontalPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 1.1, false);
        double verticalPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.5, false);

        // Loop through nearby targets and damage / knock them back
        for (Entity entity : targets) {
            // Check to see if the entity can be damaged
            if (!(entity instanceof LivingEntity) || !damageCheck(shooter, (LivingEntity) entity))
                continue;

            // Damage target
            LivingEntity target = (LivingEntity) entity;
            addSpellTarget(target, hero);
            damageEntity(target, shooter, damage, DamageCause.MAGIC);

            // Do a knock up/back effect.
            Location targetLoc = target.getLocation();

            double xDir = targetLoc.getX() - arrowLoc.getX();
            double zDir = targetLoc.getZ() - arrowLoc.getZ();
            double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

            xDir = xDir / magnitude * horizontalPower;
            zDir = zDir / magnitude * horizontalPower;

            if (ncpEnabled) {
                if (target instanceof Player) {
                    Player targetPlayer = (Player) target;
                    if (!targetPlayer.isOp()) {
                        long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 500, false);
                        if (duration > 0) {
                            NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, targetPlayer, duration);
                            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                            targetCT.addEffect(ncpExemptEffect);
                        }
                    }
                }
            }

            target.setVelocity(new Vector(xDir, verticalPower, zDir));

        }
    }

    // Buff effect used to keep track of grappling hook uses
    public class ExplosiveShotBuffEffect extends ExpirableEffect {

        private int shotsLeft = 1;
        private boolean showExpireText = true;

        public ExplosiveShotBuffEffect(Skill skill, Player applier, long duration, int shotsLeft) {
            super(skill, "ExplosiveShotBuffEffect", applier, duration, applyText, null); //TODO Implicit broadcast() call - may need changes?

            types.add(EffectType.IMBUE);
            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);

            this.shotsLeft = shotsLeft;
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

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();

            if (showExpireText) { // This stays only because someone may theoretically touch this value. //TODO Remove when we do breaking changes?
                if (expireText != null && expireText.length() > 0) {
                    if (hero.hasEffectType(EffectType.SILENT_ACTIONS))
                        player.sendMessage(expireText.replace("$1", player.getDisplayName()));
                    else
                        broadcast(player.getLocation(), expireText, player.getDisplayName());
                }
            }
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

        public NCPExemptionEffect(Skill skill, Player applier, long duration) {
            super(skill, "NCPExemptionEffect_MOVING", applier, duration);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPUtils.exempt(player, "MOVING");
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPUtils.unexempt(player, "MOVING", 0);
        }
    }
}





