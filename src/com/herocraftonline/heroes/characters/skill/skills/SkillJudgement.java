package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.MovingParticle;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SkillJudgement extends ActiveSkill implements Listener
{
    private HashMap<JudgementEvent, Long> judgedEvents = new HashMap<>();

    public SkillJudgement(Heroes plugin)
    {
        super(plugin, "Judgement");
        setDescription("Applies divine judgement to targets within $1 meters. If they are an ally, they are healed based on how " +
                "much damage they've taken in the past $2 seconds, while " +
                "enemies are damaged based on how much damage they've dealt in the past $2 second(s). Does not affect yourself.");
        setUsage("/skill judgement");
        setIdentifiers("skill judgement", "skill judgment");
        setArgumentRange(0, 0);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startClearTimer();
    }

    public void startClearTimer() {
        new BukkitRunnable() {
            public void run() {
                HashMap<JudgementEvent, Long> newJudgedEvents = new HashMap<JudgementEvent, Long>();
                for (Map.Entry<JudgementEvent, Long> e : judgedEvents.entrySet()) {
                    if (e.getValue() > System.currentTimeMillis() - 10000){
                        newJudgedEvents.put(e.getKey(), e.getValue());
                    }
                }
                judgedEvents = newJudgedEvents;
            }
        }.runTaskTimer(plugin, 0, 200);
    }

    public String getDescription(Hero ap)
    {
        double radius = SkillConfigManager.getUseSetting(ap, this, SkillSetting.RADIUS, 6, true);
        long judgementPeriod = SkillConfigManager.getUseSetting(ap, this, "judgement-period-ms", 10000, true);

        return getDescription().replace("$1", radius + "")
                .replace("$2", String.valueOf((double) (judgementPeriod / 1000)));
    }

    public ConfigurationSection getDefaultConfig()
    {
        ConfigurationSection cs = super.getDefaultConfig();

        cs.set(SkillSetting.RADIUS.node(), 6);
        cs.set("judgement-period-ms", 10000);
        cs.set("damage-per-damage-dealt", 0.35);
        cs.set("healing-per-damage-taken", 0.35);

        return cs;
    }

    public void onWarmup(Hero ap)
    {
        final Player p = ap.getPlayer();
        double radius = SkillConfigManager.getUseSetting(ap, this, SkillSetting.RADIUS, 6, true);

        new BukkitRunnable() {
            int index = 0;
            public void run() {
                if (ap.getDelayedSkill() == null){
                    cancel();
                }

                List<Location> circle = GeometryUtil.circle(p.getLocation().add(0, 0.4, 0), 32, radius);
                for (int i = 0; i < 4; i++) {
                    Location l = circle.get(index);
                    //l.getWorld().spigot().playEffect(l, Effect.INSTANT_SPELL, 0, 0, 0.0F, 0.4F, 0.0F, 0.0F, 12, 128);
                    l.getWorld().spawnParticle(Particle.SPELL_INSTANT, l, 12, 0, 0.4, 0, 0, true);
                    index++;
                    if (index == circle.size()){
                        index = 0;
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public SkillResult use(final Hero ap, String[] args)
    {
        final Player p = ap.getPlayer();
        double radius = SkillConfigManager.getUseSetting(ap, this, SkillSetting.RADIUS, 6, true);
        long judgementPeriod = SkillConfigManager.getUseSetting(ap, this, "judgement-period-ms", 10000, true);
        double damagePerDamageDealt = SkillConfigManager.getUseSetting(ap, this, "damage-per-damage-dealt", 0.35, true);
        double healingPerDamageTaken = SkillConfigManager.getUseSetting(ap, this, "healing-per-damage-taken", 0.35, true);

        broadcastExecuteText(ap);

//        p.sendMessage("DEBUG: radius of nearby entities = " + radius);
//        p.sendMessage("DEBUG: period of judgement (ms) = " + judgementPeriod);

        for (Entity e : p.getNearbyEntities(radius, radius, radius))
        {
//            p.sendMessage("nearby entity = " + e.getName() + " " + (e instanceof LivingEntity ? "LE," : "")
//                + (e instanceof LivingEntity && e instanceof Player ? " P" : " Not P"));
            if (!(e instanceof LivingEntity)) continue;

            LivingEntity target = (LivingEntity) e;
            CharacterTemplate targAle = plugin.getCharacterManager().getCharacter(target);
            HashSet<JudgementEvent> relevantEvents = new HashSet<JudgementEvent>();
            boolean isAlly = (target instanceof Player && (ap.hasParty() && ap.getParty().getMembers().contains(plugin.getCharacterManager().getHero((Player) target))));
            HashMap<JudgementEvent, Long> newJudgedEvents = new HashMap<JudgementEvent, Long>(); // incidental cleanup
            for (Map.Entry<JudgementEvent, Long> entry : judgedEvents.entrySet()) {
                if (entry.getValue() >= System.currentTimeMillis() - judgementPeriod) {
                    if (entry.getKey().getDamager().equals(targAle) || entry.getKey().getDamaged().equals(targAle)){
                        relevantEvents.add(entry.getKey());
                    }
                }
                else {
                    newJudgedEvents.put(entry.getKey(), entry.getValue());
                }
            }
//            p.sendMessage("DEBUG: current time ms: " + System.currentTimeMillis());
//            p.sendMessage("DEBUG: judgedEvents: " + judgedEvents.size() + " events");
//            p.sendMessage("DEBUG: relevantEvents: " + relevantEvents.size() + " events");
//            p.sendMessage("DEBUG: new judgedEvents: " + newJudgedEvents.size() + " events");

//            if (!relevantEvents.isEmpty()) {
//                p.sendMessage("DEBUG: relevantEvents Info: (damager, damaged, damage) [damager id, damaged id]");
//                int i = 1;
//                for (JudgementEvent event : relevantEvents){
//                    p.sendMessage(i + ". " + (event.getDamager().getEntity() instanceof Player ? event.getDamager().getName() : event.getDamager().getEntity().getCustomName())
//                            + ","
//                            + (event.getDamaged().getEntity() instanceof Player ? event.getDamaged().getName() : event.getDamaged().getEntity().getCustomName())
//                            + "," + event.getDamage()
//                            + "[" + event.getDamager().getEntity().getEntityId() + "," + event.getDamaged().getEntity().getEntityId() + "]");
//                    i++;
//                }
//            }
//            if (!newJudgedEvents.isEmpty()) {
//                p.sendMessage("DEBUG: new judgedEvents Info: (damager, damaged, damage) (time ms) [damager id, damaged id]");
//                int i = 1;
//                for (Map.Entry<JudgementEvent, Long> entry : newJudgedEvents.entrySet()){
//                    JudgementEvent event = entry.getKey();
//                    p.sendMessage(i + ". " + (event.getDamager().getEntity() instanceof Player ? event.getDamager().getName() : event.getDamager().getEntity().getCustomName())
//                            + ","
//                            + (event.getDamaged().getEntity() instanceof Player ? event.getDamaged().getName() : event.getDamaged().getEntity().getCustomName())
//                            + "," + event.getDamage() + " (" + entry.getValue() + ")"
//                            + "[" + event.getDamager().getEntity().getEntityId() + "," + event.getDamaged().getEntity().getEntityId() + "]");
//                    i++;
//                }
//            }

            judgedEvents = newJudgedEvents;
            if (relevantEvents.isEmpty()) {
                if (target instanceof Player){
                    target.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You are not judged, for you have taken no action.");
                }
                continue;
            }
            double damagedByTarget = 0.0D;
            double damageToTarget = 0.0D;
            for (JudgementEvent event : relevantEvents)
            {
                if (targAle.equals(event.getDamager())){
                    damagedByTarget += event.getDamage();
                }
                else if (targAle.equals(event.getDamaged())){
                    damageToTarget += event.getDamage();
                }
            }

            if (isAlly){
                ((Hero) targAle).heal(damageToTarget * healingPerDamageTaken);
//                p.sendMessage("DEBUG: Healed " + ((Hero) targAle).getPlayer().getName()
//                        + " (" + (damageToTarget * healingPerDamageTaken) + " amount)");
            }
            else {
                addSpellTarget(target, ap);
                damageEntity(target, p, damagedByTarget * damagePerDamageDealt, EntityDamageEvent.DamageCause.MAGIC, false);
//                p.sendMessage("DEBUG: Damaged " + target.getName() + " (" + target.getCustomName() + ") by "
//                        + (damagedByTarget * damagePerDamageDealt) + " damage");
            }

            // FIXME Looks like it actually does get used
//            MovingParticle.createMovingParticle(target.getLocation().add(0, 2.5, 0), Effect.FIREWORKS_SPARK, 0, 0, 0.5F, 1.5F, 0.5F,
//                    0.0F, -0.1F, 0.0F, 0.0F, -0.2F, 0.0F, 125, 128, false, false);
            target.getWorld().strikeLightningEffect(target.getLocation());
            //target.getWorld().spigot().playEffect(target.getLocation().add(0, 1, 0), Effect.FIREWORKS_SPARK, 0, 0, 0.5F, 0.5F, 0.5F, 0.1F, 55, 128);
            target.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, target.getLocation().add(0, 1, 0), 55, 0.5, 0.5, 0.5, 0.1, true);
            //target.getWorld().spigot().playEffect(target.getLocation().add(0, 1, 0), (isAlly ? Effect.HAPPY_VILLAGER : Effect.PARTICLE_SMOKE), 0, 0, 0.5F, 0.5F, 0.5F, 0.0F, 55, 128);
            target.getWorld().spawnParticle((isAlly ? Particle.VILLAGER_HAPPY : Particle.VILLAGER_ANGRY), target.getLocation().add(0, 1, 0), 55, 0.5, 0.5, 0.5, 0, true);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0F, 2.0F);
            if (isAlly)
            {
                target.sendMessage("    " + ChatComponents.GENERIC_SKILL + "The divines heal a portion of your injuries.");
                new BukkitRunnable() {
                    int ticks = 0;
                    int maxTicks = 5;
                    Random rand = new Random();
                    public void run() {
                        if (ticks == maxTicks) cancel();
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3F, 0.1F + (0.2F * ticks));
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0, 1);
            }
            else {
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0F, 0.5F);
                if (target instanceof Player){
                    target.sendMessage("    " + ChatComponents.GENERIC_SKILL + "Divine judgement has been passed upon you!");
                }
            }
        }

        return SkillResult.NORMAL;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void registerJudgementEvents(EntityDamageByEntityEvent e)
    {
        if (!(e.getEntity() instanceof LivingEntity) || (!(e.getDamager() instanceof LivingEntity) && !(e.getDamager() instanceof Projectile))){
            return;
        }
        CharacterTemplate dmgerAle = null;
        if (e.getDamager() instanceof LivingEntity) {
            dmgerAle = plugin.getCharacterManager().getCharacter((LivingEntity) e.getDamager());
            // this next line is gross and full of parentheses - i'll have to do something about that. later. yeah.
            // jk
        } else if (e.getDamager() instanceof Projectile && ((Projectile) e.getDamager()).getShooter() instanceof LivingEntity) {
            dmgerAle = plugin.getCharacterManager().getCharacter((LivingEntity) (((Projectile) e.getDamager()).getShooter()));
        }
        if (dmgerAle == null) return;
        CharacterTemplate entAle = plugin.getCharacterManager().getCharacter(((LivingEntity) e.getEntity()));
        judgedEvents.put(new JudgementEvent(dmgerAle, entAle, e.getDamage()), System.currentTimeMillis());
    }

    private class JudgementEvent // this is when I wish Java had structs
    {
        private CharacterTemplate damager;
        private CharacterTemplate damaged;
        private double damage;

        public JudgementEvent(CharacterTemplate damager, CharacterTemplate damaged, double damage)
        {
            this.damager = damager;
            this.damaged = damaged;
            this.damage = damage;
        }

        public CharacterTemplate getDamager() { return damager; }
        public CharacterTemplate getDamaged() { return damaged; }
        public double getDamage() { return damage; }
    }
}
