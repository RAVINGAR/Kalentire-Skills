package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillEviscerate extends TargettedSkill {

    public SkillEviscerate(Heroes plugin) {
        super(plugin, "Eviscerate");
        setDescription("You eviscerate your target for $1 damage, this attack ignores armor.");
        setUsage("/skill eviscerate <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill eviscerate");
        setTypes(SkillType.PHYSICAL, SkillType.DAMAGING, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DAMAGE.node(), 25);
        node.set(Setting.DAMAGE_INCREASE.node(), 0.0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        Material item = player.getItemInHand().getType();
        if(!Util.swords.contains(item.name())) {
            Messaging.send(player, "You can't use eviscerate with that weapon!");
            return SkillResult.FAIL;
        }
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 25, false);
        damage += (int) (SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(this));
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.HURT_FLESH , 0.9F, 1.0F);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 25, false);
        damage += (int) (SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(this));
        return getDescription().replace("$1", damage + "");
    }
}
