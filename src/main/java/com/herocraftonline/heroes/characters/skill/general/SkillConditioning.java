package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

public class SkillConditioning extends PassiveSkill implements Listenable {
    private final Listener listener;

    public SkillConditioning(Heroes plugin) {
        super(plugin, "Conditioning");
        setDescription("Passive $1% reduction of all physical damage.");
        setTypes(SkillType.MOVEMENT_INCREASE_COUNTERING, SkillType.BUFFING);
        listener = new ConditioningListener(this);
    }

    public String getDescription(Hero hero) {
        int level = hero.getHeroLevel(this);
        double amount = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.AMOUNT.node(), 0.25D, false) + SkillConfigManager.getUseSetting(hero, this, "amount-increase", 0.0D, false) * level) * 100.0D;
        amount = Math.max(amount, 0.0D);
        return getDescription().replace("$1", String.valueOf(amount));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.AMOUNT.node(), 0.2D);
        node.set("amount-increase", 0.0D);
        return node;
    }

    @NotNull
    @Override
    public Listener getListener() {
        return listener;
    }

    public static class ConditioningListener implements Listener {
        private final SkillConditioning skill;
        public ConditioningListener(SkillConditioning skill) {
            this.skill = skill;
        }
        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if ((!event.isCancelled() && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK && event.getEntity() instanceof Player && event.getDamage() >= 1.0D) || (!event.isCancelled() && event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE && event.getEntity() instanceof Player && event.getDamage() >= 1.0D) || (!event.isCancelled() && event.getCause() == EntityDamageEvent.DamageCause.THORNS && event.getEntity() instanceof Player && event.getDamage() >= 1.0D)) {
                Player player = (Player) event.getEntity();
                Hero hero = this.skill.plugin.getCharacterManager().getHero(player);
                if (hero.hasEffect("Conditioning")) {
                    double amount = SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.AMOUNT.node(), 0.25D, false) + SkillConfigManager.getUseSetting(hero, this.skill, "amount-increase", 0.0D, false) * hero.getHeroLevel(this.skill);
                    amount = Math.max(amount, 0.0D);
                    event.setDamage((int) (event.getDamage() * (1.0D - amount)));
                }
            }
        }
    }
}
