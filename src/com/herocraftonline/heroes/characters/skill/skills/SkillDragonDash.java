package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.CompatSound;

public class SkillDragonDash extends TargettedSkill {

    public SkillDragonDash(Heroes plugin) {
        super(plugin, "DragonDash");
        setDescription("Charge your opponent, damaging for $1 and knocking them back.");
        setUsage("/skill dragondash");
        setArgumentRange(0, 0);
        setIdentifiers("skill dragondash");
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.MOVEMENT_INCREASING, SkillType.FORCE);
        //setTypes(SkillType.MOVEMENT_INCREASING, SkillType.FORCE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 1);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY.node(), 0.1);
        node.set(SkillSetting.MAX_DISTANCE.node(), 8);
        node.set(SkillSetting.STAMINA.node(), 300);
        node.set("particle-power", 0.5);
        node.set("particle-amount", 25);
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
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 1, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY.node(), 0.1, false) * hero.getLevel();

        description += getDescription()
                .replace("$1", "§9" + damage + "§6");

        return description;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 1, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY.node(), 0.1, false) * hero.getLevel();

        Player player = hero.getPlayer(); // this may be needed for the player call - if not remove this.

        /* Do not damage players in creative
        if (target instanceof Player) {
            if (((Player) target).getGameMode() == GameMode.CREATIVE)
                return SkillResult.INVALID_TARGET;
        }*/

        // Check if you can damage target
        if (damageCheck(hero.getPlayer(), target)) {
            broadcastExecuteText(hero, target);
            addSpellTarget(target, hero);

            damageEntity(target, hero.getEntity(), damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);
        } else {
            return SkillResult.INVALID_TARGET;
        }

        // Move caster to target location
        Location chargeLocation = target.getLocation();

        double d1 = hero.getPlayer().getLocation().getX() - chargeLocation.getX();
        double d2 = hero.getPlayer().getLocation().getZ() - chargeLocation.getZ();

        hero.getPlayer().setVelocity(new Vector(-d1, 0.2D, -d2));

        // Knock back target
        hero.getPlayer().getLocation().getDirection().normalize();
        d1 = hero.getPlayer().getLocation().getDirection().normalize().getX() * 2;
        d2 = hero.getPlayer().getLocation().getDirection().normalize().getZ() * 2;

        target.setVelocity(new Vector(d1, 0.2D, d2));

        // Create particle effect at target
        playEffect(hero, target);

        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.CLOUD, 0, 0, 0, 0, 0, 1, 150, 16);
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_ENDERDRAGON_GROWL.value(), 1.0F, 1.2F);
        return SkillResult.NORMAL;
    }

    public void playEffect(Hero hero, LivingEntity target) {
        float particlePower = (float) SkillConfigManager.getUseSetting(hero, this, "particle-power", 0.5, false);
        int particleAmount = SkillConfigManager.getUseSetting(hero, this, "particle-amount", 100, false);
        Location loc = target.getLocation();
        loc.setY(loc.getY() + 0.5);

        hero.getPlayer().getWorld().spigot().playEffect(loc, Effect.CLOUD, 0, 0, 0, 0, 0, particlePower, particleAmount, 64);
    }
}