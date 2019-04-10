package com.herocraftonline.heroes.characters.skill.reborn.dragoon;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

public class SkillImpale extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillImpale(Heroes plugin) {
        super(plugin, "Impale");
        setDescription("You impale your target with your weapon, dealing $1 physical damage and slowing them for $2 second(s).");
        setUsage("/skill impale");
        setArgumentRange(0, 0);
        setIdentifiers("skill impale");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.MOVEMENT_SLOWING, SkillType.INTERRUPTING);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.0, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", damage + "").replace("$2", formattedDuration);
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% has been slowed by %hero%'s impale!")
                .replace("%target%", "$1")
                .replace("%hero%", "$2");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% is no longer slowed!")
                .replace("%target%", "$1");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 6);
        config.set(SkillSetting.TARGET_HIT_TOLERANCE.node(), 0.25);
        config.set("weapons", Util.shovels);
        config.set(SkillSetting.DAMAGE.node(), 50);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.0);
        config.set(SkillSetting.DURATION.node(), 3000);
        config.set("amplitude", 2);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been slowed by %hero%'s impale!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer slowed!");
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.shovels).contains(item.name())) {
            player.sendMessage("You can't use " + getName() + " with that weapon!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);
        addSpellTarget(target, hero);

        // take current player direction vector less a bit
        Vector playerDirection = player.getLocation().getDirection().normalize().multiply(0.9);
        // move target 1 block away from direction of player
        Location behindTargetLocation = target.getEyeLocation().add(playerDirection);
        // get block
        Material blockBehindTarget = behindTargetLocation.getBlock().getRelative(BlockFace.SELF).getType();

        long duration;
        int amplitude;
        if (blockBehindTarget.isSolid()) {
            target.sendMessage(hero.getPlayer().getDisplayName() + "has pinned you from their imapale!");
            duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
            amplitude = SkillConfigManager.getUseSetting(hero, this, "amplitude", 2, false);
        } else {
            // Add the slow effect
            duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
            amplitude = SkillConfigManager.getUseSetting(hero, this, "amplitude", 2, false);
        }
        SlowEffect sEffect = new SlowEffect(this, player, duration, amplitude, applyText, expireText);
        plugin.getCharacterManager().getCharacter(target).addEffect(sEffect);


        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 8.0F, 0.4F);

        player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 0.5, 0), 75, 0, 0, 0, 1);
        player.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0, 0.5, 0), 45, 0.3, 0.2, 0.3, 0.5, Bukkit.createBlockData(Material.NETHER_WART_BLOCK));
        return SkillResult.NORMAL;
    }
}
