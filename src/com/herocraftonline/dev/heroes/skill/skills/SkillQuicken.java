package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityListener;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.ExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillQuicken extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillQuicken(Heroes plugin) {
        super(plugin, "Quicken");
        setDescription("Provides a movement speed boost until you get hit");
        setUsage("/skill quicken");
        setArgumentRange(0, 0);
        setIdentifiers("skill quicken", "skill quick");
        setTypes(SkillType.BUFF, SkillType.MOVEMENT, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", 2);
        node.set(Setting.DURATION.node(), 300000);
        node.set(Setting.RADIUS.node(), 15);
        node.set("apply-text", "%hero% gained a burst of speed!");
        node.set("expire-text", "%hero% returned to normal speed!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%hero% gained a burst of speed!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%hero% returned to normal speed!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 300000, false);
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
        if (multiplier > 20) {
            multiplier = 20;
        }
        QuickenEffect qEffect = new QuickenEffect(this, duration, multiplier);
        if (!hero.hasParty()) {
            hero.addEffect(qEffect);
            return SkillResult.NORMAL;
        }
        Player player = hero.getPlayer();
        int radius = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 15, false);
        int rSquared = radius * radius;
        Location loc = player.getLocation();
        //Apply the effect to all party members
        for (Hero tHero : hero.getParty().getMembers()) {
            if (!tHero.getPlayer().getWorld().equals(player.getWorld()))
                continue;
            
            if (loc.distanceSquared(tHero.getPlayer().getLocation()) > rSquared)
                continue;
            
            tHero.addEffect(qEffect);
        }
        return SkillResult.NORMAL;
    }

    public class QuickenEffect extends ExpirableEffect {

        public QuickenEffect(Skill skill, long duration, int amplifier) {
            super(skill, "Quicken", duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            addMobEffect(1, (int) (duration / 1000) * 20, amplifier, false);
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
    
    public class SkillEntityListener extends EntityListener {

        @Override
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || event.getDamage() == 0 || !(event.getEntity() instanceof Player))
                return;
            
            Hero hero = plugin.getHeroManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("Quicken"))
                hero.removeEffect(hero.getEffect("Quicken"));
        }
        
    }
}