package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Properties;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import java.util.Set;

public class SkillBeastCall extends ActiveSkill  {

    private static final String weaponDamagePercentage = "weapon-damage-percentage";
    private static final String useExponentialDamage = "use-exponential-damage";

    public SkillBeastCall(Heroes plugin)
    {
        super(plugin, "BeastCall");
        setDescription("Summons a wolf at your side who will attack whatever you attack.");
        setArgumentRange(0, 0);
        setTypes(SkillType.SUMMONING);
        setIdentifiers("skill beastcall");
        setUsage("/skill beastcall");
    }

    public ConfigurationSection getDefaultConfig()
    {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.COOLDOWN.node(), 60000);
        config.set("health-modifier", 4);
        config.set("damage-modifier", 4);
        return config;
    }

    public SkillResult use(Hero hero, String[] strings)
    {
        Set<Monster> summons = hero.getSummons();
        if (!summons.isEmpty()) {
            for (Monster summon : summons)
            {
                com.herocraftonline.heroes.characters.effects.Effect effect = summon.getEffect("Summon");
                if ((effect != null) && (effect.getSkill().equals(this))) {
                    return SkillResult.FAIL;
                }
            }
        }
        Player player = hero.getPlayer();

        Wolf wolf = (Wolf)player.getWorld().spawnEntity(player.getLocation(), EntityType.WOLF);
        wolf.setAgeLock(true);
        Monster wolfMonster = Heroes.getInstance().getCharacterManager().getMonster(wolf);
        double maxHealth = hero.getPlayer().getMaxHealth() / SkillConfigManager.getSetting(hero.getHeroClass(), this, "health-modifier", 4);
        double damage = calculateDamage(hero) / SkillConfigManager.getSetting(hero.getHeroClass(), this, "damage-modifier", 4);
        wolfMonster.setMaxHealth(maxHealth);
        wolfMonster.setDamage(damage);

        wolf.getWorld().spigot().playEffect(wolf.getLocation(), org.bukkit.Effect.EXTINGUISH);
        hero.getPlayer().playSound(wolf.getLocation(), Sound.ENTITY_WOLF_GROWL, 1.0F, 1.0F);
        Messaging.send(hero.getPlayer(), "Summoned a wolf to fight at your side!");
        return SkillResult.NORMAL;
    }

    public String getDescription(Hero hero)
    {
        return "Summons a wolf at your side who will attack whatever you attack.";
    }

    private double calculateDamage(Hero hero)
    {
        Player player = hero.getPlayer();

        double baseDamage = 1.0D;
        try
        {
            Material weapon = Material.getMaterial(player.getItemInHand().getType().name());
            //baseDamage = this.plugin.getDamageManager().getItemDamage(weapon, player).doubleValue();
        }
        catch (Exception ignored) {}
        double weaponPercentage = SkillConfigManager.getUseSetting(hero, this, "weapon-damage-percentage", 1.0D, false);
        double damage = baseDamage * weaponPercentage;
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 40, false);

        boolean useExponent = SkillConfigManager.getUseSetting(hero, this, "use-exponential-damage", false);
        if (useExponent)
        {
            if (hero.getLevel() > 1) {
                damage += damageIncrease * Math.pow((hero.getLevel() + 20) / (Properties.maxLevel + 20), 2.0D);
            }
        }
        else {
            damage += damageIncrease * hero.getLevel();
        }
        return damage;
    }
}
