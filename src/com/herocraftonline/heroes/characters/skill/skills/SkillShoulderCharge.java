package com.herocraftonline.heroes.characters.skill.skills;

        import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
        import com.herocraftonline.heroes.characters.skill.*;
        import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

public class SkillShoulderCharge extends TargettedSkill {

    public SkillShoulderCharge(Heroes plugin) {
        super(plugin, "ShoulderCharge");
        setDescription("Charge your opponent, damaging for $1 and knocking them back.");
        setUsage("/skill shouldercharge");
        setArgumentRange(0, 0);
        setIdentifiers("skill shouldercharge");
        setTypes(SkillType.MOVEMENT_INCREASING, SkillType.FORCE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 1);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_AGILITY.node(), 0.1);
        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.MANA.node(), 10);
        node.set(SkillSetting.COOLDOWN.node(), 12000);
        node.set("particle-power", 0.5);
        node.set("particle-amount", 100);
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
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_AGILITY.node(), 0.1, false) * hero.getLevel();

        description += getDescription()
                .replace("$1", "§9" + damage + "§6");

        return description;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 1, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_AGILITY.node(), 0.1, false) * hero.getLevel();


        // Do not damage players in creative
        if (target instanceof Player) {
            if (((Player) target).getGameMode() == GameMode.CREATIVE)
                return SkillResult.INVALID_TARGET;
        }

        // Check if you can damage target
        if (Skill.damageCheck(hero.getPlayer(), target)) {
            broadcastExecuteText(hero, target);
            addSpellTarget(target, hero);

            Skill.damageEntity(target, hero.getEntity(), damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);
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