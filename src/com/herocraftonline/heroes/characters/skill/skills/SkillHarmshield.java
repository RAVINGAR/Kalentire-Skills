package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillHarmshield extends ActiveSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
	private String applyText;
    private String expireText;

    public SkillHarmshield(Heroes plugin) {
        super(plugin, "HarmShield");
        setDescription("Shields you from harm, reducing damage by $1% for $2 seconds");
        setUsage("/skill harmshield");
        setArgumentRange(0, 0);
        setIdentifiers("skill harmshield");
        setTypes(SkillType.BUFF, SkillType.SILENCABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("damage-multiplier", 0.1D);
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% is shielded from harm!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% lost his harm shield!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% is shielded from harm!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% lost his harm shield!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_SPAWN , 0.5F, 1.0F); 
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        hero.addEffect(new HarmShieldEffect(this, duration));
        // this is our fireworks shit
        Player player = hero.getPlayer();
        try {
            fplayer.playFirework(player.getWorld(), player.getLocation().add(0,1.5,0), 
            		FireworkEffect.builder().flicker(false).trail(false)
            		.with(FireworkEffect.Type.STAR)
            		.withColor(Color.MAROON)
            		.withFade(Color.YELLOW)
            		.build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }

    public class HarmShieldEffect extends ExpirableEffect {

        public HarmShieldEffect(Skill skill, long duration) {
            super(skill, "HarmShield", duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

    }

    public class SkillHeroListener implements Listener {

        private final Skill skill;
        
        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }
        
        @EventHandler(priority = EventPriority.HIGH)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
                return;
            }
            
            event.setDamage(getAdjustment((Player) event.getEntity(), event.getDamage()));
        }

        @EventHandler(priority = EventPriority.HIGH)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
                return;
            }
            
            event.setDamage(getAdjustment((Player) event.getEntity(), event.getDamage()));
        }

        private int getAdjustment(Player player, int damage) {
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect("HarmShield"))
                damage *= SkillConfigManager.getUseSetting(hero, skill, "damage-multiplier", 0.1D, false);
            return damage;
        }
    }
    
    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false) / 1000;
        float damageReduction = (float) SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 0.1D, false);
        damageReduction *= 100F;
        damageReduction = 100F - damageReduction;
        return getDescription().replace("$1", damageReduction + "").replace("$2", duration + "");
    }
    
}
