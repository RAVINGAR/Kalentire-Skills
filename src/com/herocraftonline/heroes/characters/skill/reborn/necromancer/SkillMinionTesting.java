package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SummonEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillMinionTesting extends ActiveSkill {

    private final String minionEffectName = "MinionTesting";
    private boolean disguiseApiLoaded;

    public SkillMinionTesting(Heroes plugin) {
        super(plugin, "MinionTesting");
        setDescription("Test skill for minion stuff. Don't add this to a live server pls.");
        setUsage("/skill miniontesting");
        setArgumentRange(0, 0);
        setIdentifiers("skill miniontesting");
        setTypes(SkillType.SUMMONING, SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_DISEASE, SkillType.SILENCEABLE);

        if (Bukkit.getServer().getPluginManager().getPlugin("LibsDisguises") != null) {
            disguiseApiLoaded = true;
        }
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("minion-attack-damage", 50.0);
        config.set("minion-on-hit-debuff-tick-damage", 15.0);
        config.set("minion-on-hit-debuff-period", 500);
        config.set("minion-on-hit-debuff-duration", 2000);
        config.set("minion-max-hp", 200.0);
        config.set("minion-speed-amplifier", 2);
        config.set("minion-duration", 10000);
        config.set("launch-velocity", 1.5);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double launchVelocity = SkillConfigManager.getUseSetting(hero, this, "launch-velocity", 1.5, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, "minion-duration", 10000, false);

        // Wolfs have the most reliable default AI for following and helping the player. We'll disguise it as something else later.
        //MythicMobs mm = MythicMobs.inst();
        Zombie minion = (Zombie) player.getWorld().spawnEntity(player.getEyeLocation(), EntityType.ZOMBIE);
//        minion.setOwner(player);
//        Insentient insentient = PathfinderGoalAPI.getAPI().getPathfindeGoalEntity(minion);
//        insentient.clearPathfinderGoals();
//        insentient.addPathfinderGoal(0, new Attack(insentient, player, 10, 1));
//        insentient.addPathfinderGoal(0, new PathfinderGoalFollowEntity(insentient, player, 10, 1));

        //ActiveMob mob = mm.getMobManager().spawnMob("MinionTesting", player.getEyeLocation());

        final Monster monster = plugin.getCharacterManager().getMonster(minion);
        monster.setExperience(0);
        monster.addEffect(new MinionTestingEffect(this, hero, duration));

        minion.setVelocity(player.getLocation().getDirection().normalize().multiply(launchVelocity));
        minion.setFallDistance(-7F);

        return SkillResult.NORMAL;
    }

//    public class CustomZombie extends EntityZombie {
//        public CustomZombie(org.bukkit.World world) {
//            super(((CraftWorld) world).getHandle());
//
//            Set goalB = (Set) getPrivateField("b", PathfinderGoalSelector.class, goalSelector);
//            goalB.clear();
//            Set goalC = (Set) getPrivateField("c", PathfinderGoalSelector.class, goalSelector);
//            goalC.clear();
//            Set targetB = (Set) getPrivateField("b", PathfinderGoalSelector.class, targetSelector);
//            targetB.clear();
//            Set targetC = (Set) getPrivateField("c", PathfinderGoalSelector.class, targetSelector);
//            targetC.clear();
//
//            this.goalSelector.a(0, new PathfinderGoalFloat(this));
//            this.goalSelector.a(2, new PathfinderGoalMeleeAttack(this, EntityHuman.class, 1.0D, false));
//            this.goalSelector.a(4, new PathfinderGoalMeleeAttack(this, EntityVillager.class, 1.0D, true));
//            this.goalSelector.a(5, new PathfinderGoalMoveTowardsRestriction(this, 1.0D));
//            this.goalSelector.a(6, new PathfinderGoalMoveThroughVillage(this, 1.0D, false));
//            this.goalSelector.a(7, new PathfinderGoalRandomStroll(this, 1.0D));
//            this.goalSelector.a(8, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
//            this.goalSelector.a(8, new PathfinderGoalRandomLookaround(this));
//            this.targetSelector.a(1, new PathfinderGoalHurtByTarget(this, true));
//            this.targetSelector.a(2, new PathfinderGoalNearestAttackableTarget(this, EntityHuman.class, 0, true));
//            this.targetSelector.a(2, new PathfinderGoalNearestAttackableTarget(this, EntityVillager.class, 0, false));
//        }
//
//        public Object getPrivateField(String fieldName, Class clazz, Object object) {
//            Field field;
//            Object o = null;
//
//            try {
//                field = clazz.getDeclaredField(fieldName);
//
//                field.setAccessible(true);
//
//                o = field.get(object);
//            } catch (NoSuchFieldException | IllegalAccessException e) {
//                e.printStackTrace();
//            }
//
//            return o;
//        }
//    }

    private class SkillListener implements Listener {
        private final Skill skill;

        SkillListener(Skill skill) {
            this.skill = skill;
        }

//        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
//        public void onEntityDamage(EntityDamageByEntityEvent event) {
//            if (event.getDamage() == 0 || !(event.getDamager() instanceof Wolf) || !(event.getEntity() instanceof LivingEntity))
//                return;
//
//            CharacterTemplate attackerCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getDamager());
//            if (!attackerCT.hasEffect(minionEffectName))
//                return;
//
//            Hero summoner = ((MinionTestingEffect) attackerCT.getEffect(minionEffectName)).getSummoner();
//
//            CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
//            double tickDamage = SkillConfigManager.getUseSetting(summoner, skill, "minion-on-hit-debuff-tick-damage", 15.0, false);
//            long period = SkillConfigManager.getUseSetting(summoner, skill, "minion-on-hit-debuff-period", 500, false);
//            long duration = SkillConfigManager.getUseSetting(summoner, skill, "minion-on-hit-debuff-duration", 2000, false);
//
//            defenderCT.addEffect(new NoxiousPoisonEffect(skill, summoner.getPlayer(), period, duration, tickDamage));
//        }
    }

    private class MinionTestingEffect extends SummonEffect {
        MinionTestingEffect(Skill skill, Hero summoner, long duration) {
            super(skill, minionEffectName, duration, summoner, null);

            types.add(EffectType.DISEASE);
            types.add(EffectType.WATER_BREATHING);

            int speedAmplifier = SkillConfigManager.getUseSetting(summoner, skill, "minion-speed-amplifier", 2, false);

            addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, (int) (duration / 50), 0));
            addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (duration / 50), speedAmplifier));
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            double maxHp = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-max-hp", 500.0, false);
            double hitDmg = SkillConfigManager.getUseSetting(getSummoner(), skill, "minion-attack-damage", 50.0, false);

            Wolf minion = (Wolf) monster.getEntity();
            minion.setMaxHealth(maxHp);
            minion.setHealth(maxHp);
            minion.setCustomName("");
            monster.setDamage(hitDmg);

//            if (disguiseApiLoaded) {
//                if (!DisguiseAPI.isDisguised(minion)) {
//                    DisguiseAPI.undisguiseToAll(minion);
//                }
//
//                MobDisguise disguise = new MobDisguise(DisguiseType.getType(EntityType.CAVE_SPIDER), true);
////                PlayerDisguise disguise = new PlayerDisguise(applier);
//                disguise.setKeepDisguiseOnPlayerDeath(true);
//                disguise.setEntity(minion);
//                disguise.setShowName(true);
//                disguise.setModifyBoundingBox(false);
//                disguise.setReplaceSounds(true);
//                LivingWatcher watcher = disguise.getWatcher();
//                watcher.setCustomName(ChatColor.DARK_GREEN + applier.getName() + "'s Minion");
////                watcher.setArmor(applier.getInventory().getArmorContents().clone());
//                disguise.startDisguise();
//            }
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            LivingEntity minion = monster.getEntity();

//            if (disguiseApiLoaded) {
//                if (DisguiseAPI.isDisguised(minion)) {
//                    Disguise disguise = DisguiseAPI.getDisguise(minion);
//                    disguise.stopDisguise();
//                    disguise.removeDisguise();
//                }
//            }

            minion.remove();
        }
    }
}
