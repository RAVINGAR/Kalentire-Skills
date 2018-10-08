package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroEnterCombatEvent;
import com.herocraftonline.heroes.api.events.HeroLeaveCombatEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.standard.BleedEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import joptsimple.internal.Strings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    private static final String BLEED_FREQUENCY_NODE = "bleed-frequency";
    private static final int DEFAULT_BLEED_FREQUENCY = 2;

    private static final String BLEED_STACK_AMOUNT_NODE = "bleed-stack-amount";
    private static final int DEFAULT_BLEED_STACK_AMOUNT = 1;

    private static final String BLEED_STACK_DURATION_NODE = "bleed-stack-duration";
    private static final int DEFAULT_BLEED_STACK_DURATION = 2000;

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

        node.set(BLEED_FREQUENCY_NODE, DEFAULT_BLEED_FREQUENCY);
        node.set(BLEED_STACK_AMOUNT_NODE, DEFAULT_BLEED_STACK_AMOUNT);
        node.set(BLEED_STACK_DURATION_NODE, DEFAULT_BLEED_STACK_DURATION);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {

        int bleedFrequency = SkillConfigManager.getUseSetting(hero, this, BLEED_FREQUENCY_NODE, DEFAULT_BLEED_FREQUENCY, true);
        if (bleedFrequency < 1) {
            bleedFrequency = 1;
        }

        int bleedStackAmount = SkillConfigManager.getUseSetting(hero, this, BLEED_STACK_AMOUNT_NODE, DEFAULT_BLEED_STACK_AMOUNT, false);
        if (bleedStackAmount < 1) {
            bleedStackAmount = 1;
        }

        int bleedStackDuration = SkillConfigManager.getUseSetting(hero, this, BLEED_STACK_DURATION_NODE, DEFAULT_BLEED_STACK_DURATION, false);
        if (bleedStackDuration < 0) {
            bleedStackDuration = 0;
        }

        String bleedFrequencyParam;
        switch (bleedFrequency) {
            case 1:
                bleedFrequencyParam = Strings.EMPTY;
                break;
            case 2:
                bleedFrequencyParam = bleedFrequency + "nd ";
                break;
            case 3:
                bleedFrequencyParam = bleedFrequency + "rd ";
                break;
            default:
                bleedFrequencyParam = bleedFrequency + "th ";
                break;
        }

        String bleedStackAmountParam = Integer.toString(bleedStackAmount);
        String bleedStackDurationParam = Util.smallDecFormat.format(bleedStackDuration / 1000.0);


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

        if (!e.isProjectile() && e.getDamager() instanceof Hero && e.getEntity() instanceof LivingEntity && e.getDamager().hasEffect(getName())) {

            LivingEntity target = (LivingEntity) e.getEntity();

            if (target.getNoDamageTicks() <= 10) {

                Hero hero = (Hero) e.getDamager();
                Player player = hero.getPlayer();
                ItemStack weapon = player.getInventory().getItemInMainHand();

                if (weapon != null && shovels.contains(weapon.getType())) {

                    playerAttackCounts.compute(hero.getPlayer().getUniqueId(), (playerId, attackCount) -> {

                        if (attackCount == null) {
                            attackCount = 1;
                        } else {
                            attackCount++;
                        }

                        int bleedApplyFrequency = SkillConfigManager.getUseSetting(hero, this, BLEED_FREQUENCY_NODE, DEFAULT_BLEED_FREQUENCY, true);
                        if (bleedApplyFrequency < 1) {
                            bleedApplyFrequency = 1;
                        }

                        if (attackCount % bleedApplyFrequency == 0) {

                            CharacterTemplate targetCharacter = plugin.getCharacterManager().getCharacter(target);

                            int bleedStackAmount = SkillConfigManager.getUseSetting(hero, this, BLEED_STACK_AMOUNT_NODE, DEFAULT_BLEED_STACK_AMOUNT, false);
                            if (bleedStackAmount < 1) {
                                bleedStackAmount = 1;
                            }

                            int bleedStackDuration = SkillConfigManager.getUseSetting(hero, this, BLEED_STACK_DURATION_NODE, DEFAULT_BLEED_STACK_DURATION, false);
                            if (bleedStackDuration < 0) {
                                bleedStackDuration = 0;
                            }

                            targetCharacter.addEffectStacks(BleedEffect.NAME, bleedStackDuration, player, this, bleedStackAmount, name -> new BleedEffect(this));

                            //TODO DEBUG
                            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + ChatColor.GRAY + " Bleed Stack applied on attack ["
                                    + ChatColor.WHITE + bleedStackAmount + ChatColor.GRAY + "] For a total of `" + ChatColor.WHITE
                                    + targetCharacter.getEffectStackCount(BleedEffect.NAME) + ChatColor.GRAY + "`");
                        }

                        return attackCount;
                    });
                }
            }
        }
    }
}
