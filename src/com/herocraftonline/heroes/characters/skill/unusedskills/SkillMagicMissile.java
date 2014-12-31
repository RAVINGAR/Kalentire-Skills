package com.herocraftonline.heroes.characters.skill.unusedskills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

public class SkillMagicMissile extends ActiveSkill {

    public SkillMagicMissile(Heroes plugin) {
        super(plugin, "MagicMissile");
        setDescription("Fire a missile that deals $1 damage.");
        setUsage("/skill magicmissile");
        setArgumentRange(0, 0);
        setIdentifiers("skill magicmissile");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.DAMAGING);

        // Register event
        Bukkit.getServer().getPluginManager().registerEvents(new SkillMagicMissileListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 3);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.2);
        node.set(SkillSetting.MANA.node(), 5);
        node.set(SkillSetting.COOLDOWN.node(), 250);
        node.set("velocity", 4);
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



        // Damage
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 2, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.2, false) * hero.getLevel();

        description += getDescription().replace("$1", "§9" + damage + "§6");

        return description;
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        double velocity = SkillConfigManager.getUseSetting(hero, this, "velocity", 4, false);
        Vector velocityVector = hero.getPlayer().getEyeLocation().getDirection().multiply(velocity);
        hero.getPlayer().launchProjectile(Arrow.class).setVelocity(velocityVector);
        return SkillResult.NORMAL;
    }

    public class SkillMagicMissileListener implements Listener {

        @EventHandler
        public void onWeaponDamage(WeaponDamageEvent event) {
            if(event.isCancelled()
                    || !event.getAttackerEntity().getType().equals(EntityType.ARROW)
                    || !(event.getDamager().getEntity() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getDamager().getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);

            double damage = SkillConfigManager.getUseSetting(hero, SkillMagicMissile.this, SkillSetting.DAMAGE.node(), 3, false)
                    + SkillConfigManager.getUseSetting(hero, SkillMagicMissile.this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.2, false) * player.getLevel();
            event.setDamage(damage);
        }

        @EventHandler
        public void onProjectileHit(ProjectileHitEvent event) {
            if (!(event.getEntity().getShooter() instanceof Player) || !(event.getEntity() instanceof Arrow)) return;

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity().getShooter());

            if (!hero.hasAccessToSkill(SkillMagicMissile.this)) return;

            event.getEntity().remove();
        }
    }
}