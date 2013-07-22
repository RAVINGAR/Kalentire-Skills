package com.herocraftonline.heroes.characters.skill.skills;

import net.minecraft.server.v1_6_R2.EntityCreature;
import net.minecraft.server.v1_6_R2.EntityPlayer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftCreature;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

public class SkillSmoke extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillSmoke(Heroes plugin) {
        super(plugin, "Smoke");
        setDescription("Vanish in a cloud of smoke! You will not be visible to other players for the next $1 seconds. Taking damage or using abilities will cause you to reappear. If you attack an enemy while smoked, you strike a weak point, stunning the target for $2 seconds.");
        setUsage("/skill smoke");
        setArgumentRange(0, 0);
        setIdentifiers("skill smoke");
        setNotes("Note: Interacting with anything removes the effect.");
        setNotes("Note: Taking damage removes the effect.");
        setNotes("Note: Using skills removes the effect.");
        setTypes(SkillType.ILLUSION, SkillType.PHYSICAL, SkillType.BUFF, SkillType.COUNTER, SkillType.STEALTHY);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double duration = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4500, false) / 1000.0);
        double stunDuration = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, "stun-duration", 1000, false) / 1000.0);

        return getDescription().replace("$1", duration + "").replace("$2", stunDuration + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 20000);
        node.set("stun-duration", 1000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] Someone vanished in a cloud of smoke!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% has reappeared!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] Someone vanished in a cloud of smoke!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% has reappeared!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4500, false);
        Player player = hero.getPlayer();
        player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 4);
        hero.addEffect(new SmokeEffect(this, duration));

        // If any nearby monsters are targeting the player, force them to change their target.
        for (Entity entity : player.getNearbyEntities(50, 50, 50)) {
            if (!(entity instanceof CraftCreature))
                continue;

            EntityCreature notchMob = (EntityCreature) ((CraftCreature) entity).getHandle();
            if (notchMob.target == null)
                continue;

            EntityPlayer notchPlayer = (EntityPlayer) ((CraftPlayer) player).getHandle();
            if (notchMob.target.equals(notchPlayer))
                notchMob.setGoalTarget(null);
        }

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityTarget(EntityTargetEvent event) {
            if (!(event.getTarget() instanceof Player) || event.getTarget() == null) {
                return;
            }

            Player player = (Player) event.getTarget();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (!(hero.hasEffect("SmokeEffect")))
                return;

            event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillUse(SkillUseEvent event) {
            Hero hero = event.getHero();

            if (hero.hasEffect("SmokeEffect")) {
                if (!event.getSkill().getTypes().contains(SkillType.STEALTHY))
                    hero.removeEffect(hero.getEffect("SmokeEffect"));
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (!(event.getEntity() instanceof LivingEntity))
                return;

            if (!(event instanceof EntityDamageByEntityEvent))
                return;

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            if (subEvent.getCause() != DamageCause.ENTITY_ATTACK || !(subEvent.getDamager() instanceof Player))
                return;

            Hero attackingHero = plugin.getCharacterManager().getHero((Player) subEvent.getDamager());

            if (!attackingHero.hasEffect("SmokeStunBuffEffect"))
                return;

            // Remove the smoke effect
            attackingHero.removeEffect(attackingHero.getEffect("SmokeStunBuffEffect"));

            // Stun the target
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            long duration = SkillConfigManager.getUseSetting(attackingHero, skill, "stun-duration", 1000, false);

            targetCT.addEffect(new StunEffect(skill, duration));
        }
    }

    public class SmokeEffect extends ExpirableEffect {

        public SmokeEffect(Skill skill, long duration) {
            super(skill, "SmokeEffect", duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.INVIS);
            types.add(EffectType.UNTARGETABLE_NO_MSG);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();

            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (onlinePlayer.equals(player) || onlinePlayer.hasPermission("heroes.admin.seeinvis")) {
                    continue;
                }
                onlinePlayer.hidePlayer(player);
            }

            if (applyText != null && applyText.length() > 0)
                broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (onlinePlayer.equals(player)) {
                    continue;
                }
                onlinePlayer.showPlayer(player);
            }

            // Give them a grace period to stun a target player.
            hero.addEffect(new SmokeStunBuffEffect(skill, 500));

            if (expireText != null && expireText.length() > 0)
                broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

        public class SmokeStunBuffEffect extends ExpirableEffect {

            public SmokeStunBuffEffect(Skill skill, long duration) {
                super(skill, "SmokeStunBuffEffect", duration);

                types.add(EffectType.BENEFICIAL);
            }

            @Override
            public void applyToHero(Hero hero) {
                super.applyToHero(hero);
            }

            @Override
            public void removeFromHero(Hero hero) {
                super.removeFromHero(hero);
            }
        }
    }
}
