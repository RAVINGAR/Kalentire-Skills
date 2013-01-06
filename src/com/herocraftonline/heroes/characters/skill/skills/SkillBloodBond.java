package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillBloodBond extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillBloodBond(Heroes plugin) {
        super(plugin, "BloodBond");
        setDescription("Toggle-able passive: heals your party with $1 of your magic damage dealt each attack.");
        setUsage("/skill bloodbond");
        setArgumentRange(0, 0);
        setIdentifiers("skill bloodbond");
        setTypes(SkillType.BUFF, SkillType.SILENCABLE, SkillType.HEAL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int percent = (int) (SkillConfigManager.getUseSetting(hero, this, "percent", 0.05, false) +
                (SkillConfigManager.getUseSetting(hero, this, "percent-increase", 0.0, false) * hero.getSkillLevel(this))) * 100;
        percent = percent > 0 ? percent : 0;
        int radius = (int) (SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS.node(), 15.0, false) +
                (SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS_INCREASE.node(), 0.0, false) * hero.getSkillLevel(this)));
        radius = radius > 1 ? radius : 1;
        int mana = (int) (SkillConfigManager.getUseSetting(hero, this, "mana-tick", 10.0, false) -
                (SkillConfigManager.getUseSetting(hero, this, "mana-tick-decrease", 0.0, false) * hero.getSkillLevel(this)));
        mana = mana > 0 ? mana : 0;
        String description = getDescription().replace("$1", percent + "%") + "R:" + radius;
        if (mana > 0) {
            description += " M:" + mana;
        }
        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("on-text", "%hero% %skill%s with his party!");
        node.set("off-text", "%hero% breaks the %skill%!");
        node.set(Setting.PERIOD.node(), 3000);
        node.set("percent", 0.05);
        node.set("percent-increase", 0.0);
        node.set(Setting.RADIUS.node(), 15.0);
        node.set(Setting.RADIUS_INCREASE.node(), 0.0);
        node.set("mana-tick", 10.0);
        node.set("mana-tick-decrease", 0.0);
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, "on-text", "%hero% %skill%s with his party!").replace("%hero%", "$1").replace("%skill%", "$2");
        expireText = SkillConfigManager.getRaw(this, "off-text", "%hero% breaks the %skill%!").replace("%hero%", "$1").replace("%skill%", "$2");
    }

    @Override
    public SkillResult use(Hero hero, String args[]) {
        if (hero.hasEffect("BloodBond")) {
            hero.removeEffect(hero.getEffect("BloodBond"));
        } else {
            long period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD.node(), 3000, false);
            int mana = (int) (SkillConfigManager.getUseSetting(hero, this, "mana-tick", 10.0, false) -
                    (SkillConfigManager.getUseSetting(hero, this, "mana-tick-decrease", 0.0, false) * hero.getSkillLevel(this)));
            mana = mana > 0 ? mana : 0;
            hero.addEffect(new BloodBondEffect(this, period, mana));
        }
        return SkillResult.NORMAL;
    }

    public class BloodBondEffect extends PeriodicEffect {

        private int mana;
        private boolean firstTime = true;

        public BloodBondEffect(SkillBloodBond skill, long period, int mana) {
            super(skill, "BloodBond", period);
            this.mana = mana;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.HEAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            firstTime = true;
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName(), "BloodBond");
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName(), "BloodBond");
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
            if (mana > 0 && !firstTime) {
                if (hero.getMana() - mana < 0) {
                    hero.setMana(0);
                } else {
                    hero.setMana(hero.getMana() - mana);
                }
            } else if (firstTime) {
                firstTime = false;
            }
            if (hero.getMana() < mana) {
                hero.removeEffect(this);
            }
        }
    }

    public class SkillEntityListener implements Listener {
        private final Skill skill;
        public SkillEntityListener(Skill skill) {
                this.skill = skill;
        }

        @EventHandler
        public void onEntityDamageByEntity(EntityDamageByEntityEvent event){
            if((!(event.isCancelled()))&&(event.getCause().equals(DamageCause.MAGIC))&&(event.getDamager() instanceof Player)){
                Hero hero = plugin.getCharacterManager().getHero((Player) event.getDamager());
                if(hero.hasEffect("BloodBond")&&(hero.hasParty())){
                    int percent = (int) (SkillConfigManager.getUseSetting(hero, this.skill, "percent", 0.05, false) +
                            (SkillConfigManager.getUseSetting(hero, this.skill, "percent-increase", 0.0, false) * hero.getSkillLevel(this.skill)));
                    percent = percent > 0 ? percent : 0;
                    int radius = (int) (SkillConfigManager.getUseSetting(hero, this.skill, Setting.RADIUS.node(), 15.0, false) +
                            (SkillConfigManager.getUseSetting(hero, this.skill, Setting.RADIUS_INCREASE.node(), 0.0, false) * hero.getSkillLevel(this.skill)));
                    radius = radius > 1 ? radius : 1;
                    int amount = (int) (event.getDamage()*percent);
                    for(Hero member : hero.getParty().getMembers()){
                        if(member.getPlayer().getLocation().distance(hero.getPlayer().getLocation()) <= radius){
                            if(member.getHealth() + amount < member.getMaxHealth()){
                                member.setHealth(member.getHealth()+amount);
                            }else{
                                member.setHealth(member.getMaxHealth());
                            }
                            member.syncHealth();
                        }
                    }
                }
            }
        }

        @EventHandler
        public void onSkillDamage(SkillDamageEvent event){
            if((!(event.isCancelled()))&&(event.getDamager() instanceof Player)){
                Hero hero = plugin.getCharacterManager().getHero((Player) event.getDamager());
                if(hero.hasEffect("BloodBond")&&(hero.hasParty())){
                    int percent = (int) (SkillConfigManager.getUseSetting(hero, this.skill, "percent", 0.05, false) +
                            (SkillConfigManager.getUseSetting(hero, this.skill, "percent-increase", 0.0, false) * hero.getSkillLevel(this.skill)));
                    percent = percent > 0 ? percent : 0;
                    int radius = (int) (SkillConfigManager.getUseSetting(hero, this.skill, Setting.RADIUS.node(), 15.0, false) +
                            (SkillConfigManager.getUseSetting(hero, this.skill, Setting.RADIUS_INCREASE.node(), 0.0, false) * hero.getSkillLevel(this.skill)));
                    radius = radius > 1 ? radius : 1;
                    int amount = (int) (event.getDamage()*percent);
                    for(Hero member : hero.getParty().getMembers()){
                        if(member.getPlayer().getLocation().distance(hero.getPlayer().getLocation()) <= radius){
                            if(member.getHealth() + amount < member.getMaxHealth()){
                                member.setHealth(member.getHealth()+amount);
                            }else{
                                member.setHealth(member.getMaxHealth());
                            }
                            member.syncHealth();
                        }
                    }
                }
            }
        }
    }
}