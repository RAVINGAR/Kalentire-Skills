package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillSiphonBlood extends TargettedSkill {

    public VisualEffect fplayer = new VisualEffect();		// Firework effect

    public SkillSiphonBlood(Heroes plugin) {
        super(plugin, "SiphonBlood");
        setDescription("Siphon blood from your target, dealing $1 dark damage and restoring your health for $2% of the damage dealt. Life stolen is increased by $3% per level of Blood Union. Increases Blood Union by $4");
        setUsage("/skill siphonblood");
        setArgumentRange(0, 0);
        setIdentifiers("skill siphonblood");
        setTypes(SkillType.DAMAGING, SkillType.SILENCABLE, SkillType.HARMFUL, SkillType.DARK);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 6);
        node.set(SkillSetting.DAMAGE.node(), 95);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0.25);
        node.set("heal-mult", 1.1);
        node.set("blood-union-heal-mult-increase", 0.04);
        node.set("blood-union-increase", 1);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {

        // Damage stuff
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100, false);
        damage = (int) (damage + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.25, false) * hero.getSkillLevel(this));

        // Heal mult stuff
        int healMult = (int) (SkillConfigManager.getUseSetting(hero, this, "heal-mult", 1.1, false) * 100);
        int healMultIncrease = (int) (SkillConfigManager.getUseSetting(hero, this, "blood-union-heal-mult-increase", 0.04, false) * 100);

        int bloodUnionIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-increase", 1, false);

        return getDescription().replace("$1", damage + "").replace("$2", healMult + "").replace("$3", healMultIncrease + "").replace("$4", bloodUnionIncrease + "");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        // Calculate damage
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 95, false);
        damage = damage + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.25D, false) * hero.getSkillLevel(this);

        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", 1.1, false);

        // Get Blood Union Level
        int bloodUnionLevel = 0;
        if (hero.hasEffect("BloodUnionEffect")) {
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");

            bloodUnionLevel = buEffect.getBloodUnionLevel();
        }

        // Damage target
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC);

        // Increase health multiplier by blood union level
        double healIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-heal-mult-increase", 0.05, false);
        healIncrease *= bloodUnionLevel;
        healMult += healIncrease;

        HeroRegainHealthEvent hrEvent = new HeroRegainHealthEvent(hero, damage * healMult, this, hero);

        plugin.getServer().getPluginManager().callEvent(hrEvent);
        if (!hrEvent.isCancelled()) {
            hero.heal(hrEvent.getAmount());
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

        // Play Effect
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation(), FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BURST).withColor(Color.GREEN).withFade(Color.PURPLE).build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
    }
}
