package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.BlindEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillDarkAxe extends TargettedSkill {

    public SkillDarkAxe(Heroes plugin) {
        super(plugin, "DarkAxe");
        setDescription("You infuse your axe with dark magic and cleave your target, dealing $1 damage, interrupting their abilities, and slowing them for $2 second(s).");
        setUsage("/skill darkaxe");
        setArgumentRange(0, 0);
        setIdentifiers("skill darkaxe");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.BLINDING, SkillType.INTERRUPTING);
    }

    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage))
                .replace("$2", Util.decFormat.format(duration / 1000.0));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 4);
        config.set(SkillSetting.INTERRUPT_COOLDOWN.node(), 3000);
        config.set(SkillSetting.DAMAGE.node(), 60);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.DURATION.node(), 3000);
        return config;
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.axes).contains(item.name())) {
            player.sendMessage("You can't use DarkAxe with that weapon!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero, target);

        // Prep variables
        CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);

        // Damage the target and add the slow effect.
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        // Create the effect and blind the target
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        BlindEffect sEffect = new BlindEffect(this, player, duration);
        targCT.addEffect(sEffect);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.0F);
        FireworkEffect firework = FireworkEffect.builder()
                .flicker(false)
                .trail(false)
                .withColor(Color.BLACK)
                .with(FireworkEffect.Type.BURST)
                .build();
        VisualEffect.playInstantFirework(firework, target.getLocation());

        return SkillResult.NORMAL;
    }
}