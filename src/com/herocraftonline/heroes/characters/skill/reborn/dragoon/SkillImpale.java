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
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

public class SkillImpale extends TargettedSkill {

    private String stunApplyText;
    private String applyText;
    private String expireText;

    public SkillImpale(Heroes plugin) {
        super(plugin, "Impale");
        setDescription("You impale your target with your weapon, dealing $1 physical damage and slowing them for $2 second(s). " +
                "If you impale the target against a wall, they will be stunned for $3 second(s) as well.");
        setUsage("/skill impale");
        setArgumentRange(0, 0);
        setIdentifiers("skill impale");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.MOVEMENT_SLOWING, SkillType.INTERRUPTING);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        int stunDuration = SkillConfigManager.getUseSetting(hero, this, "pinned-stun-duration", 2000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage))
                .replace("$2", Util.decFormat.format(duration / 1000.0))
                .replace("$3", Util.decFormat.format(stunDuration / 1000.0));
    }

    @Override
    public void init() {
        super.init();

        stunApplyText = SkillConfigManager.getRaw(this, "stun-apply-text", ChatComponents.GENERIC_SKILL + "%target% has been pinned by %hero%'s impale!")
                .replace("%target%", "$1")
                .replace("%hero%", "$2");

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% has been stunned by %hero%'s impale!")
                .replace("%target%", "$1")
                .replace("%hero%", "$2");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% is no longer impaled!")
                .replace("%target%", "$1");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 6);
        config.set(SkillSetting.TARGET_HIT_TOLERANCE.node(), 0.25);
        config.set(SkillSetting.ON_INTERRUPT_FORCE_COOLDOWN.node(), 1500);
        config.set(SkillSetting.DAMAGE.node(), 50);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.0);
        config.set(SkillSetting.DURATION.node(), 3000);
        config.set("pinned-stun-duration", 2000);
        config.set("slow-amplitude", 2);
        config.set("stun-apply-text", ChatComponents.GENERIC_SKILL + "%target% has been pinned by %hero%'s impale!");
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been stunned by %hero%'s impale!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer impaled!");
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);
        addSpellTarget(target, hero);

        // take current player direction vector less a bit
        Vector playerDirection = player.getLocation().getDirection().normalize().multiply(0.9);

        Location behindLoc = target.getEyeLocation().add(playerDirection);
        Material blockBehindTarget = behindLoc.getBlock().getType();

        long duration;
        int amplitude;
        if ((blockBehindTarget.isSolid() && !Util.transparentBlocks.contains(blockBehindTarget))) {
            // Impaled, let's stun them!
            duration = SkillConfigManager.getUseSetting(hero, this, "pinned-stun-duration", 2000, false);

            StunEffect effect = new StunEffect(this, player, duration, stunApplyText, expireText);
            plugin.getCharacterManager().getCharacter(target).addEffect(effect);
        }


        duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);


        StunEffect effect = new StunEffect(this, player, duration, applyText, expireText);
        plugin.getCharacterManager().getCharacter(target).addEffect(effect);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_HURT, 1.0F, 1.0F);
        player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 0.5, 0), 75, 0, 0, 0, 1);
        player.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0, 0.5, 0), 45, 0.3, 0.2, 0.3, 0.5
//                ,Bukkit.createBlockData(Material.NETHER_WART_BLOCK)
        );

        return SkillResult.NORMAL;
    }
}
