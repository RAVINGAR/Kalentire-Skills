package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Properties;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SkillMeteor
        extends ActiveSkill
{
    public static final String WEAPON_DAMAGE_MULTIPLIER = "weapon-damage-multiplier";
    public static final String USE_EXPONENTIAL_DAMAGE = "use-exponential-damage";
    public static final String METEOR_HEIGHT_INCREASE = "meteor-height-increase";
    public static final String DOWNWARD_VELOCITY = "downward-velocity";
    public static final String FIRE_TICKS = "fire-ticks";

    private Map<LargeFireball, Long> meteors = new LinkedHashMap<LargeFireball, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<LargeFireball, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000L <= System.currentTimeMillis());
        }
    };


    public SkillMeteor(Heroes plugin)
    {
        super(plugin, "Meteor");
        setDescription("You summon a meteor in the sky that, upon hitting a target, deals $1 damage and lights them on fire");
        setUsage("/skill meteor");
        setArgumentRange(0, 0);
        setIdentifiers("skill meteor");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.DAMAGING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    public String getDescription(Hero hero)
    {
        double damage = calculateDamage(hero);

        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage);
    }

    public ConfigurationSection getDefaultConfig()
    {
        ConfigurationSection node = super.getDefaultConfig();


        node.set(SkillSetting.MAX_DISTANCE.node(),(20));
        node.set(SkillSetting.COOLDOWN.node(),(15000));
        node.set(SkillSetting.USE_TEXT.node(), getDefaultUseText());


        node.set("weapon-damage-multiplier",(1.8D));
        node.set(SkillSetting.DAMAGE_INCREASE.node(),(40));
        node.set("use-exponential-damage",(true));
        node.set(SkillSetting.RADIUS.node(),(6));
        node.set("meteor-height-increase", (15));
        node.set("downward-velocity", (5.0D));
        node.set("fire-ticks", (50));

        return node;
    }

    public SkillResult use(Hero hero, String[] args)
    {
        Player player = hero.getPlayer();

        int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 12, false);

        Block tBlock = player.getTargetBlock((Set<Material>)null, maxDist);
        if (tBlock == null) {
            return SkillResult.INVALID_TARGET;
        }
        int meteorHeight = SkillConfigManager.getUseSetting(hero, this, "meteor-height-increase", 20, false);

        Location tBlockLoc = tBlock.getLocation();
        Location meteorSummonLoc = new Location(tBlockLoc.getWorld(), tBlockLoc.getX(), tBlockLoc.getY() + meteorHeight, tBlockLoc.getZ());

        broadcastExecuteText(hero);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_BLAST, 0.2F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERDRAGON_FIREBALL_EXPLODE, 0.6F, 2.0F);

        double yVelocity = SkillConfigManager.getUseSetting(hero, this, "downward-velocity", 0.5D, false);
        LargeFireball meteor = meteorSummonLoc.getWorld().spawn(meteorSummonLoc, LargeFireball.class);
        this.meteors.put(meteor, System.currentTimeMillis());

        Vector velocity = new Vector(0.0D, -yVelocity, 0.0D);

        meteor.setShooter(player);
        meteor.setDirection(new Vector(0, -1, 0));
        meteor.setVelocity(velocity);

        meteor.setBounce(false);
        meteor.setIsIncendiary(false);
        meteor.setFireTicks(0);
        meteor.setYield(0.0F);

        return SkillResult.NORMAL;
    }

    private double calculateDamage(Hero hero)
    {
        Player player = hero.getPlayer();

        double baseDamage = 1.0D;
        try
        {
            Material weapon = Material.getMaterial(player.getItemInHand().getType().name());
            //baseDamage = this.plugin.getDamageManager().getItemDamage(weapon, player).doubleValue();
        }
        catch (Exception e) {}
        double weaponPercentage = SkillConfigManager.getUseSetting(hero, this, "weapon-damage-multiplier", 1.8D, false);
        double damage = baseDamage * weaponPercentage;
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 40, false);

        boolean useExponent = SkillConfigManager.getUseSetting(hero, this, "use-exponential-damage", false);
        if (useExponent)
        {
            if (hero.getLevel() > 1) {
                damage += damageIncrease * Math.pow((hero.getLevel() + 20) / (Properties.maxLevel + 20), 2.0D);
            }
        }
        else {
            damage += damageIncrease * hero.getLevel();
        }
        return damage;
    }

    private String getSkillDenoter()
    {
        return ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] ";
    }

    private String getDefaultUseText()
    {
        return getSkillDenoter() + "%hero% used %skill%!";
    }

    public class SkillEntityListener
            implements Listener
    {
        private final Skill skill;

        public SkillEntityListener(Skill skill)
        {
            this.skill = skill;
        }

        @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
        public void onProjectileHit(ProjectileHitEvent event)
        {
            if (!(event.getEntity() instanceof LargeFireball)) {
                return;
            }
            LargeFireball projectile = (LargeFireball)event.getEntity();
            if (!(projectile.getShooter() instanceof Player)) {
                return;
            }
            if (!SkillMeteor.this.meteors.containsKey(projectile)) {
                return;
            }
            Player shooter = (Player)projectile.getShooter();
            Hero hero = SkillMeteor.this.plugin.getCharacterManager().getHero(shooter);

            int radius = SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.RADIUS, 4, false);
            double damage = SkillMeteor.this.calculateDamage(hero);
            int numFireTicks = SkillConfigManager.getUseSetting(hero, this.skill, "fire-ticks", 50, false);


            List<Entity> targets = projectile.getNearbyEntities(radius, radius, radius);
            for (Entity target : targets) {
                if (((target instanceof LivingEntity)) && (Skill.damageCheck(shooter, (LivingEntity)target)))
                {
                    LivingEntity targetLE = (LivingEntity)target;


                    targetLE.setFireTicks(numFireTicks);
                    SkillMeteor.this.plugin.getCharacterManager().getCharacter(targetLE).addEffect(new CombustEffect(this.skill, shooter));


                    SkillMeteor.this.addSpellTarget(targetLE, hero);
                    damageEntity(targetLE, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);
                }
            }
            SkillMeteor.this.meteors.remove(projectile);
        }

        @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
        public void onEntityDamage(EntityDamageByEntityEvent event)
        {
            Entity projectile = event.getDamager();
            if ((!(projectile instanceof LargeFireball)) || (!SkillMeteor.this.meteors.containsKey(projectile))) {
                return;
            }
            event.setCancelled(true);
        }
    }
}
