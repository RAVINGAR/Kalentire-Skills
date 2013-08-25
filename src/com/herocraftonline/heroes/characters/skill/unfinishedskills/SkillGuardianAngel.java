package com.herocraftonline.heroes.characters.skill.unfinishedskills;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.InvulnerabilityEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillGuardianAngel extends ActiveSkill {

    public SkillGuardianAngel(Heroes plugin) {
        super(plugin, "GuardianAngel");
        setDescription("Gives you and your party within $1 blocks invulnerability for $2s");
        setUsage("/skill guardianangel");
        setArgumentRange(0, 0);
        setIdentifiers("skill guardianangel", "skill gangel");
        setTypes(SkillType.BUFF, SkillType.SILENCABLE, SkillType.COUNTER);
    }

    @Override
    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false) +
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE, 0.0, false) * hero.getSkillLevel(this));
        double duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 12000, false) +
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE, 0, false) * hero.getSkillLevel(this));
        String description = getDescription().replace("$1", radius + "").replace("$2", (int) (duration / 1000) + "");
        
        //COOLDOWN
        int cooldown = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false)
                - SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE, 0, false) * hero.getSkillLevel(this)) / 1000;
        if (cooldown > 0) {
            description += " CD:" + cooldown + "s";
        }
        
        //MANA
        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 10, false)
                - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE, 0, false) * hero.getSkillLevel(this));
        if (mana > 0) {
            description += " M:" + mana;
        }
        
        //HEALTH_COST
        int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false) - 
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST_REDUCE, mana, true) * hero.getSkillLevel(this));
        if (healthCost > 0) {
            description += " HP:" + healthCost;
        }
        
        //STAMINA
        int staminaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA, 0, false)
                - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA_REDUCE, 0, false) * hero.getLevel());
        if (staminaCost > 0) {
            description += " FP:" + staminaCost;
        }
        
        //DELAY
        int delay = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY, 0, false) / 1000;
        if (delay > 0) {
            description += " W:" + delay + "s";
        }
        
        //EXP
        int exp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXP, 0, false);
        if (exp > 0) {
            description += " XP:" + exp;
        }
        return description;
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.RADIUS.node(), 10);
        node.set(SkillSetting.RADIUS_INCREASE.node(), 0.0);
        node.set(SkillSetting.DURATION.node(), 12000); // in Milliseconds - 10 minutes
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0.0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 12000, false) +
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE, 0, false) * hero.getLevel());

        InvulnerabilityEffect iEffect = new InvulnerabilityEffect(this, duration);
        if (!hero.hasParty()) {
            hero.addEffect(iEffect);
        } else {
            int rangeSquared = (int) Math.pow(SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 10, false) +
                (SkillConfigManager.getUseSetting(hero, this, "radius-increase", 0.0, false) * hero.getLevel()), 2);
            for (Hero pHero : hero.getParty().getMembers()) {
                Player pPlayer = pHero.getPlayer();
                if (!pPlayer.getWorld().equals(player.getWorld()) || pPlayer.getLocation().distanceSquared(player.getLocation()) > rangeSquared) {
                    continue;
                }
                pHero.addEffect(iEffect);
            }
        }
        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_DEATH , 0.5F, 1.0F); 
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
}