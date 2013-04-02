package com.herocraftonline.heroes.characters.skill.skills;
//originial - http://pastie.org/private/diqqlssrsjp7fkn7hjayqg
import java.util.Iterator;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillHealingBloom extends ActiveSkill {

    public SkillHealingBloom(Heroes plugin) {
        super(plugin, "HealingBloom");
        setDescription("Blooms your party, healing them for $1$2 per $3s for $4s");
        setUsage("/skill healingbloom");
        setIdentifiers("skill healingbloom");
        setTypes(SkillType.SILENCABLE, SkillType.HEAL, SkillType.LIGHT);
        setArgumentRange(0,0);
    }

    public class RejuvinationEffect extends PeriodicExpirableEffect {
        int mode;
        double amountHealed;
        public RejuvinationEffect(Skill skill, Heroes plugin, long period, long duration, int mode, double amountHealed) {
            super(skill, plugin, "HealingBloom", period, duration);
            this.mode = mode;
            this.amountHealed = amountHealed;
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();
            int amount = (int) amountHealed;
            switch(mode) {
            case 1:
                amount = (int) (player.getMaxHealth() * amountHealed * 0.01);
                break;
            case 2:
                amount = (int) ((player.getMaxHealth() - player.getHealth()) * amountHealed * 0.01);
                break;
            default: 
                break;
            }
            
            HeroRegainHealthEvent event = new HeroRegainHealthEvent(hero, amount, skill);
            Bukkit.getPluginManager().callEvent(event);
            if(!event.isCancelled()) {
                hero.heal(event.getAmount());
            }
        }

        @Override
        public void tickMonster(Monster arg0) {
            // TODO Auto-generated method stub

        }		
    }
    @Override
    public SkillResult use(Hero hero, String[] args) {
        //Load Skill Mode
        boolean amount = SkillConfigManager.getUseSetting(hero, this, "AmountMode", true);
        boolean percentMax = SkillConfigManager.getUseSetting(hero, this, "PercentMaxHealthMode", true);
        boolean percentMissing = SkillConfigManager.getUseSetting(hero, this, "PercentMissingHealthMode", true);
        int mode = 0;
        if(percentMax) {
            mode = 1;
        }
        if(percentMissing) {
            mode = 2;
        }
        if((!(amount || percentMax || percentMissing)) || (amount && percentMax) || (amount && percentMissing) || (percentMax && percentMissing)) {
            mode = 0;
            Bukkit.getServer().getLogger().log(Level.SEVERE, "[SkillHealingBloom] Invalid mode selection, defaulting to amount mode");
        }
        //

        HeroParty hParty = hero.getParty();
        if(hParty == null) {
            hero.getPlayer().sendMessage("§7[§2Skill§7] You must be in a party to use §fHealingBloom!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        double amountHealed = SkillConfigManager.getUseSetting(hero, this, "amount", 5, false);
        double period = SkillConfigManager.getUseSetting(hero, this, "period", 1000, false);
        double duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 30000, false);
        broadcastExecuteText(hero);
        //this.broadcast(hero.getPlayer().getLocation(), hero.getName() + " used HealingBloom!");
        Vector v = hero.getPlayer().getLocation().toVector();
        Iterator<Hero> partyMembers = hParty.getMembers().iterator();
        int range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        while(partyMembers.hasNext()) {
            Hero targetHero = partyMembers.next();
            if (!targetHero.getPlayer().getWorld().equals(hero.getPlayer().getWorld())) {
                continue;
            }
            if(targetHero.getPlayer().getLocation().toVector().distanceSquared(v) < Math.pow(range, 2)); {
                targetHero.addEffect(new RejuvinationEffect(this, plugin, (long) period, (long)duration, mode, amountHealed));
            }
        }
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero h) {
        boolean amount = SkillConfigManager.getUseSetting(h, this, "AmountMode", true);
        boolean percentMax = SkillConfigManager.getUseSetting(h, this, "PercentMaxHealthMode", false);
        boolean percentMissing = SkillConfigManager.getUseSetting(h, this, "PercentMissingHealthMode", false);
        int mode = 0;
        if(percentMax) {
            mode = 1;
        }
        if(percentMissing) {
            mode = 2;
        }
        if((!(amount || percentMax || percentMissing)) || (amount && percentMax) || (amount && percentMissing) || (percentMax && percentMissing)) {
            mode = 0;
            Bukkit.getServer().getLogger().log(Level.SEVERE, "[SkillHealingBloom] Invalid mode selection, defaulting to amount mode");
        }
        String modeOut = "ERROR: Skill getDescription() failed!";
        switch(mode) {
        case 0:
            modeOut = " health";
            break;
        case 1:
            modeOut = "% of their maximum health";
            break;
        case 2:
            modeOut = "% of their missing health";
            break;
        }
        double amountHealed = SkillConfigManager.getUseSetting(h, this, "amount", 5, false);
        double period = SkillConfigManager.getUseSetting(h, this, "period", 1000, false)*0.001;
        double duration = SkillConfigManager.getUseSetting(h, this, SkillSetting.DURATION.node(), 30000, false)*0.001;

        return getDescription()
                .replace("$1",amountHealed + "")
                .replace("$2", modeOut)
                .replace("$3", period + "")
                .replace("$4", duration + "");
    }
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(30000));
        node.set("period", Integer.valueOf(1000));
        node.set("amount", Integer.valueOf(5));
        node.set("AmountMode", true);
        node.set("PercentMaxHealthMode", false);
        node.set("PercentMissingHealthMode", false);
        node.set("maxrange", Integer.valueOf(0));
        return node;
    }
}