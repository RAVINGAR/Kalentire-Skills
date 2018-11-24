package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.standard.BleedingEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import joptsimple.internal.Strings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;

public class SkillExtraDamageToBleedingEnemies extends PassiveSkill implements Listener {

    // TODO Find a unified place for this for multipul skills
    private static final EnumSet<Material> shovels = EnumSet.of(Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL);

    private static final String FLAT_DAMAGE_INCREASE_PER_BLEEDING_STACK_NODE = "flat-damage-increase-per-bleeding-stack";
    private static final double DEFAULT_FLAT_DAMAGE_INCREASE_PER_BLEEDING_STACK = 2.0;

    private static final String PERCENT_DAMAGE_INCREASE_PER_BLEEDING_STACK_NODE = "percent-damage-increase-per-bleeding-stack";
    private static final double DEFAULT_PERCENT_DAMAGE_INCREASE_PER_BLEEDING_STACK = 0.02;

    public SkillExtraDamageToBleedingEnemies(Heroes plugin) {
        super(plugin, "ExtraDamageToBleedingEnemies");
        setDescription("Basic attack damage is increased by $1%$2 per stack of bleed on the target.");
    }

    @Override
    public void init() {
        super.init();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {

        ConfigurationSection node = super.getDefaultConfig();

        node.set(FLAT_DAMAGE_INCREASE_PER_BLEEDING_STACK_NODE, DEFAULT_FLAT_DAMAGE_INCREASE_PER_BLEEDING_STACK);
        node.set(PERCENT_DAMAGE_INCREASE_PER_BLEEDING_STACK_NODE, DEFAULT_PERCENT_DAMAGE_INCREASE_PER_BLEEDING_STACK);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {

        double flatDamageIncreasePerBleedingStack = SkillConfigManager.getUseSetting(hero, this,
                FLAT_DAMAGE_INCREASE_PER_BLEEDING_STACK_NODE, DEFAULT_FLAT_DAMAGE_INCREASE_PER_BLEEDING_STACK, false);
        if (flatDamageIncreasePerBleedingStack < 0) {
            flatDamageIncreasePerBleedingStack = 0;
        }

        double percentDamageIncreasePerBleedingStack = SkillConfigManager.getUseSetting(hero, this,
                PERCENT_DAMAGE_INCREASE_PER_BLEEDING_STACK_NODE, DEFAULT_PERCENT_DAMAGE_INCREASE_PER_BLEEDING_STACK, false);
        if (percentDamageIncreasePerBleedingStack < 0) {
            percentDamageIncreasePerBleedingStack = 0;
        }

        String flatDamageIncreasePerBleedStackParam = flatDamageIncreasePerBleedingStack > 0 ? " +" + Util.smallDecFormat.format(flatDamageIncreasePerBleedingStack) : Strings.EMPTY;
        String percentDamageIncreasePerBleedStackParam = Util.smallDecFormat.format(percentDamageIncreasePerBleedingStack * 100);

        return Messaging.parameterizeMessage(getDescription(), percentDamageIncreasePerBleedStackParam, flatDamageIncreasePerBleedStackParam);
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onWeaponDamage(WeaponDamageEvent e) {

        if (e.getDamager() instanceof Hero && e.getEntity() instanceof LivingEntity) {

            Hero hero = (Hero) e.getDamager();
            Player player = hero.getPlayer();
            ItemStack weapon = player.getInventory().getItemInMainHand();

            if (weapon != null && shovels.contains(weapon.getType()) && hasPassive(hero)) {

                LivingEntity target = (LivingEntity) e.getEntity();


                if (damageCheck(player, target)) {

                    CharacterTemplate targetCharacter = Heroes.getInstance().getCharacterManager().getCharacter(target);

                    BleedingEffect bleedEffect = BleedingEffect.get(targetCharacter);
                    if (bleedEffect != null) {

                        double flatDamageIncreasePerBleedingStack = SkillConfigManager.getUseSetting(hero, this,
                                FLAT_DAMAGE_INCREASE_PER_BLEEDING_STACK_NODE, DEFAULT_FLAT_DAMAGE_INCREASE_PER_BLEEDING_STACK, false);
                        if (flatDamageIncreasePerBleedingStack < 0) {
                            flatDamageIncreasePerBleedingStack = 0;
                        }

                        double percentDamageIncreasePerBleedingStack = SkillConfigManager.getUseSetting(hero, this,
                                PERCENT_DAMAGE_INCREASE_PER_BLEEDING_STACK_NODE, DEFAULT_PERCENT_DAMAGE_INCREASE_PER_BLEEDING_STACK, false);
                        if (percentDamageIncreasePerBleedingStack < 0) {
                            percentDamageIncreasePerBleedingStack = 0;
                        }

                        double flatDamageIncrease = flatDamageIncreasePerBleedingStack * bleedEffect.getStackCount();
                        double percentDamageIncrease = percentDamageIncreasePerBleedingStack * bleedEffect.getStackCount();

                        double extraDamage = flatDamageIncrease + (e.getDamage() * percentDamageIncrease);
                        e.setDamage((e.getDamage() + extraDamage));
                    }
                }
            }
        }
    }
}
