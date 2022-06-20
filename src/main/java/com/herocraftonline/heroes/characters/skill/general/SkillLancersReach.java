package com.herocraftonline.heroes.characters.skill.general;


// I'm pretty sure this skill doesn't work right, and it probably isn't very good to run it on the server due to the interact listener.
// Back to the drawing board with this one, but I'll keep the code just in case.


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;

public class SkillLancersReach extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillLancersReach(Heroes plugin) {
        super(plugin, "LancersReach");
        setDescription("Utilize the full capabilities of your lance, allowing you to attack enemies up to $1 blocks away for $2 second(s).");
        setUsage("/skill lancersreach");
        setArgumentRange(0, 0);
        setIdentifiers("skill lancersreach");
        setTypes(SkillType.BUFFING, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.UNBINDABLE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int reach = SkillConfigManager.getUseSetting(hero, this, "reach-distance", 7, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", reach + "").replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 10000);
        node.set("reach-distance", 7);
        node.set("attack cooldown-duration", 500);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is reaching!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer reaching.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is reaching!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer reaching.").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        int reach = SkillConfigManager.getUseSetting(hero, this, "reach-distance", 7, false);
        hero.addEffect(new LancersReachEffect(this, player, duration, reach));

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            CharacterTemplate attackerCT = event.getDamager();
            if (attackerCT.hasEffect("ReachingLance")) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerInteract(PlayerInteractEvent event) {

            // Make sure the player is right clicking.
            if (!(event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK))
                return;

            if (!event.hasItem())
                return;

            Player player = event.getPlayer();
            Hero hero = plugin.getCharacterManager().getHero(player);

            if (!hero.hasEffect("ReachingLance") || hero.hasEffect("ReachingLanceCooldownEffect"))
                return;

            ItemStack activatedItem = event.getItem();
            Material itemType = activatedItem.getType();
            if (!SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.swords).contains(itemType.name())) {
                return;
            }

            if (player.getLocation().getBlockY() > player.getLocation().getWorld().getMaxHeight()) {
                return;
            }

            LancersReachEffect reachEffect = (LancersReachEffect) hero.getEffect("ReachingLance");
            int reachDistance = reachEffect.getReachDistance();

            List<Block> lineOfSight;
            try {
                lineOfSight = player.getLineOfSight(Util.transparentBlocks, reachDistance);
            }
            catch (final IllegalStateException e) {
                return;
            }

            LivingEntity finalTarget = null;

            final Set<Location> locs = new HashSet<Location>();
            for (final Block block : lineOfSight) {
                locs.add(block.getRelative(BlockFace.UP).getLocation());
                locs.add(block.getLocation());
                locs.add(block.getRelative(BlockFace.DOWN).getLocation());
            }
            lineOfSight = null;
            final List<Entity> nearbyEntities = player.getNearbyEntities(reachDistance, reachDistance, reachDistance);
            for (final Entity entity : nearbyEntities) {
                if ((entity instanceof LivingEntity) && damageCheck(player, (LivingEntity) entity)) {
                    if (locs.contains(entity.getLocation().getBlock().getLocation())) {
                        if ((entity instanceof Player) && !player.canSee((Player) entity)) {
                            continue;
                        }
                        finalTarget = (LivingEntity) entity;
                        break;
                    }
                }
            }

            if (finalTarget != null) {
                // Damage target
                double damage = plugin.getDamageManager().getHighestItemDamage(hero, itemType);

                addSpellTarget(finalTarget, hero);
                damageEntity(finalTarget, player, damage, DamageCause.ENTITY_ATTACK);

                int duration = SkillConfigManager.getUseSetting(hero, skill, "attack cooldown-duration", 500, false);
                hero.addEffect(new LancersReachCooldownEffect(skill, player, duration));
            }
        }
    }

    public class LancersReachEffect extends ExpirableEffect {

        private int reachDistance;

        public LancersReachEffect(Skill skill, Player applier, long duration, int reachDistance) {
            super(skill, "ReachingLance", applier, duration, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.PHYSICAL);

            this.setReachDistance(reachDistance);
        }

        public int getReachDistance() {
            return reachDistance;
        }

        public void setReachDistance(int reachDistance) {
            this.reachDistance = reachDistance;
        }
    }

    public class LancersReachCooldownEffect extends ExpirableEffect {

        public LancersReachCooldownEffect(Skill skill, Player applier, long duration) {
            super(skill, "ReachingLanceCooldownEffect", applier, duration, null, null);
        }
    }
}
