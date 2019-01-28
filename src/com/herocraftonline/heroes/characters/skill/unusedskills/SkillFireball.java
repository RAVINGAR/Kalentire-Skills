//package com.herocraftonline.heroes.characters.skill.unusedskills;
//
//import com.herocraftonline.heroes.Heroes;
//import com.herocraftonline.heroes.api.SkillResult;
//import com.herocraftonline.heroes.characters.Hero;
//import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
//import com.herocraftonline.heroes.characters.skill.*;
//import net.minecraft.server.v1_12_R1.*;
//import org.bukkit.Location;
//import org.bukkit.configuration.ConfigurationSection;
//import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
//import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
//import org.bukkit.entity.Entity;
//import org.bukkit.entity.*;
//import org.bukkit.event.entity.EntityCombustByEntityEvent;
//import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
//import org.bukkit.util.Vector;
//
//import java.text.DecimalFormat;
//import java.util.LinkedList;
//import java.util.List;
//
//public class SkillFireball extends ActiveSkill {
//
//    public SkillFireball(Heroes plugin) {
//        super(plugin, "Fireball");
//        setDescription("Shoot a fireball that deals $1 damage and lights target on fire. Drake: shoot fireballs in a cone that deal $2 damage");
//        setUsage("/skill fireball");
//        setArgumentRange(0, 0);
//        setIdentifiers("skill fireball");
//        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.DAMAGING);
//        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
//            @Override
//            public void run() {
//               // EntityUtil.registerCustomEntity(SkillLargeFireball.class, "SkillFireballLarge", 12, false);
//               // EntityUtil.registerCustomEntity(SkillSmallFireball.class, "SkillFireballSmall", 13, false);
//            }
//        });
//
//    }
//
//    @Override
//    public ConfigurationSection getDefaultConfig() {
//        final ConfigurationSection node = super.getDefaultConfig();
//        node.set(SkillSetting.DAMAGE.node(), 4);
//        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0.0);
//        node.set("velocity-multiplier", 1.0);
//        node.set("fire-ticks", 100);
//        return node;
//    }
//
//    @Override
//    public SkillResult use(Hero hero, String[] args) {
//        final SmallFireball fireball = shootFireball(hero);
//        final double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 1.0, false);
//        fireball.setVelocity(fireball.getVelocity().multiply(mult));
//        broadcastExecuteText(hero);
//        return SkillResult.NORMAL;
//    }
//
//    @Override
//    public String getDescription(Hero hero) {
//        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false);
//        damage += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getHeroLevel(this));
//        double drakeDamage = SkillConfigManager.getUseSetting(hero, this, "drake-damage", 4, false);
//        drakeDamage += (SkillConfigManager.getUseSetting(hero, this, "drake-damage-boost", 0.0, false) * hero.getHeroLevel(this));
//        DecimalFormat dF = new DecimalFormat("#0.##");
//        return getDescription().replace("$1", dF.format(damage)).replace("$2", dF.format(drakeDamage));
//    }
//
//    public SmallFireball shootFireball(Hero hero) {
//        Player p = hero.getPlayer();
//        Location spawnLoc = p.getEyeLocation();
//        CraftWorld cWorld = (CraftWorld) spawnLoc.getWorld();
//        Vector direction = spawnLoc.getDirection().multiply(10);
//        SkillSmallFireball fireball = new SkillSmallFireball(cWorld.getHandle(), ((CraftPlayer)p).getHandle(), direction.getX(), direction.getY(), direction.getZ(), hero, this);
//        fireball.projectileSource = p;
//        fireball.setPositionRotation(spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ(), spawnLoc.getYaw(), spawnLoc.getPitch());
//        cWorld.getHandle().addEntity(fireball);
//        return (SmallFireball) fireball.getBukkitEntity();
//    }
//
//    public List<LargeFireball> shootFireballDrake(Hero hero) {
//        Player p = hero.getPlayer();
//        Location spawnLoc = p.getEyeLocation();
//        CraftWorld cWorld = (CraftWorld) spawnLoc.getWorld();
//        LinkedList<LargeFireball> fireballs = new LinkedList<LargeFireball>();
//        for (int i = -1; i <= 1; i++) {
//            Location toSpawn = spawnLoc.clone();
//            toSpawn.setYaw(toSpawn.getYaw() + i * 30);
//            Vector direction = toSpawn.getDirection().multiply(10);
//            SkillLargeFireball fireball = new SkillLargeFireball(cWorld.getHandle(), ((CraftPlayer)p).getHandle(), direction.getX(), direction.getY(), direction.getZ(), hero, this);
//            fireball.projectileSource = p;
//            fireball.setPositionRotation(toSpawn.getX(), toSpawn.getY(), toSpawn.getZ(), toSpawn.getYaw(), toSpawn.getPitch());
//            cWorld.getHandle().addEntity(fireball);
//            fireballs.add((LargeFireball)fireball.getBukkitEntity());
//        }
//        return fireballs;
//    }
//
//    public static class SkillSmallFireball extends EntitySmallFireball {
//
//        private Hero hero;
//        private Skill skill;
//
//        //Load due to unload/saved entity
//        public SkillSmallFireball(World world) {
//            super(world);
//            this.die();
//        }
//
//        public SkillSmallFireball(World world, EntityPlayer player, double x, double y, double z, Hero hero, Skill skill) {
//            super(world,player,x,y,z);
//            this.hero = hero;
//            this.skill = skill;
//        }
//
//        @Override
//        public void setDirection(double d0, double d1, double d2) {
//            // CraftBukkit end
//            double d3 = (double) MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
//
//            this.dirX = d0 / d3 * 0.1D;
//            this.dirY = d1 / d3 * 0.1D;
//            this.dirZ = d2 / d3 * 0.1D;
//        }
//
//        protected void a(MovingObjectPosition movingobjectposition) {
//            try {
//                if (!this.world.isClientSide) {
//                    if (movingobjectposition.entity != null) {
//                        Entity target = movingobjectposition.entity.getBukkitEntity();
//                        if (!(target instanceof LivingEntity)) {
//                            return;
//                        }
//                        if (Skill.damageCheck(hero.getPlayer(), (LivingEntity) target)) {
//                            int fireLength = (int) Math.ceil(SkillConfigManager.getUseSetting(hero, skill, "fire-ticks", 100, false)/20);
//                            EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(hero.getPlayer(), target, fireLength);
//                            Heroes.getInstance().getServer().getPluginManager().callEvent(event);
//                            if (!event.isCancelled()) {
//                                target.setFireTicks(event.getDuration() * 20);
//                                Heroes.getInstance().getCharacterManager().getCharacter((LivingEntity) target).addEffect(new CombustEffect(skill, (Player) hero.getPlayer()));
//                                skill.addSpellTarget(target, hero);
//                                double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 4, false);
//                                damage += (SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getHeroLevel(skill));
//                                skill.damageEntity((LivingEntity) target, hero.getPlayer(), damage, DamageCause.MAGIC, false);
//                            }
//                        }
//                    }
//
//                    this.die();
//                }
//            } catch (Exception e) {
//                System.out.println(e.getMessage());
//                e.printStackTrace();
//                this.die();
//            }
//        }
//    }
//
//    public static class SkillLargeFireball extends EntityLargeFireball {
//
//        private Hero hero;
//        private Skill skill;
//
//        public SkillLargeFireball(World world) {
//            super(world);
//            this.die();
//        }
//
//        public SkillLargeFireball(World world, EntityPlayer player, double x, double y, double z, Hero hero, Skill skill) {
//            super(world,player,x,y,z);
//            this.hero = hero;
//            this.skill = skill;
//        }
//
//        public boolean damageEntity(DamageSource damagesource, float f) {
//            return false; //Invulnerable, prevent reflection
//        }
//
//        @Override
//        public void setDirection(double d0, double d1, double d2) {
//            // CraftBukkit end
//            double d3 = (double) MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
//
//            this.dirX = d0 / d3 * 0.1D;
//            this.dirY = d1 / d3 * 0.1D;
//            this.dirZ = d2 / d3 * 0.1D;
//        }
//
//        public void a(MovingObjectPosition movingobjectposition) {
//            try {
//                if (!this.world.isClientSide) {
//                    if (movingobjectposition.entity != null) {
//                        Entity target = movingobjectposition.entity.getBukkitEntity();
//                        if (!(target instanceof LivingEntity)) {
//                            return;
//                        }
//                        if (Skill.damageCheck(hero.getPlayer(), (LivingEntity) target)) {
//                            int fireLength = (int) Math.ceil(SkillConfigManager.getUseSetting(hero, skill, "fire-ticks", 100, false)/20);
//                            EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(hero.getPlayer(), target, fireLength);
//                            Heroes.getInstance().getServer().getPluginManager().callEvent(event);
//                            if (!event.isCancelled()) {
//                                target.setFireTicks(event.getDuration() * 20);
//                                Heroes.getInstance().getCharacterManager().getCharacter((LivingEntity) target).addEffect(new CombustEffect(skill, (Player) hero.getPlayer()));
//                                skill.addSpellTarget(target, hero);
//                                double damage = SkillConfigManager.getUseSetting(hero, skill, "drake-damage", 4, false);
//                                damage += (SkillConfigManager.getUseSetting(hero, skill, "drake-damage-boost", 0.0, false) * hero.getHeroLevel(skill));
//                                skill.damageEntity((LivingEntity) target, hero.getPlayer(), damage, DamageCause.MAGIC, false);
//                            }
//                        }
//                    }
//
//                    this.die();
//                }
//            } catch (Exception e) {
//                System.out.println(e.getMessage());
//                e.printStackTrace();
//                this.die();
//            }
//        }
//    }
//}