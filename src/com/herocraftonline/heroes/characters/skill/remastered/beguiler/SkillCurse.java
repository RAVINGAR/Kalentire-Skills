package com.herocraftonline.heroes.characters.skill.remastered.beguiler;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillCurse extends PassiveSkill {
    private String missText;

    public SkillCurse(Heroes plugin) {
        super(plugin, "Curse");
        setDescription("Enemies have a $1% chance for their melee attacks to miss the you completely.");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.AGGRESSIVE, SkillType.DEBUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEventListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("miss-chance", 0.20);
        config.set("miss-text", ChatComponents.GENERIC_SKILL + "%target% misses an attack!");
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        return config;
    }

    @Override
    public String getDescription(Hero hero) {
        double chance = SkillConfigManager.getScaledUseSettingDouble(hero, this, "miss-chance", false);
        return getDescription().replace("$1", Util.decFormat.format(chance * 100.0));
    }

    @Override
    public void init() {
        super.init();
        missText = SkillConfigManager.getRaw(this, "miss-text", ChatComponents.GENERIC_SKILL + "%target% misses an attack!");
    }

    @Override
    public void apply(Hero hero) {
        // Note we don't want the default passive effect, we're making our own with a custom constructor
        double missChance = SkillConfigManager.getScaledUseSettingDouble(hero, this, "miss-chance", false);
        hero.addEffect(new CurseEffect(this, hero.getPlayer(), missChance));
    }

    public class CurseEffect extends PassiveEffect {
        private final double missChance;

        protected CurseEffect(Skill skill, Player applier, double missChance) {
            super(skill, applier, new EffectType[] {EffectType.DARK, EffectType.MAGIC});
            this.missChance = missChance;
        }

        public double getMissChance() {
            return missChance;
        }
    }

    public class SkillEventListener implements Listener {
        private final PassiveSkill skill;

        public SkillEventListener(PassiveSkill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.isCancelled() || event.getDamage() == 0)
                return;

            final Entity defenderEntity = event.getEntity();
            if (!(defenderEntity instanceof Player))
                return;

            CharacterTemplate attacker = event.getDamager();
            final LivingEntity attackerEntity = attacker.getEntity();

            final Hero defender = plugin.getCharacterManager().getHero((Player) defenderEntity);
            if (skill.hasPassive(defender)) {
                CurseEffect cEffect = (CurseEffect) defender.getEffect(skill.getName());
                if (cEffect != null && Util.nextRand() < cEffect.getMissChance()) {
                    event.setCancelled(true);
                    //attackerEntity.getWorld().spigot().playEffect(attacker.getEntity().getLocation(), Effect.WITCH_MAGIC, 0, 0, 0.5F, 1.0F, 0.5F, 0.5F, 35, 16);
                    attackerEntity.getWorld().spawnParticle(Particle.SPELL_WITCH, attackerEntity.getLocation(), 35, 0.5, 1, 0.5, 0.5);
                    broadcast(attackerEntity.getLocation(), missText.replace("%target%", CustomNameManager.getName(attacker)));
                    attackerEntity.getWorld().playSound(attackerEntity.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 1.0F, 0F);
                }
            }
        }
    }
}
