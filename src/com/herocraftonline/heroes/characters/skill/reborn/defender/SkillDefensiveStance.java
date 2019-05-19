package com.herocraftonline.heroes.characters.skill.reborn.defender;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;


public class SkillDefensiveStance extends ActiveSkill {
    public static final String SKILL_MESSAGE_PREFIX_SPACES = "    ";

    private String applyText;
    private String expireText;
    private String parryText;
    private String parrySkillText;
    private String parrySkillMagicText;

    public SkillDefensiveStance(Heroes plugin) {
        super(plugin, "DefensiveStance");
        setDescription("When prepared you have a $1% chance to completely negate the next incoming attack within $2 second(s) using your blade (and shield).");
        setUsage("/skill defensivestance");
        setArgumentRange(0, 0);
        setIdentifiers("skill defensivestance", "skill dstance");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.COOLDOWN.node(), 0);
        node.set(SkillSetting.DELAY.node(), 1000);
        node.set(SkillSetting.DURATION.node(), 8000);
        node.set(SkillSetting.CHANCE.node(), 0.8);
        node.set(SkillSetting.CHANCE_PER_LEVEL.node(), 0.02);
        node.set("require-shield-to-activate", false);
        node.set("allow-doors-as-shields", false);
        node.set("allow-trapdoors-as-shields", false);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% steadies themself in a defensive stance!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% loosened their stance!");
        node.set("parry-text", ChatComponents.GENERIC_SKILL + "%hero% parried an attack!");
        node.set("parry-skill-text", ChatComponents.GENERIC_SKILL + "%hero% has parried %target%'s %skill%.");
        node.set("parry-skill-magic-text", ChatComponents.GENERIC_SKILL + "%hero% has countered %target%'s magic.");
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        double overallChance = getOverallHitChance(hero, this);
        return getDescription().replace("$1", Util.decFormat.format(overallChance * 100) + "")
                .replace("$2", Util.decFormat.format(duration / 1000) + "");
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% steadies themself in a defensive stance!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% loosened their stance!").replace("%hero%", "$1");
        parryText = SkillConfigManager.getRaw(this, "parry-text", ChatComponents.GENERIC_SKILL + "%hero% parried an attack!");
        parrySkillText = SkillConfigManager.getRaw(this, "parry-skill-text", ChatComponents.GENERIC_SKILL + "%hero% has parried %target%'s %skill%.");
        parrySkillMagicText = SkillConfigManager.getRaw(this, "parry-skill-magic-text", ChatComponents.GENERIC_SKILL + "%hero% has countered %target%'s magic.");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        boolean requireShieldToActivate = SkillConfigManager.getUseSetting(hero, this, "require-shield-to-activate", false);
        if (requireShieldToActivate && isWearingShield(hero)){
            player.sendMessage("You must have a shield equipped to use this skill");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        hero.addEffect(new DefensiveStanceEffect(this, player, duration));

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6F, 1.0F);
        return SkillResult.NORMAL;
    }

    private double getOverallHitChance(Hero hero, Skill skill) {
        double chance = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.CHANCE, false);
        if (chance > 1) {
            chance = 1;
        } else if (chance < 0) {
            chance = 0;
        }
        return chance;
    }

    private boolean isWearingShield(Hero hero){
        boolean allowDoors = SkillConfigManager.getUseSetting(hero, this, "allow-doors-as-shields", false);
        boolean allowTrapdoors = SkillConfigManager.getUseSetting(hero, this, "allow-trapdoors-as-shields", false);

        ItemStack mainhandItem = NMSHandler.getInterface().getItemInMainHand(hero.getPlayer().getInventory());
        ItemStack offhandItem = NMSHandler.getInterface().getItemInOffHand(hero.getPlayer().getInventory());

        return isValidShield(mainhandItem.getType(), allowDoors, allowTrapdoors)
                || isValidShield(offhandItem.getType(), allowDoors, allowTrapdoors);
    }

    private boolean isValidShield(Material material, boolean allowDoors, boolean allowTrapdoors){
        return material == Material.SHIELD || (allowDoors && isDoor(material))
                || (allowTrapdoors && (material == Material.TRAP_DOOR || material == Material.IRON_TRAPDOOR));
    }

    private boolean isDoor(Material material){
        switch (material){
            case WOOD_DOOR: // Oak
            case BIRCH_DOOR_ITEM:
            case SPRUCE_DOOR_ITEM:
            case JUNGLE_DOOR_ITEM:
            case ACACIA_DOOR_ITEM:
            case DARK_OAK_DOOR_ITEM:
            case IRON_DOOR:
                return true;
        }
        return false;
    }


    public class DefensiveStanceEffect extends ExpirableEffect {

        public DefensiveStanceEffect(Skill skill, Player applier, long duration) {
            super(skill, "DefensiveStance", applier, duration);
            this.types.add(EffectType.PHYSICAL);
            this.types.add(EffectType.MAGIC);
            this.types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }
    }

    public class SkillEntityListener implements Listener {

        @SuppressWarnings("unused")
        private Skill skill;

        SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getDamage() == 0 || !(event.getEntity() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect(getName())) {
                double overallChance = getOverallHitChance(hero, skill);

                if (overallChance != 1 && Util.nextRand() > overallChance) {
                    return;
                }

                hero.removeEffect(hero.getEffect(getName()));
                event.setCancelled(true);

                player.sendMessage(SKILL_MESSAGE_PREFIX_SPACES + parryText.replace("%hero%", "You"));
                if (event.getDamager() instanceof Hero) {
                    ((Hero) event.getDamager()).getPlayer().sendMessage(SKILL_MESSAGE_PREFIX_SPACES
                            + parryText.replace("%hero%", player.getName()));
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.getDamage() == 0 || !(event.getEntity() instanceof Player)) {
                return;
            }
            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect(getName())) {
                double overallChance = getOverallHitChance(hero, skill);

                //TODO: Check if the random chance is properly checked.
                if (overallChance != 1 && Util.nextRand() > overallChance) {
                    return;
                }

                hero.removeEffect(hero.getEffect(getName()));
                event.setCancelled(true);

                String message = SKILL_MESSAGE_PREFIX_SPACES
                        + (event.getSkill().isType(SkillType.ABILITY_PROPERTY_MAGICAL) ? parrySkillMagicText : parrySkillText);
                message = message.replace("%hero%", player.getName())
                        .replace("%target%", CustomNameManager.getName(event.getDamager()))
                        .replace("%skill%", event.getSkill().getName());

                player.sendMessage(message);
                if (event.getDamager() instanceof Hero) {
                    ((Hero) event.getDamager()).getPlayer().sendMessage(message);
                }
            }
        }
    }
}