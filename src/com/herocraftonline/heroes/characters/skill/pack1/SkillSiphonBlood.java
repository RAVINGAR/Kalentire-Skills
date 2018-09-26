package com.herocraftonline.heroes.characters.skill.pack1;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;

public class SkillSiphonBlood extends TargettedSkill {

    public SkillSiphonBlood(Heroes plugin) {
        super(plugin, "SiphonBlood");
        setDescription("Siphon blood from your target, dealing $1 dark damage and restoring your health for $2% of the damage dealt. Life stolen is increased by $3% per level of Blood Union. Increases Blood Union by $4.");
        setUsage("/skill siphonblood");
        setArgumentRange(0, 0);
        setIdentifiers("skill siphonblood");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_DARK);
    }

    @Override
    public String getDescription(Hero hero) {

        // Damage stuff
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        // Heal mult stuff
        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", 1.1, false);
        double healMultIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-heal-mult-increase", 0.04, false);

        int bloodUnionIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-increase", 1, false);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedHealMult = Util.decFormat.format(healMult * 100);
        String formattedHealMultIncrease = Util.decFormat.format(healMultIncrease * 100);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedHealMult).replace("$3", formattedHealMultIncrease).replace("$4", bloodUnionIncrease + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.1);
        node.set(SkillSetting.DAMAGE.node(), 80);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.0);
        node.set("heal-mult", 1.1);
        node.set("blood-union-heal-mult-increase", 0.06);
        node.set("blood-union-increase", 1);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        // Calculate damage
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 98, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", 1.1, false);

        // Get Blood Union Level
        int bloodUnionLevel = 0;
        if (hero.hasEffect("BloodUnionEffect")) {
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");

            bloodUnionLevel = buEffect.getBloodUnionLevel();
        }

        // Damage target
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        // Increase health multiplier by blood union level
        double healIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-heal-mult-increase", 0.05, false);
        healIncrease *= bloodUnionLevel;
        healMult += healIncrease;

        HeroRegainHealthEvent hrEvent = new HeroRegainHealthEvent(hero, damage * healMult, this, hero);

        plugin.getServer().getPluginManager().callEvent(hrEvent);
        if (!hrEvent.isCancelled()) {
            hero.heal(hrEvent.getDelta());
        }

        // Increase Blood Union
        if (hero.hasEffect("BloodUnionEffect")) {
            int bloodUnionIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-increase", 1, false);
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");

            if (target instanceof Player)
                buEffect.addBloodUnion(bloodUnionIncrease, true);
            else
                buEffect.addBloodUnion(bloodUnionIncrease, false);
        }
        
        //player.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.LAVADRIP, 0, 0, 0.3F, 0.3F, 0.3F, 0.1F, 50, 16);
        player.getWorld().spawnParticle(Particle.DRIP_LAVA, target.getEyeLocation(), 50, 0.3, 0.3, 0.3, 0.1);
        //player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.TILE_BREAK, Material.NETHER_STALK.getId(), 0, 0.3F, 0.3F, 0.3F, 0.1F, 50, 16);
        player.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation(), 50, 0.3, 0.3, 0.3, 0.1, Bukkit.createBlockData(Material.NETHER_WART_BLOCK));
        player.getWorld().playSound(target.getLocation(), CompatSound.ENTITY_GENERIC_DRINK.value(), 4.0F, 1);

        return SkillResult.NORMAL;
    }
}
