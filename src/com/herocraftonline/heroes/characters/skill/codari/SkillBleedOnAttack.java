package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroEnterCombatEvent;
import com.herocraftonline.heroes.api.events.HeroLeaveCombatEvent;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillBleedOnAttack extends PassiveSkill implements Listener {

    // TODO Find a unified place for this for multipul skills
    private static final EnumSet<Material> shovels = EnumSet.of(Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL);

    private static final String BLEEDING_FREQUENCY_NODE = "bleeding-frequency";
    private static final int DEFAULT_BLEEDING_FREQUENCY = 2;

    private static final String BLEEDING_STACK_AMOUNT_NODE = "bleeding-stack-amount";
    private static final int DEFAULT_BLEEDING_STACK_AMOUNT = 1;

    private static final String BLEEDING_STACK_DURATION_NODE = "bleeding-stack-duration";
    private static final int DEFAULT_BLEEDING_STACK_DURATION = 2000;

    private final Map<UUID, Integer> playerAttackCounts;

    public SkillBleedOnAttack(Heroes plugin) {
        super(plugin, "BleedOnAttack");
        setDescription("Basic attacks apply $1 stack(s) of bleed with a duration of $2 second(s) every $3attack while in combat.");

        playerAttackCounts = new HashMap<>();
    }

    @Override
    public void init() {
        super.init();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {

        ConfigurationSection node = super.getDefaultConfig();

        node.set(BLEEDING_FREQUENCY_NODE, DEFAULT_BLEEDING_FREQUENCY);
        node.set(BLEEDING_STACK_AMOUNT_NODE, DEFAULT_BLEEDING_STACK_AMOUNT);
        node.set(BLEEDING_STACK_DURATION_NODE, DEFAULT_BLEEDING_STACK_DURATION);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {

        int bleedingFrequency = SkillConfigManager.getUseSetting(hero, this, BLEEDING_FREQUENCY_NODE, DEFAULT_BLEEDING_FREQUENCY, true);
        if (bleedingFrequency < 1) {
            bleedingFrequency = 1;
        }

        int bleedingStackAmount = SkillConfigManager.getUseSetting(hero, this, BLEEDING_STACK_AMOUNT_NODE, DEFAULT_BLEEDING_STACK_AMOUNT, false);
        if (bleedingStackAmount < 1) {
            bleedingStackAmount = 1;
        }

        int bleedingStackDuration = SkillConfigManager.getUseSetting(hero, this, BLEEDING_STACK_DURATION_NODE, DEFAULT_BLEEDING_STACK_DURATION, false);
        if (bleedingStackDuration < 0) {
            bleedingStackDuration = 0;
        }

        String bleedFrequencyParam;
        switch (bleedingFrequency) {
            case 1:
                bleedFrequencyParam = Strings.EMPTY;
                break;
            case 2:
                bleedFrequencyParam = bleedingFrequency + "nd ";
                break;
            case 3:
                bleedFrequencyParam = bleedingFrequency + "rd ";
                break;
            default:
                bleedFrequencyParam = bleedingFrequency + "th ";
                break;
        }

        String bleedStackAmountParam = Integer.toString(bleedingStackAmount);
        String bleedStackDurationParam = Util.smallDecFormat.format(bleedingStackDuration / 1000.0);


        return Messaging.parameterizeMessage(getDescription(), bleedStackAmountParam, bleedStackDurationParam, bleedFrequencyParam);
    }

    @EventHandler
    private void onEnterCombat(HeroEnterCombatEvent e) {
        if (e.getHero().hasEffect(getName())) {
            playerAttackCounts.putIfAbsent(e.getHero().getPlayer().getUniqueId(), 0);
        }
    }

    @EventHandler
    private void onLeaveCombat(HeroLeaveCombatEvent e) {
        playerAttackCounts.remove(e.getHero().getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent e) {
        playerAttackCounts.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onWeaponDamage(WeaponDamageEvent e) {

        if (!e.isProjectile() && e.getDamager() instanceof Hero && e.getEntity() instanceof LivingEntity) {

            LivingEntity target = (LivingEntity) e.getEntity();

            Hero hero = (Hero) e.getDamager();
            final Player player = hero.getPlayer();
            ItemStack weapon = player.getInventory().getItemInMainHand();

            if (weapon != null && shovels.contains(weapon.getType()) && hasPassive(hero)) {

                playerAttackCounts.compute(hero.getPlayer().getUniqueId(), (playerId, attackCount) -> {

                    if (attackCount == null) {
                        attackCount = 1;
                    } else {
                        attackCount++;
                    }

                    int bleedingApplyFrequency = SkillConfigManager.getUseSetting(hero, this, BLEEDING_FREQUENCY_NODE, DEFAULT_BLEEDING_FREQUENCY, true);
                    if (bleedingApplyFrequency < 1) {
                        bleedingApplyFrequency = 1;
                    }

                    if (attackCount % bleedingApplyFrequency == 0) {

                        CharacterTemplate targetCharacter = plugin.getCharacterManager().getCharacter(target);

                        int bleedingStackDuration = SkillConfigManager.getUseSetting(hero, this, BLEEDING_STACK_DURATION_NODE, DEFAULT_BLEEDING_STACK_DURATION, false);
                        if (bleedingStackDuration < 0) {
                            bleedingStackDuration = 0;
                        }

                        int bleedingStackAmount = SkillConfigManager.getUseSetting(hero, this, BLEEDING_STACK_AMOUNT_NODE, DEFAULT_BLEEDING_STACK_AMOUNT, false);
                        if (bleedingStackAmount < 0) {
                            bleedingStackAmount = 0;
                        }

                        if (bleedingStackDuration > 0 && bleedingStackAmount > 0) {
                            BleedingEffect.applyStacks(targetCharacter, this, player, bleedingStackDuration, bleedingStackAmount);
                        }
                    }

                    return attackCount;
                });
            }
        }
    }
}
