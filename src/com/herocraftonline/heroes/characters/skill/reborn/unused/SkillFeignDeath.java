//package com.herocraftonline.heroes.characters.skill.reborn.necromancer;
//
//import com.herocraftonline.heroes.Heroes;
//import com.herocraftonline.heroes.api.SkillResult;
//import com.herocraftonline.heroes.characters.CustomNameManager;
//import com.herocraftonline.heroes.characters.Hero;
//import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
//import com.herocraftonline.heroes.characters.party.HeroParty;
//import com.herocraftonline.heroes.characters.skill.*;
//import com.herocraftonline.heroes.chat.ChatComponents;
//import com.herocraftonline.heroes.util.Util;
//import mc.alk.tracker.controllers.MessageController;
//import org.bukkit.Bukkit;
//import org.bukkit.ChatColor;
//import org.bukkit.Location;
//import org.bukkit.Material;
//import org.bukkit.configuration.ConfigurationSection;
//import org.bukkit.entity.Entity;
//import org.bukkit.entity.LivingEntity;
//import org.bukkit.entity.Player;
//import org.bukkit.entity.Skeleton;
//import org.bukkit.inventory.ItemStack;
//import org.bukkit.potion.PotionEffect;
//import org.bukkit.potion.PotionEffectType;
//
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//
//public class SkillFeignDeath extends ActiveSkill {
//    private String applyText;
//    private String expireText;
//    private boolean battleTrackerEnabled = false;
//
//    public SkillFeignDeath(Heroes plugin) {
//        super(plugin, "FeignDeath");
//        setDescription("You feign your death, displaying a deceptive message of death to nearby players. " +
//                "After feigning, you are invisible for $1 second(s). Moving will break the effect however.");
//        setUsage("/skill feigndeath");
//        setIdentifiers("skill feigndeath");
//        setArgumentRange(0, 0);
//        setTypes(SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.BLINDING, SkillType.SILENCEABLE, SkillType.STEALTHY, SkillType.BUFFING);
//
//        if (Bukkit.getServer().getPluginManager().getPlugin("BattleTracker") != null) {
//            battleTrackerEnabled = true;
//        }
//    }
//
//    public String getDescription(Hero hero) {
//        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
//
//        return getDescription()
//                .replace("$1", Util.decFormat.format(duration / 1000.0));
//    }
//
//    public ConfigurationSection getDefaultConfig() {
//        ConfigurationSection config = super.getDefaultConfig();
//        config.set(SkillSetting.DURATION.node(), 60000);
//        config.set("detection-range", 1.0);
//        config.set("max-move-distance", 1.0);
//        return config;
//    }
//
//    public void init() {
//        super.init();
//
//        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "You feign death!");
//        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "You appear to be living!");
//    }
//
//    public SkillResult use(Hero hero, String[] args) {
//        Player player = hero.getPlayer();
//
//        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
//
//        // Feign Death
//        hero.addEffect(new FeignDeathEffect(this, player, duration));
//
//        return SkillResult.NORMAL;
//    }
//
//    public class FeignMoveChecker implements Runnable {
//
//        private Map<Hero, Location> oldLocations = new HashMap<>();
//        private Skill skill;
//
//        FeignMoveChecker(Skill skill) {
//            this.skill = skill;
//        }
//
//        @Override
//        public void run() {
//            Iterator<Entry<Hero, Location>> heroes = oldLocations.entrySet().iterator();
//            while (heroes.hasNext()) {
//                Entry<Hero, Location> entry = heroes.next();
//                Hero hero = entry.getKey();
//                Location oldLoc = entry.getValue();
//                if (!hero.hasEffect("Invisible")) {
//                    heroes.remove();
//                    continue;
//                }
//
//                Location newLoc = hero.getPlayer().getLocation();
//                if (newLoc.distance(oldLoc) > SkillConfigManager.getUseSetting(hero, skill, "max-move-distance", 1.0, false)) {
//                    hero.removeEffect(hero.getEffect("Invisible"));
//                    heroes.remove();
//                    continue;
//                }
//                double detectRange = SkillConfigManager.getUseSetting(hero, skill, "detection-range", 1.0, false);
//                List<Entity> nearEntities = hero.getPlayer().getNearbyEntities(detectRange, detectRange, detectRange);
//                for (Entity entity : nearEntities) {
//                    if (entity instanceof Player) {
//                        if (hero.hasParty()) {
//                            Hero nearHero = plugin.getCharacterManager().getHero((Player) entity);
//                            HeroParty heroParty = hero.getParty();
//                            boolean isPartyMember = false;
//                            for (Hero partyMember : heroParty.getMembers()) {
//                                if (nearHero.equals(partyMember)) {
//                                    isPartyMember = true;
//                                    break;
//                                }
//                            }
//
//                            if (isPartyMember)
//                                return;
//                        }
//
//                        hero.removeEffect(hero.getEffect("Invisible"));
//                        heroes.remove();
//                        break;
//                    }
//                }
//            }
//        }
//
//        public void addHero(Hero hero) {
//            if (!hero.hasEffect("Invisible"))
//                return;
//
//            oldLocations.put(hero, hero.getPlayer().getLocation());
//        }
//    }
//
//    // Buff effect used to keep track of warmup time
//    public class FeignDeathEffect extends InvisibleEffect {
//
//        private boolean isPVE;
//
//        public FeignDeathEffect(Skill skill, Player applier, long duration) {
//            super(skill, applier, duration, applyText, expireText);
//
//            addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) ((duration / 1000) * 20), 0), false);             // Blind
//        }
//
//        @Override
//        public void applyToHero(Hero hero) {
//            super.applyToHero(hero);
//            Player player = hero.getPlayer();
//
//            String playerName = player.getName();
//
//            isPVE = true;
//            LivingEntity lastCombatTarget = hero.getCombatEffect().getLastCombatant();
//            if (lastCombatTarget == null) {
//
//                String deathMessage = "";
//
//                if (battleTrackerEnabled) {
//                    deathMessage = MessageController.getPvEMessage(false, null, playerName, null);
//                } else
//                    deathMessage = "[" + ChatColor.AQUA + "PVE" + ChatColor.DARK_GRAY + "]" + ChatColor.DARK_AQUA + playerName + ChatColor.DARK_GRAY + " choked on a pretzel.";
//
//                broadcast(player.getLocation(), deathMessage);
//            } else if (lastCombatTarget instanceof Player) {
//                String targetName = ((Player) lastCombatTarget).getName();
//
//                String deathMessage = "";
//
//                if (battleTrackerEnabled) {
//                    boolean melee = true;
//                    ItemStack lastEnemyHeldItem = ((Player) lastCombatTarget).getItemInHand();
//                    if (lastEnemyHeldItem.getType() == Material.BOW)
//                        melee = false;
//
//                    deathMessage = MessageController.getPvPMessage(melee, targetName, playerName, lastEnemyHeldItem);
//                } else
//                    deathMessage = "[" + ChatColor.GREEN + "PVP" + ChatColor.DARK_GRAY + "]" + ChatColor.DARK_AQUA + playerName + ChatColor.DARK_GRAY + " was dominated by " + ChatColor.BLUE + targetName + ChatColor.DARK_GRAY + "!";
//
//                isPVE = false;
//                broadcast(player.getLocation(), deathMessage);
//            } else {
//                String targetName = CustomNameManager.getName(lastCombatTarget);
//
//                String deathMessage = "";
//
//                if (battleTrackerEnabled) {
//                    boolean melee = true;
//                    if (lastCombatTarget instanceof Skeleton)
//                        melee = false;
//
//                    deathMessage = MessageController.getPvEMessage(melee, targetName, playerName, null);
//                } else
//                    deathMessage = "[" + ChatColor.AQUA + "PVE" + ChatColor.DARK_GRAY + "]" + ChatColor.DARK_AQUA + playerName + ChatColor.DARK_GRAY + " was dominated by " + ChatColor.BLUE + targetName + ChatColor.DARK_GRAY + "!";
//
//                broadcast(player.getLocation(), deathMessage);
//            }
//        }
//
//        @Override
//        public void removeFromHero(Hero hero) {
//            super.removeFromHero(hero);
//
//            final Player player = hero.getPlayer();
//
//            if (player.hasPotionEffect(PotionEffectType.POISON) || player.hasPotionEffect(PotionEffectType.WITHER)
//                    || player.hasPotionEffect(PotionEffectType.HARM)) {
//                // If they have a harmful effect present when removing the ability, delay effect removal by a bit.
//                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
//                    @Override
//                    public void run() {
//                        expire(player);
//                    }
//                }, 2L);
//            } else {
//                expire(player);
//            }
//        }
//
//        private void expire(Player player) {
//            String playerName = player.getName();
//
//            String feignDeathExpireText = "";
//            if (isPVE)
//                feignDeathExpireText = "[" + ChatColor.AQUA + "PVE" + ChatColor.DARK_GRAY + "]";
//            else
//                feignDeathExpireText = "[" + ChatColor.GREEN + "PVP" + ChatColor.DARK_GRAY + "]";
//
//            feignDeathExpireText += ChatColor.DARK_AQUA + playerName + ChatColor.DARK_GRAY + " was faking death!";
//
//            broadcast(player.getLocation(), feignDeathExpireText);
//        }
//    }
//}
