package com.herocraftonline.heroes.characters.skill.skills;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillIcebolt extends ActiveSkill {

    private Map<Snowball, Long> snowballs = new LinkedHashMap<Snowball, Long>(100) {
        private static final long serialVersionUID = 4632858378318784263L;
        @Override
        protected boolean removeEldestEntry(Entry<Snowball, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
        
    };

    private String applyText;
    private String expireText;
    
    public SkillIcebolt(Heroes plugin) {
        super(plugin, "Icebolt");
        setDescription("You launch a ball of ice that deals $1 damage to your target and slows them for $2 seconds.");
        setUsage("/skill icebolt");
        setArgumentRange(0, 0);
        setIdentifiers("skill icebolt");
        setTypes(SkillType.ICE, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DAMAGE.node(), 3);
        node.set("slow-duration", 5000); // 5 seconds
        node.set("speed-multiplier", 2);
        node.set(Setting.APPLY_TEXT.node(), "%target% has been slowed by %hero%!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% is no longer slowed!");
        return node;
        
    }
    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%target% has been slowed by %hero%!").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%target% is no longer slowed!").replace("%target%", "$1");
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.setShooter(player);
        snowballs.put(snowball, System.currentTimeMillis());
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {
        
        private final Skill skill;
        
        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }
        
        @EventHandler()
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }
            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            Entity projectile = subEvent.getDamager();
            if (!(projectile instanceof Snowball) || !snowballs.containsKey(projectile)) {
                return;
            }

            snowballs.remove(projectile);

            Entity dmger = ((Snowball) subEvent.getDamager()).getShooter();
            if (dmger instanceof Player) {
                Hero hero = plugin.getCharacterManager().getHero((Player) dmger);
                if (!damageCheck((Player) dmger, (LivingEntity) event.getEntity())) {
                    event.setCancelled(true);
                    return;
                }
                event.getEntity().setFireTicks(0);
                int damage = SkillConfigManager.getUseSetting(hero, skill, Setting.DAMAGE, 3, false);
                
                long duration = SkillConfigManager.getUseSetting(hero, skill, "slow-duration", 10000, false);
                int amplifier = SkillConfigManager.getUseSetting(hero, skill, "speed-multiplier", 2, false);
                
                SlowEffect iceSlowEffect = new SlowEffect(skill, duration, amplifier, false, applyText, expireText, hero);
                LivingEntity target = (LivingEntity) event.getEntity();
                plugin.getCharacterManager().getCharacter(target).addEffect(iceSlowEffect);
                addSpellTarget(event.getEntity(), hero);
                damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);
                event.setCancelled(true);
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 4, false);
        return getDescription().replace("$1", damage + "").replace("$2", duration / 1000 + "");
    }
}
