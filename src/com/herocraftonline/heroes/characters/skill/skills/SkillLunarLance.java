package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillLunarLance extends TargettedSkill {

    //public VisualEffect fplayer = new VisualEffect();

    public SkillLunarLance(Heroes plugin) {
        super(plugin, "LunarLance");
        setDescription("Strike the target with a Lunar Lance dealing $1 physical damage and burning $2 mana from the target.");
        setUsage("/skill lunarlance");
        setArgumentRange(0, 0);
        setIdentifiers("skill lunarlance");
        setTypes(SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.MANA_DECREASING, SkillType.AGGRESSIVE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("weapons", Util.shovels);
        node.set(SkillSetting.MAX_DISTANCE.node(), 6);
        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.84);
        node.set("mana-burn-amount", 90);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(50), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.0, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        int manaBurn = SkillConfigManager.getUseSetting(hero, this, "mana-burn-amount", 90, false);

        return getDescription().replace("$1", damage + "").replace("$2", manaBurn + "");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();
        ItemStack item = player.getItemInHand();

        // Ensure they have a weapon in hand
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.shovels).contains(item.getType().name())) {
            // Notify them that they don't have a shovel equipped
            Messaging.send(hero.getPlayer(), "You cannot use this skill with that weapon!");

            return SkillResult.FAIL;
        }

        // Broadcast skill usage
        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(50), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        // If the target is a player, drain their mana
        if ((target instanceof Player)) {
            // Get the target hero
            Hero tHero = plugin.getCharacterManager().getHero((Player) target);

            // Burn their mana
            int manaBurn = SkillConfigManager.getUseSetting(hero, this, "mana-burn-amount", 90, false);
            if (tHero.getMana() > manaBurn) {
                // Burn the target's mana
                int newMana = tHero.getMana() - manaBurn;
                tHero.setMana(newMana);
            }
            else {
                // Burn all of their remaining mana
                tHero.setMana(0);
            }

            if (tHero.isVerboseMana())
                Messaging.send(player, Messaging.createManaBar(tHero.getMana(), tHero.getMaxMana()));
        }

        /* Player Firework Effect
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0, 1.5, 0), FireworkEffect.builder().flicker(true)
                    .trail(false).with(FireworkEffect.Type.BALL_LARGE).withColor(Color.BLUE).withFade(Color.AQUA).build());
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.EXTINGUISH, 3);
        player.getWorld().playSound(player.getLocation(), Sound.CAT_HISS, 0.8F, 1.0F);

        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);

        return SkillResult.NORMAL;
    }
}
