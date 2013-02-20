package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillFlyingKick extends TargettedSkill {

    private String useText;

    public SkillFlyingKick(Heroes plugin) {
        super(plugin, "FlyingKick");
        setDescription("Kicks your target upward");
        setUsage("/skill flyingkick <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill flyingkick");
        setTypes(SkillType.HARMFUL, SkillType.PHYSICAL, SkillType.DAMAGING);
    }

    public String getDescription(Hero hero) {
        StringBuilder descr = new StringBuilder(getDescription());

        double silenceSec = SkillConfigManager.getUseSetting(hero, this, "silence-duration", 3000, false) / 1000.0;
        if (silenceSec > 0) {
            descr.append(" and silences it for ");
            descr.append(Util.formatDouble(silenceSec));
            descr.append("s");
        }

        double cdSec = SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN, 15000, false) / 1000.0;
        if (cdSec > 0) {
            descr.append(" CD:");
            descr.append(Util.formatDouble(cdSec));
            descr.append("s");
        }

        int mana = SkillConfigManager.getUseSetting(hero, this, Setting.MANA, 20, false);
        if (mana > 0) {
            descr.append(" M:");
            descr.append(mana);
        }

        return descr.toString();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();
        defaultConfig.set(Setting.USE_TEXT.node(), "%player%: This is SPARTAAAA!!!");
        defaultConfig.set(Setting.COOLDOWN.node(), 15000);
        defaultConfig.set(Setting.MANA.node(), 20);
        defaultConfig.set(Setting.MAX_DISTANCE.node(), 5);
        defaultConfig.set(Setting.DAMAGE.node(), 1);
        defaultConfig.set("silence-duration", 3000);
        return defaultConfig;
    }

    @Override
    public void init() {
        super.init();
        useText = SkillConfigManager.getRaw(this, Setting.USE_TEXT, "%player%: This is HEROCRAFTT!!!").replace("%player%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        target.setVelocity(new Vector(Math.random() * 0.4 - 0.2, 0.8, Math.random() * 0.4 - 0.2));

        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 1, false);
        damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);

        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            Hero targetHero = plugin.getCharacterManager().getHero(targetPlayer);

            int silenceDuration = SkillConfigManager.getUseSetting(hero, this, "silence-duration", 3000, false);
            if (silenceDuration > 0) {
                targetHero.addEffect(new SilenceEffect(this, silenceDuration));
            }
        }
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.HURT_FLESH , 10.0F, 1.0F);
        broadcast(target.getLocation(), useText, hero.getName());
        return SkillResult.NORMAL;
    }
}