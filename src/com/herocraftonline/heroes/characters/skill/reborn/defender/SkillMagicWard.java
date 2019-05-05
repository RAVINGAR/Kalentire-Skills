package com.herocraftonline.heroes.characters.skill.reborn.defender;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import jline.internal.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class SkillMagicWard extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillMagicWard(Heroes plugin) {
        super(plugin, "MagicWard");
        setDescription("Create a Magical Ward on your party for the next $1 second(s)$2. While active, you reduce incoming magic damage by $3%.");
        setArgumentRange(0, 0);
        setUsage("/skill magicward");
        setIdentifiers("skill magicward", "skill rayshield");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.DEBUFFING, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
        double damageReduction = SkillConfigManager.getUseSetting(hero, this, "damage-reduction", 0.2, false);
        int maxAlliesProtected = SkillConfigManager.getUseSetting(hero, this, "max-allies-protected", -1, false);
        boolean requireShieldToActivate = SkillConfigManager.getUseSetting(hero, this, "require-shield-to-activate", false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDamageReduction = Util.decFormat.format(damageReduction * 100);

        String description = getDescription();
        if (requireShieldToActivate){
            description += " Requires a held shield to activate.";
        }
        return description.replace("$1", formattedDuration)
                .replace("$2", maxAlliesProtected > 0 ? (", for up to " + maxAlliesProtected + " allies.") : "")
                .replace("$3", formattedDamageReduction);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("damage-reduction", 0.2);
        node.set(SkillSetting.COOLDOWN.node(), 15000);
        node.set(SkillSetting.DURATION.node(), 8000);
        node.set(SkillSetting.RADIUS.node(), 10.0);
        node.set("max-allies-protected", -1);
        node.set("require-shield-to-activate", false);
        node.set("allow-doors-as-shields", false);
        node.set("allow-trapdoors-as-shields", false);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s party is being protected by a Magic Ward!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s Magic Ward has faded");

        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero%'s party is being protected by a Magic Ward!")
                .replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero%'s Magic Ward has faded")
                .replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        boolean requireShieldToActivate = SkillConfigManager.getUseSetting(hero, this, "require-shield-to-activate", false);
        if (requireShieldToActivate && isWearingShield(hero)){
            player.sendMessage("You must have a shield equipped to use this skill");
            return SkillResult.FAIL;
        }

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
        double damageReduction = SkillConfigManager.getUseSetting(hero, this, "damage-reduction", 0.2, false);
        int maxAlliesProtected = SkillConfigManager.getUseSetting(hero, this, "max-allies-protected", -1, false);
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 10.0, false);

        // Apply ward to party members
        if (hero.hasParty() && maxAlliesProtected != 0){
            Set<Hero> partyMembers = hero.getParty().getMembers();
            partyMembers.remove(hero);

            if (maxAlliesProtected < 0) {
                // protect whole party
                maxAlliesProtected = partyMembers.size();
            }

            int alliesProtected = 0;
            for (Hero partyMember : hero.getParty().getMembers()) {
                if (alliesProtected == maxAlliesProtected){
                    break;
                }
                if (partyMember.getPlayer().getLocation().distance(player.getLocation()) <= radius) {
                    partyMember.addEffect(new MagicWardEffect(this, player, duration, damageReduction, null, null));
                    alliesProtected++;
                }
            }

        }
        // apply ward to caster
        hero.addEffect(new MagicWardEffect(this, player, duration, damageReduction, applyText, expireText));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.8F, 1.0F);

        return SkillResult.NORMAL;
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

    public class SkillHeroListener implements Listener {

        @EventHandler
        public void onSkillDamage(SkillDamageEvent event) {
            if (!(event.getEntity() instanceof Player) || !event.getSkill().isType(SkillType.ABILITY_PROPERTY_MAGICAL))
                return;

            Hero defenderHero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (defenderHero.hasEffect("MagicWard")) {
                double damageReduction = 1.0 - ((MagicWardEffect) defenderHero.getEffect("MagicWard")).getDamageReduction();
                event.setDamage((event.getDamage() * damageReduction));
            }
        }
    }

    public class MagicWardEffect extends ExpirableEffect {

        private final double damageReduction;

        public MagicWardEffect(Skill skill, Player applier, long duration, double damageReduction, String applyText, String expireText) {
            super(skill, "MagicWard", applier, duration, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);
            types.add(EffectType.BENEFICIAL);

            this.damageReduction = damageReduction;
        }

        public double getDamageReduction() {
            return damageReduction;
        }
    }
}
