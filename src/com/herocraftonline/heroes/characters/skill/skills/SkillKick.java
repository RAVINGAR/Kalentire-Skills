package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Setting;

public class SkillKick extends TargettedSkill {

    public SkillKick(Heroes plugin) {
        super(plugin, "Kick");
        setDescription("Damages your target for $1 damage, and making them unable to use skills for $2 seconds.");
        setUsage("/skill kick <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill kick");
        setTypes(SkillType.DEBUFF, SkillType.PHYSICAL, SkillType.HARMFUL, SkillType.INTERRUPT);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000);
        node.set(Setting.DAMAGE.node(), 4);
        node.set(Setting.EXPIRE_TEXT.node(), "%hero% is no longer silenced!");
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 4, false);
        addSpellTarget(target, hero);
        if (!damageEntity(target, hero.getPlayer(), damage, DamageCause.ENTITY_ATTACK)) {
            return SkillResult.INVALID_TARGET;
        }
        if (target instanceof Player) {
            SilenceEffect sEffect = new SilenceEffect(this, duration);
            plugin.getCharacterManager().getHero((Player) target).addEffect(sEffect);
        }
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.HURT_FLESH , 0.8F, 1.0F); 
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 4, false);
        return getDescription().replace("$1", damage + "").replace("$2", duration / 1000 + "");
    }
}
