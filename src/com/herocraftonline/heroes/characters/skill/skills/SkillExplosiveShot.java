package com.herocraftonline.heroes.characters.skill.skills;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillExplosiveShot extends ActiveSkill {

    private boolean ncpEnabled = false;
    public VisualEffect fplayer = new VisualEffect();

    private Map<Arrow, Long> explosiveShots = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 1L;

        protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
            return (size() > 60) || (((Long) eldest.getValue()).longValue() + 5000L <= System.currentTimeMillis());
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
        setTypes(SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_FIRE, SkillType.FORCE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null)
            ncpEnabled = true;
    }

    public String getDescription(Hero hero) {
        int numShots = SkillConfigManager.getUseSetting(hero, this, "num-shots", 1, false);

        String numShotsString = "";
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

        return getDescription().replace("$1", numShots + "").replace("$2", numShotsString + "").replace("$3", formattedDuration).replace("$4", radius + "").replace("$5", formattedDamage);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.USE_TEXT.node(), "");
        node.set("num-shots", Integer.valueOf(1));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(4000));
        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(4));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(80));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(2.0));
        node.set("horizontal-power", Double.valueOf(1.1));
        node.set("vertical-power", Double.valueOf(0.5));
        node.set("ncp-exemption-duration", 500);
        node.set(SkillSetting.APPLY_TEXT.node(), String.valueOf(Messaging.getSkillDenoter() + "%hero%'s arrows are " + ChatColor.WHITE + ChatColor.BOLD + "Explosive" + ChatColor.RESET + "!"));
        node.set(SkillSetting.EXPIRE_TEXT.node(), String.valueOf(Messaging.getSkillDenoter() + "%hero%'s arrows are no longer Explosive."));


        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero%'s arrows are " + ChatColor.WHITE + ChatColor.BOLD + "Explosive Shot" + ChatColor.RESET + "!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, "expire-text-fail", Messaging.getSkillDenoter() + "%hero%'s arrows are no longer Explosive.").replace("%hero%", "$1");
        shotText = SkillConfigManager.getRaw(this, "shot-text", Messaging.getSkillDenoter() + "%hero% has unleashed an " + ChatColor.WHITE + ChatColor.BOLD + "Explosive Shot" + ChatColor.RESET + "!").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
        int numShots = SkillConfigManager.getUseSetting(hero, this, "num-shots", 1, false);
        hero.addEffect(new ExplosiveShotBuffEffect(this, hero.getPlayer(), duration, numShots));

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
            broadcast(player.getLocation(), shotText, player.getDisplayName());

            // Add the projectile to the hashlist
            Arrow explosiveShot = (Arrow) event.getProjectile();
            explosiveShot.setFireTicks(20);
            explosiveShots.put(explosiveShot, Long.valueOf(System.currentTimeMillis()));
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onProjectileHit(ProjectileHitEvent event) {
            if (!(event.getEntity() instanceof Arrow))
                return;

            Arrow projectile = (Arrow) event.getEntity();
            if ((!(projectile.getShooter() instanceof Player)))
                return;

            if (!(explosiveShots.containsKey(projectile)))
                return;

            Player shooter = (Player) projectile.getShooter();
            Hero hero = plugin.getCharacterManager().getHero(shooter);

            // Remove the shot from the hashlist
            explosiveShots.remove(projectile);

            int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 4, false);
            double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 80, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 2.0, false);
            damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

            // Play an effect, but only if we actually hit a target.
            // Play explosion effect
            projectile.getWorld().playEffect(projectile.getLocation(), org.bukkit.Effect.SMOKE, 4);
            try {
                fplayer.playFirework(projectile.getWorld(), projectile.getLocation(), FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BURST).withColor(Color.ORANGE).withFade(Color.RED).build());
            }
            catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            // Prep some variables
            Location arrowLoc = projectile.getLocation();
            List<Entity> targets = projectile.getNearbyEntities(radius, radius, radius);
            double horizontalPower = SkillConfigManager.getUseSetting(hero, skill, "horizontal-power", 1.1, false);
            double veticalPower = SkillConfigManager.getUseSetting(hero, skill, "vertical-power", 0.5, false);

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
                            long duration = SkillConfigManager.getUseSetting(hero, skill, "ncp-exemption-duration", 500, false);
                            if (duration > 0) {
                                NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(skill, targetPlayer, duration);
                                CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                                targetCT.addEffect(ncpExemptEffect);
                            }
                        }
                    }
                }

                target.setVelocity(new Vector(xDir, veticalPower, zDir));

                // Play effect
                try {
                    fplayer.playFirework(target.getWorld(), target.getLocation(), FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BURST).withColor(Color.ORANGE).withFade(Color.RED).build());
                }
                catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return;
        }
    }

    // Buff effect used to keep track of grappling hook uses
    public class ExplosiveShotBuffEffect extends ExpirableEffect {

        private int shotsLeft = 1;
        private boolean showExpireText = true;

        public ExplosiveShotBuffEffect(Skill skill, Player applier, long duration, int shotsLeft) {
            super(skill, "ExplosiveShotBuffEffect", applier, duration);

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

        public NCPExemptionEffect(Skill skill, Player applier, long duration) {
            super(skill, "NCPExemptionEffect_MOVING", applier, duration);
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