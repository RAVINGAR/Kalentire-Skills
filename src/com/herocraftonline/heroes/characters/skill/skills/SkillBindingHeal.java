package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillBindingHeal extends  TargettedSkill {

    public SkillBindingHeal(Heroes plugin) {
        super(plugin, "BindingHeal");
        setDescription("Heal target for $1 and then self for $2; if you have no target, then heal self for $1.");
        setUsage("/skill bindingheal");
        setArgumentRange(0, 0);
        setIdentifiers("skill bindingheal");
        setTypes(SkillType.HEALING, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 4);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.2);
        node.set(SkillSetting.MAX_DISTANCE.node(), 15);
        node.set(SkillSetting.MANA.node(), 10);
        node.set(SkillSetting.COOLDOWN.node(), 4000);
        node.set("particle-power", 0.5);
        node.set("particle-amount", 10);
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        String description = "";
        String ending = "§6; ";

        // Mana
        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA.node(), 0, false)
                - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE_PER_LEVEL.node(), 0, false) * hero.getLevel());
        if (mana > 0) {
            description += "§6Cost: §9" + mana + "MP" + ending;
        }

        // Health cost
        int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false) -
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, mana, true) * hero.getLevel());
        if (healthCost > 0 && mana > 0) {
            description += "§6" + healthCost + ending;
        } else if (healthCost > 0) {
            description += "§6Cost: §c" + healthCost + "HP" + ending;
        }

        // Cooldown
        int cooldown = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 0, false)
                - SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 0, false) * hero.getLevel()) / 1000;
        if (cooldown > 0) {
            description += "§6CD: §9" + cooldown + "s" + ending;
        }

        // Damage
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 4, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.2, false) * hero.getLevel();

        description += getDescription().replace("$1", "§9" + damage + "§6").replace("$2", "§9" + damage * 0.5 + "§6");

        return description;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 4, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.2, false) * hero.getLevel();
        float particlePower = (float) SkillConfigManager.getUseSetting(hero, this, "particle-power", 0.5, false);
        int particleAmount = SkillConfigManager.getUseSetting(hero, this, "particle-amount", 10, false);

        if (!(target instanceof Player)) { return SkillResult.INVALID_TARGET_NO_MSG; }

        if (!target.equals(player)) {
            // Heal target and self for half
            double curHealth = target.getHealth();
            double total;
            if (damage + curHealth < target.getMaxHealth()) {
                total = damage + curHealth;
                broadcast(target.getLocation(), ((Player) target).getName() + " healed for " + (float)(damage));
            } else {
                total = target.getMaxHealth();
                broadcast(target.getLocation(), ((Player) target).getName() + " healed for " + (float)(target.getMaxHealth() - curHealth));
            }
            target.setHealth(total);

            curHealth = player.getHealth();
            if ((damage * 0.5) + curHealth < player.getMaxHealth()) {
                total = (damage * 0.5) + curHealth;
                broadcast(player.getLocation(), player.getName() + " healed for " + (float)(damage));
            } else {
                total = player.getMaxHealth();
                broadcast(player.getLocation(), player.getName() + " healed for " + (float)(player.getMaxHealth() - curHealth));
            }
            player.setHealth(total);

            // Play effects
            target.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.HEART, 0, 0, 0, 0, 0, particlePower, particleAmount, 64);
            player.getWorld().spigot().playEffect(player.getEyeLocation(), Effect.HEART, 0, 0, 0, 0, 0, particlePower, particleAmount, 64);

            // Broadcast
            broadcastExecuteText(hero);

            return SkillResult.NORMAL;
        } else if (target.equals(player)) {
            // Heal self only
            double curHealth = player.getHealth();
            double total;
            if (damage + curHealth < player.getMaxHealth()) {
                total = damage + curHealth;
                broadcast(player.getLocation(), player.getName() + " healed for " + (float)(damage));
            } else {
                total = player.getMaxHealth();
                broadcast(player.getLocation(), player.getName() + " healed for " + (float)(player.getMaxHealth() - curHealth));
            }
            player.setHealth(total);

            // Play effect
            player.getWorld().spigot().playEffect(player.getEyeLocation(), Effect.HEART, 0, 0, 0, 0, 0, particlePower, particleAmount, 24);

            // Broadcast
            broadcastExecuteText(hero);

            return SkillResult.NORMAL;
        } else {
            return SkillResult.INVALID_TARGET;
        }
    }
}