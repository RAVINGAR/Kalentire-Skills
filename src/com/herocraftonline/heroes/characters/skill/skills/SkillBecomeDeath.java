package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillBecomeDeath extends ActiveSkill {
    
    private String applyText;
    private String expireText;
    
    public SkillBecomeDeath(Heroes plugin) {
        super(plugin, "BecomeDeath");
        setDescription("For $1 seconds you look undead and wont be targeted by undead. You also no longer need to breath air.");
        setUsage("/skill becomedeath");
        setArgumentRange(0, 0);
        setIdentifiers("skill becomedeath", "bdeath");
        setTypes(SkillType.SILENCABLE, SkillType.BUFF, SkillType.DARK);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 30000);
        node.set(Setting.APPLY_TEXT.node(), "%hero% gains the features of a zombie!");
        node.set(Setting.EXPIRE_TEXT.node(), "%hero% no longer appears as an undead!");
        return node;
    }
    
    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%hero% gains the features of a zombie!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%hero% no longer appears as an undead!").replace("%hero%", "$1");
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 30000, false);
        hero.addEffect(new UndeadEffect(this, duration));
        return SkillResult.NORMAL;
    }
    
    public class UndeadEffect extends ExpirableEffect {

        public UndeadEffect(Skill skill, long duration) {
            super(skill, "Undead", duration);
            addMobEffect(13, (int) (duration / 1000) * 20, 0, false);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.DARK);
            this.types.add(EffectType.WATER_BREATHING);
        }

        @Override
        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }
    
    public class SkillEntityListener implements Listener {


        @EventHandler(priority = EventPriority.HIGHEST)
        public void onEntityTarget(EntityTargetEvent event) {
            if (event.isCancelled() || !(event.getTarget() instanceof Player)) {
                return;
            }
            
            if (!isUndead(event.getEntity())) {
                return;
            }
            
            Hero hero = plugin.getHeroManager().getHero((Player) event.getTarget());
            if (hero.hasEffect("Undead")) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || event.getDamage() == 0) {
                return;
            }
            
            if (!isUndead(event.getEntity()) || !(event instanceof EntityDamageByEntityEvent)) {
                return;
            }
            
            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            if (subEvent.getDamager() instanceof Player) {
                Hero hero = plugin.getHeroManager().getHero((Player) subEvent.getDamager());
                if (hero.hasEffect("Undead"))
                    hero.removeEffect(hero.getEffect("Undead"));
            } else if (subEvent.getDamager() instanceof Projectile) {
                if (((Projectile) subEvent.getDamager()).getShooter() instanceof Player) {
                    Hero hero = plugin.getHeroManager().getHero((Player) ((Projectile) subEvent.getDamager()).getShooter());
                    if (hero.hasEffect("Undead"))
                        hero.removeEffect(hero.getEffect("Undead"));
                }
            }
        }
        
        private boolean isUndead(Entity entity) {
            return entity instanceof Zombie || entity instanceof Skeleton || entity instanceof Ghast;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 30000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
    
}
