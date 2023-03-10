package com.herocraftonline.heroes.characters.skill.general;

import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.WitheringEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;

public class SkillDarkBlade extends TargettedSkill {

    public SkillDarkBlade(Heroes plugin) {
        super(plugin, "DarkBlade");
        setDescription("Strike your target with a blade of dark, dealing $1 physical damage, draining $2 of their mana, and giving it to you.");
        setUsage("/skill darkblade");
        setArgumentRange(0, 0);
        setIdentifiers("skill darkblade");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.SILENCEABLE, SkillType.DAMAGING,
                SkillType.AGGRESSIVE, SkillType.MANA_INCREASING, SkillType.MANA_DECREASING);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 98, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        int manaDrain = SkillConfigManager.getUseSetting(hero, this, "mana-drain", 100, false);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedManaDrain = Util.decFormat.format(manaDrain);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedManaDrain);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set(SkillSetting.DAMAGE.node(), 85);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.75);
        node.set("mana-drain", 100);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 98, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);
        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        targetCT.addEffect(new WitheringEffect(this, player, 1000, 0, null, null));

        int manaDrain = SkillConfigManager.getUseSetting(hero, this, "mana-drain", 100, false);

        // If the target is a player, drain their mana
        if ((target instanceof Player)) {
            // Get the target hero
            Hero tHero = plugin.getCharacterManager().getHero((Player) target);

            if (tHero.getMana() > manaDrain) {
                int newMana = tHero.getMana() - manaDrain;
                tHero.setMana(newMana);
            }
            else {
                tHero.setMana(0);
            }

            if (tHero.isVerboseMana())
                player.sendMessage(ChatComponents.Bars.mana(tHero.getMana(), tHero.getMaxMana(), false));
        }

        HeroRegainManaEvent hrEvent = new HeroRegainManaEvent(hero, manaDrain, this);
        plugin.getServer().getPluginManager().callEvent(hrEvent);
        if (!hrEvent.isCancelled()) {
            hero.setMana(hrEvent.getDelta() + hero.getMana());

            if (hero.isVerboseMana())
                player.sendMessage(ChatComponents.Bars.mana(hero.getMana(), hero.getMaxMana(), false));
        }

        //player.getWorld().spigot().playEffect(target.getEyeLocation().add(0, 0.5, 0), org.bukkit.Effect.WITCH_MAGIC, 0, 0, 0, 0, 0, 1, 35, 16);'
        player.getWorld().spawnParticle(Particle.SPELL_WITCH, target.getLocation().add(0, 0.5, 0), 35, 0, 0, 0, 1);
        player.getWorld().playSound(target.getLocation(), Sound.AMBIENT_CAVE, 5.0F, 0.2F);
        return SkillResult.NORMAL;
    }
}