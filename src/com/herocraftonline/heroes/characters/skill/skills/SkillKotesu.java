package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillKotesu extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillKotesu(Heroes plugin) {
        super(plugin, "Kotesu");
        setDescription("Deal $1 dark damage and slow the target. Costs $2% of your sword.");
        setUsage("/skill kotesu");
        setArgumentRange(0, 0);
        setIdentifiers("skill kotesu");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.MOVEMENT_SLOWING);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.6, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        int swordSacrificePercent = SkillConfigManager.getUseSetting(hero, this, "sword-sacrifice-percent", 5, false);

        return getDescription().replace("$1", damage + "").replace("$2", swordSacrificePercent + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.6);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "$1 is slowed by Kotesu's power!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "Kotesu's power fades from $1.");
        node.set("sword-sacrifice-percent", 5);
        node.set("slowness-duration", 2000);
        node.set("slowness-amplitude", 2);
        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "$1 is slowed by Kotesu's power!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "Kotesu's power fades from $1.");
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!damageCheck(player, target)) {
            Messaging.send(player, "You can't damage that target!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Material item = player.getItemInHand().getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords).contains(item.name())) {
            Messaging.send(player, "You can't use Kotesu with that weapon!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int duraPercent = SkillConfigManager.getUseSetting(hero, this, "sword-sacrifice-percent", 5, false);
        if (duraPercent > 0) {
            short dura = player.getItemInHand().getDurability();
            short maxDura = player.getItemInHand().getType().getMaxDurability();
            short duraCost = (short) (maxDura * (duraPercent * 0.01));

            if (dura == (short) 0) {
                player.getItemInHand().setDurability((short) (duraCost));
            } else if (maxDura - dura > duraCost) {
                player.getItemInHand().setDurability((short) (dura + duraCost));
            } else if (maxDura - dura == duraCost) {
                player.setItemInHand(null);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_BREAK, 0.5F, 1.0F);
            } else {
                Messaging.send(player, "Your Katana doesn't have enough durability to use Kotesu!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
        }

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.6, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        long duration = SkillConfigManager.getUseSetting(hero, this, "slowness-duration", 2000, false);
        int amplifier = SkillConfigManager.getUseSetting(hero, this, "slowness-amplitude", 1, false);

        if (damage > 0) {
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC, false);
        }

        SlowEffect slowEffect = new SlowEffect(this, "KotesuSlowEffect", player, duration, amplifier, applyText, expireText);

        plugin.getCharacterManager().getCharacter(target).addEffect(slowEffect);

        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 150, 16);


        return SkillResult.NORMAL;
    }
}
