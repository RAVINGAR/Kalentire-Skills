package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class SkillNightmare extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillNightmare(Heroes plugin) {
        super(plugin, "Nightmare");
        setDescription("Summon a nightmare around your current location plunging all enemies within $1 blocks of you into a nightmare, " +
                "dealing $2 damage, blinding, and slowing them for $3 seconds.");
        setUsage("/skill nightmare");
        setIdentifiers("skill nightmare");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DARK, SkillType.AREA_OF_EFFECT,
                SkillType.BLINDING, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.INTERRUPTING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format(damage))
                .replace("$3", Util.decFormat.format(duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 45);
        config.set(SkillSetting.ON_INTERRUPT_FORCE_COOLDOWN.node(), 2000);
        config.set(SkillSetting.RADIUS.node(), 8.0);
        config.set(SkillSetting.DURATION.node(), 4000);
        config.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 50);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has blinded %target% with %skill%!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has recovered their sight!");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% has blinded %target% with %skill%!")
                .replace("%hero%", "$2")
                .replace("%target%", "$1")
                .replace("%skill%", "$3");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% has recovered their sight!")
                .replace("%hero%", "$2")
                .replace("%target%", "$1")
                .replace("%skill%", "$3");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        long interruptForceCooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.ON_INTERRUPT_FORCE_COOLDOWN, 2000, false);

        broadcastExecuteText(hero);

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity))
                continue;

            CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) entity);
            character.addEffect(new NightmareEffect(this, player, duration));
            if (character instanceof Hero) {
                ((Hero) character).interruptDelayedSkill(interruptForceCooldown);
            }

            addSpellTarget(entity, hero);
            damageEntity(character.getEntity(), player, damage, EntityDamageEvent.DamageCause.MAGIC);
        }

        for (double r = 1.0; r < radius * 2.0; r++) {
            List<Location> particleLocations = GeometryUtil.circle(player.getLocation(), 36, r / 2);
            for (Location particleLocation : particleLocations) {
                player.getWorld().spigot().playEffect(particleLocation, Effect.SPELL, 0, 0, 0, 0.1F, 0, 0.0F, 1, 16);
//                player.getWorld().spawnParticle(Particle.SPELL, particleLocation, 1, 0, 0.1, 0, 0, null, true);
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.2F, 2.0F);

        return SkillResult.NORMAL;
    }

    class NightmareEffect extends ExpirableEffect {

        NightmareEffect(Skill skill, Player applier, long duration) {
            super(skill, "Nightmare", applier, duration, applyText, expireText);

            types.add(EffectType.HARMFUL);
            types.add(EffectType.DARK);
            types.add(EffectType.BLIND);
            types.add(EffectType.DISPELLABLE);

            addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) ((duration / 1000) * 20), 3));
        }
    }
}