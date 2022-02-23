package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.common.interfaces.Burning;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public class SkillCombustingAxe extends PassiveSkill {

    private String combustText;

    public SkillCombustingAxe(Heroes plugin) {
        super(plugin, "CombustingAxe");
        setDescription("Axe attacks on burning enemies will combust the target, dealing the remaining burning damage immediately at a $1% rate.");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.DAMAGING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        double damageEffectiveness = SkillConfigManager.getUseSetting(hero, this, "damage-effectiveness", 1.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damageEffectiveness * 100));
    }

    public void init() {
        super.init();

        combustText = SkillConfigManager.getRaw(this, "combust-text", ChatComponents.GENERIC_SKILL + "%hero%'s axe combusted %target%!")
                .replace("%hero%", "$1")
                .replace("%target%", "$2");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("damage-effectiveness", 1.0);
        config.set("combust-text", ChatComponents.GENERIC_SKILL + "%hero%'s axe combusted %target%!");
        return config;
    }

    public class SkillHeroListener implements Listener {
        private Skill skill;

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getEntity().getFireTicks() <= 0 || !(event.getDamager() instanceof Hero) || !(event.getEntity() instanceof LivingEntity))
                return;

            Hero hero = (Hero) event.getDamager();
            if (!hero.canUseSkill(skill))
                return;

            Player player = hero.getPlayer();
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand == null || !Util.axes.contains(mainHand.getType().name()))
                return;

            LivingEntity targetLE = (LivingEntity) event.getEntity();
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(targetLE);

            double damage = 0;
            double damageEffectiveness = SkillConfigManager.getUseSetting(hero, skill, "damage-effectiveness", 1.0, false);
            boolean foundBurningEffect = false;
            for (final Effect effect : targetCT.getEffects()) {
                if (!(effect instanceof Burning))
                    continue;

                Burning burningEffect = (Burning) effect;
                damage = burningEffect.getRemainingDamage() * damageEffectiveness;
                targetCT.removeEffect(effect);
                targetLE.setFireTicks(0);
                foundBurningEffect = true;
            }

            if (!foundBurningEffect) {
                damage = plugin.getDamageManager().calculateFireTickDamage(targetLE, damageEffectiveness);
                targetLE.setFireTicks(0);
            }

            if (damage <= 0)
                return;

            addSpellTarget(targetLE, hero);
            damageEntity(targetLE, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);

            FireworkEffect firework = FireworkEffect.builder()
                    .flicker(false)
                    .trail(true)
                    .withColor(Color.RED)
                    .withColor(Color.RED)
                    .withColor(Color.ORANGE)
                    .withFade(Color.BLACK)
                    .with(FireworkEffect.Type.BURST)
                    .build();
            VisualEffect.playInstantFirework(firework, targetLE.getLocation());

            broadcast(targetLE.getLocation(), "    " + combustText, player.getName(), CustomNameManager.getName(targetCT));
        }
    }
}
