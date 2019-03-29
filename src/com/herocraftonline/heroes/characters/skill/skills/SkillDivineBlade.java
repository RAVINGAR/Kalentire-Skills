package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class SkillDivineBlade extends ActiveSkill implements Listener
{
    public static final String EFFECT_NAME = "Divine Blade";
    public static final String BLIND_EFFECT_NAME = "Divine Blindness";
    private HashMap<UUID, Long> hitTimes = new HashMap<>();
    public SkillDivineBlade(Heroes plugin)
    {
        super(plugin, "DivineBlade");
        setDescription("For $1 second(s), melee strikes restore $2 health to yourself and allies within $3 meters of yourself. Can only trigger every $4 second(s). If Radiance is active, "
                + "blinds targets for $5 second(s) on hit.");
        setUsage("/skill divineblade");
        setIdentifiers("skill divineblade", "skill dblade");
        setArgumentRange(0, 0);
        setTypes(SkillType.BUFFING, SkillType.HEALING);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public String getDescription(Hero ap)
    {
        long duration = SkillConfigManager.getUseSetting(ap, this, SkillSetting.DURATION, 8000, true);
        double healing = SkillConfigManager.getUseSetting(ap, this, SkillSetting.HEALING, 10, true) +
                (SkillConfigManager.getUseSetting(ap, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.3, true)
                        * ap.getAttributeValue(AttributeType.WISDOM));
        double radius = SkillConfigManager.getUseSetting(ap, this, SkillSetting.RADIUS, 6, true);
        long triggerInterval = SkillConfigManager.getUseSetting(ap, this, "trigger-interval", 500, true);
        long blindDuration = SkillConfigManager.getUseSetting(ap, this, "blind-duration", 2000, true);

        return getDescription().replace("$1", String.valueOf((double) duration / 1000))
                .replace("$2", healing + "")
                .replace("$3", radius + "")
                .replace("$4", String.valueOf((double) triggerInterval / 1000))
                .replace("$5", String.valueOf((double) blindDuration / 1000));
    }

    public ConfigurationSection getDefaultConfig()
    {
        ConfigurationSection cs = super.getDefaultConfig();

        cs.set(SkillSetting.DURATION.node(), 8000);
        cs.set(SkillSetting.RADIUS.node(), 6);
        cs.set(SkillSetting.HEALING.node(), 10);
        cs.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.3);
        cs.set("trigger-interval", 500);
        cs.set("blind-duration", 2000);

        return cs;
    }

    public SkillResult use(Hero ap, String[] args)
    {
        final Player player = ap.getPlayer();

        long duration = SkillConfigManager.getUseSetting(ap, this, SkillSetting.DURATION, 8000, true);
        DivineBladeStatus dbs = new DivineBladeStatus(this, player, duration);
        ap.addEffect(dbs);

        new BukkitRunnable()
        {
            Random rand = new Random();
            public void run()
            {
                if (!ap.hasEffect(EFFECT_NAME)) cancel();
                //player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.7, 0), Effect.INSTANT_SPELL, 0, 0, 0.3F, 0.3F, 0.3F, 0.0F, 3, 128);
                player.getWorld().spawnParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 0.7, 0), 3, 0.3, 0.3, 0.3, 0, true);
                //player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.7, 0), Effect.FIREWORKS_SPARK, 0, 0, 0.3F, 0.3F, 0.3F, 0.0F, 3, 128);
                player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation().add(0, 0.7, 0),3, 0.3, 0.3, 0.3, 0, true);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.01f, rand.nextFloat() + 1.0f);
            }
        }.runTaskTimer(plugin, 0, 1);


        return SkillResult.NORMAL;
    }

    @EventHandler
    public void healOnHit(WeaponDamageEvent e)
    {
        if (!(e.getDamager() instanceof Hero) || !(e.getEntity() instanceof LivingEntity)) return;
        Hero ap = (Hero) e.getDamager();
        LivingEntity ent = (LivingEntity) e.getEntity();
        if (!ap.hasEffect(EFFECT_NAME)) return;
        if (!ap.getHeroClass().isAllowedWeapon(ap.getPlayer().getInventory().getItemInMainHand().getType())) return;
        if (hitTimes.get(ap.getPlayer().getUniqueId()) != null && hitTimes.get(ap.getPlayer().getUniqueId()) > System.currentTimeMillis()) return;

        double radius = SkillConfigManager.getUseSetting(ap, this, SkillSetting.RADIUS, 6, true);
//        ap.getPlayer().getWorld().spigot().playEffect(ap.getPlayer().getLocation().add(0, 0.2, 0), Effect.INSTANT_SPELL, 0, 0,
//                2.0F, 0.1F, 2.0F, 0.5F, 75, 128);
//        ap.getPlayer().getWorld().spigot().playEffect(ap.getPlayer().getLocation().add(0, 0.2, 0), Effect.INSTANT_SPELL, 0, 0,
//                2.0F, 0.1F, 2.0F, 0.5F, 75, 128);
        ap.getPlayer().getWorld().spawnParticle(Particle.SPELL_INSTANT, ap.getPlayer().getLocation().add(0, 0.2, 0), 75, 2, 0.1, 2, 0.5, true);
        ap.getPlayer().getWorld().spawnParticle(Particle.SPELL_INSTANT, ap.getPlayer().getLocation().add(0, 0.2, 0), 75, 2, 0.1, 2, 0.5, true);
        ap.getPlayer().getWorld().playSound(ap.getPlayer().getLocation(), Sound.BLOCK_CHORUS_FLOWER_GROW, 5.0F, 2.0F);

        double healing = SkillConfigManager.getUseSetting(ap, this, SkillSetting.HEALING, 10, true) +
                (SkillConfigManager.getUseSetting(ap, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.3, true)
                        * ap.getAttributeValue(AttributeType.WISDOM));

        if (ap.hasParty())
            for (Hero ally : ap.getParty().getMembers()) {
                if (!ally.getPlayer().getWorld().equals(ap.getPlayer().getWorld())) continue;
                if (ally.getPlayer().getLocation().distance(ap.getPlayer().getLocation()) <= radius) ally.heal(healing);
            }
        else ap.heal(healing);


        long blindDuration = SkillConfigManager.getUseSetting(ap, this, "blind-duration", 2000, true);
        CharacterTemplate ale = plugin.getCharacterManager().getCharacter(ent);
        ale.addEffect(new DivineBlindnessStatus(this, ap.getPlayer(), blindDuration));

        long triggerInterval = SkillConfigManager.getUseSetting(ap, this, "trigger-interval", 500, true);
        hitTimes.put(ap.getPlayer().getUniqueId(), System.currentTimeMillis() + triggerInterval);
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent e)
    {
        hitTimes.remove(e.getPlayer().getUniqueId());
    }


    public class DivineBladeStatus extends ExpirableEffect
    {
        public DivineBladeStatus(Skill skill, Player applier, long duration)
        {
            super(skill, EFFECT_NAME, applier, duration);
        }

        public void applyToHero(Hero ap)
        {
            super.applyToHero(ap);
            final Player player = ap.getPlayer();
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0F, 0.5F);
            broadcast(ap.getPlayer().getLocation(), ChatComponents.GENERIC_SKILL + ap.getName() + "'s blade is infused with holy power!");
        }

        public void removeFromHero(Hero ap)
        {
            super.removeFromHero(ap);
            final Player player = ap.getPlayer();
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0F, 0.5F);
            broadcast(ap.getPlayer().getLocation(), ChatComponents.GENERIC_SKILL +  "Holy power fades from " + ap.getName() + "'s sword.");
        }
    }

    public class DivineBlindnessStatus extends ExpirableEffect
    {
        public DivineBlindnessStatus(Skill plugin, Player applier, long duration)
        {
            super(plugin, BLIND_EFFECT_NAME, applier, duration);
        }

        public void applyToHero(Hero ap)
        {
            super.applyToHero(ap);
            final Player player = ap.getPlayer();
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (int) getDuration() / 50, 4, true, false));
            broadcast(ap.getPlayer().getLocation(), ChatComponents.GENERIC_SKILL + ap.getName() + " is blinded!");
        }

        public void removeFromHero(Hero ap)
        {
            super.removeFromHero(ap);
            final Player player = ap.getPlayer();
            broadcast(ap.getPlayer().getLocation(), ChatComponents.GENERIC_SKILL + ap.getName() + " is no longer blinded.");
        }
    }
}
