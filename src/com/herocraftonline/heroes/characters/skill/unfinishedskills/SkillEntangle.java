package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import net.minecraft.server.v1_6_R2.EntityLiving;
import net.minecraft.server.v1_6_R2.MobEffectList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillEntangle extends TargettedSkill {

    private String applyText;
    private String expireText;

    public VisualEffect fplayer = new VisualEffect();

    public SkillEntangle(Heroes plugin) {
        // Heroes stuff
        super(plugin, "Entangle");
        setDescription("Deals $1 damage and roots your target in place for $2 seconds. The effect breaks when the target takes damage.");
        setUsage("/skill entangle");
        setIdentifiers("skill entangle");
        setTypes(SkillType.HARMFUL, SkillType.DEBUFF, SkillType.SILENCABLE, SkillType.EARTH, SkillType.MOVEMENT);
        setArgumentRange(0, 0);

        // Start up the listener for root skill usage
        Bukkit.getServer().getPluginManager().registerEvents(new RootListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double duration = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false) / 1000.0);
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false);

        return getDescription().replace("$1", damage + "").replace("$2", duration + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 1);
        node.set(SkillSetting.PERIOD.node(), 100);
        node.set(SkillSetting.DURATION.node(), 4000);
        node.set(SkillSetting.USE_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% used %skill% on %target%!");
        node.set(SkillSetting.APPLY_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %target% has been rooted!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %target% has broken free from the root!");

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %target% has been rooted!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %target% has broken free from the root!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        //deal  damage
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false);
        damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC, false);

        // Broadcast use text
        broadcastExecuteText(hero, target);

        // Play Sound
        Player player = hero.getPlayer();
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ZOMBIE_WOODBREAK, 0.8F, 1.0F);

        // Play Effect
        try {
            this.fplayer.playFirework(player.getWorld(), target.getLocation().add(0.0D, 1.5D, 0.0D), FireworkEffect.builder().flicker(true).trail(false).with(FireworkEffect.Type.BURST).withColor(Color.OLIVE).build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 100, false);

        //EntangleEffect EntangleEffect = new EntangleEffect(this, hero.getPlayer(), duration);
        EntangleEffect EntangleEffect = new EntangleEffect(this, period, duration, hero.getPlayer());

        // Add root effect to the target
        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        targetCT.addEffect(EntangleEffect);

        return SkillResult.NORMAL;
    }

    private class RootListener implements Listener {

        //        private Skill skill;
        //
        //        public RootListener(Skill skill) {
        //            this.skill = skill;
        //        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || event.getDamage() == 0) {
                return;
            }

            if (!(event.getEntity() instanceof LivingEntity))
                return;

            final CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());

            if (defenderCT.hasEffect("Root"))
                defenderCT.removeEffect(defenderCT.getEffect("Root"));
        }

        // Below is my attempt at preventing sprinting. None of it worked as much as I would have hoped. Keeping it here for future attempts.

        //        @EventHandler(priority = EventPriority.MONITOR)
        //        public void onFoodLevelChangeEvent(FoodLevelChangeEvent event) {
        //
        //            if (!(event.getEntity() instanceof Player))
        //                return;
        //
        //            // We always set to 6 to disable sprinting. If we aren't at 6 we don't need to perform any further checks
        //            if (event.getFoodLevel() != 6)
        //                return;
        //
        //            Player player = (Player) event.getEntity();
        //            Hero hero = plugin.getCharacterManager().getHero(player);
        //            if (hero.hasEffect("Root")) {
        //                event.setCancelled(true);
        //                Messaging.send(player, "Cancelled a food change event.");
        //            }
        //        }

        //        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        //        public void onPlayerSprintToggle(PlayerToggleSprintEvent event) {
        //            if (!event.isSprinting())
        //                return;
        //
        //            Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
        //            if (hero.hasEffect("Root")) {
        //                final Player player = event.getPlayer();
        //
        //                // Don't allow an entangled player to sprint. If they are sprinting, turn it off.
        //                final int currentHunger = player.getFoodLevel();
        //                player.setFoodLevel(1);
        //                player.setSprinting(false);
        //
        //                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
        //                {
        //                    public void run()
        //                    {
        //                        player.setFoodLevel(currentHunger);
        //                    }
        //                }, 0L);
        //
        //            }
        //        }

        //        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        //        public void onPlayerSprintToggle(PlayerToggleSprintEvent event) {
        //            if (!event.isSprinting())
        //                return;
        //
        //            Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
        //            if (hero.hasEffect("Root")) {
        //                if (!(hero.getEffect("Root") instanceof EntangleEffect))
        //                    return;
        //
        //                Player player = event.getPlayer();
        //
        //                // Store some temp variables
        //                EntangleEffect oldEntangleEffect = (EntangleEffect) hero.getEffect("Root");
        //                Player applier = oldEntangleEffect.getApplier();
        //                long duration = oldEntangleEffect.getDuration();
        //
        //                // Damage them for sprinting
        //                double damage = SkillConfigManager.getUseSetting(hero, skill, "sprint-damage", 10, false);
        //                damageEntity((LivingEntity) player, applier, damage, EntityDamageEvent.DamageCause.MAGIC);
        //
        //                // Put the entangle effect back on them
        //                EntangleEffect newEntangleEffect = new EntangleEffect(skill, applier, duration);
        //                hero.addEffect(newEntangleEffect);
        //            }
        //        }
        //    }
    }

    public class EntangleEffect extends PeriodicExpirableEffect {

        private final Player applier;
        private Location loc;

        public EntangleEffect(Skill skill, int period, int duration, Player applier) {
            super(skill, "Root", period, duration);
            this.applier = applier;

            types.add(EffectType.ROOT);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.MAGIC);
            types.add(EffectType.DISPELLABLE);

            addMobEffect(2, (int) (duration / 1000) * 20, 127, false);      // Max slowness is 127
            addMobEffect(8, (int) (duration / 1000) * 20, 128, false);      // Max negative jump boost
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            broadcast(monster.getEntity().getLocation(), applyText, Messaging.getLivingEntityName(monster), applier.getDisplayName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);

            broadcast(monster.getEntity().getLocation(), expireText, Messaging.getLivingEntityName(monster), applier.getDisplayName());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            final Player player = hero.getPlayer();
            loc = hero.getPlayer().getLocation();

            // Don't allow an entangled player to sprint. If they are sprinting, turn it off.
            final int currentHunger = player.getFoodLevel();
            player.setFoodLevel(1);
            player.setSprinting(false);

            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
            {
                public void run()
                {
                    player.setFoodLevel(currentHunger);
                }
            }, 0L);

            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(final Hero hero) {

            Player player = hero.getPlayer();
            EntityLiving el = ((CraftLivingEntity) player).getHandle();
            
            if (el.hasEffect(MobEffectList.POISON) || el.hasEffect(MobEffectList.WITHER) || el.hasEffect(MobEffectList.HARM)) {
                // If they have a harmful effect present when removing the ability, delay effect removal by a bit.
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        EntangleEffect.super.removeFromHero(hero);
                    }
                }, (long) (0.2 * 20));
            }
            else
                super.removeFromHero(hero);

            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

        @Override
        public void tickHero(Hero hero) {
            final Location location = hero.getPlayer().getLocation();
            if ((location.getX() != loc.getX()) || (location.getZ() != loc.getZ())) {

                // If they have any velocity, we wish to remove it.
                Player player = hero.getPlayer();
                player.setVelocity(new Vector(0, 0, 0));

                // Retain the player's Y position and facing directions
                loc.setYaw(location.getYaw());
                loc.setPitch(location.getPitch());
                loc.setY(location.getY());

                // Teleport the Player back into place.
                player.teleport(loc);
            }
        }

        @Override
        public void tickMonster(Monster monster) {}

    }

    // Below is the effect used for a "normal" root that doesn't use teleportation as a base. Kept here for future attempts to tweak the skill.

    //    public class EntangleEffect extends ExpirableEffect {
    //
    //        private Player applier;
    //
    //        public EntangleEffect(Skill skill, Player applier, long duration) {
    //            super(skill, "Root", duration);
    //            this.setApplier(applier);
    //
    //            types.add(EffectType.ROOT);
    //            types.add(EffectType.HARMFUL);
    //            types.add(EffectType.MAGIC);
    //            types.add(EffectType.DISPELLABLE);
    //
    //            addMobEffect(2, (int) (duration / 1000) * 20, 127, false);      // Max slowness
    //            addMobEffect(8, (int) (duration / 1000) * 20, 128, false);      // Max negative jump boost
    //        }
    //
    //        @Override
    //        public void applyToMonster(Monster monster) {
    //            super.applyToMonster(monster);
    //            broadcast(monster.getEntity().getLocation(), applyText, Messaging.getLivingEntityName(monster));
    //        }
    //
    //        @Override
    //        public void applyToHero(Hero hero) {
    //            super.applyToHero(hero);
    //            final Player player = hero.getPlayer();
    //
    //            // Don't allow an entangled player to sprint. If they are sprinting, turn it off.
    //            if (player.isSprinting()) {
    //                final int currentHunger = player.getFoodLevel();
    //                player.setFoodLevel(1);
    //                player.setSprinting(false);
    //
    //                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
    //                {
    //                    public void run()
    //                    {
    //                        player.setFoodLevel(currentHunger);
    //                    }
    //                }, 0L);
    //            }
    //
    //            broadcast(player.getLocation(), applyText, player.getDisplayName());
    //        }
    //
    //        @Override
    //        public void removeFromHero(Hero hero) {
    //            super.removeFromHero(hero);
    //            final Player player = hero.getPlayer();
    //            broadcast(player.getLocation(), expireText, player.getDisplayName());
    //        }
    //
    //        @Override
    //        public void removeFromMonster(Monster monster) {
    //            super.removeFromMonster(monster);
    //            broadcast(monster.getEntity().getLocation(), expireText, Messaging.getLivingEntityName(monster));
    //        }
    //
    //        public Player getApplier() {
    //            return applier;
    //        }
    //
    //        public void setApplier(Player applier) {
    //            this.applier = applier;
    //        }
}