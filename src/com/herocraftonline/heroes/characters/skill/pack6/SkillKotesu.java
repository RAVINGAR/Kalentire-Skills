package com.herocraftonline.heroes.characters.skill.pack6;

import org.bukkit.Material;
import org.bukkit.Particle;
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
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.CompatSound;
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
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.6, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int swordSacrificePercent = SkillConfigManager.getUseSetting(hero, this, "sword-sacrifice-percent", 5, false);

        return getDescription().replace("$1", damage + "").replace("$2", swordSacrificePercent + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.6);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "$1 is slowed by Kotesu's power!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "Kotesu's power fades from $1.");
        node.set("sword-sacrifice-percent", 5);
        node.set("slowness-duration", 2000);
        node.set("slowness-amplitude", 2);
        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "$1 is slowed by Kotesu's power!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "Kotesu's power fades from $1.");
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!damageCheck(player, target)) {
            player.sendMessage("You can't damage that target!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords).contains(item.name())) {
            player.sendMessage("You can't use Kotesu with that weapon!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int duraPercent = SkillConfigManager.getUseSetting(hero, this, "sword-sacrifice-percent", 5, false);
        if (duraPercent > 0) {
            short dura = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getDurability();
            short maxDura = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType().getMaxDurability();
            short duraCost = (short) (maxDura * (duraPercent * 0.01));

            if (dura == (short) 0) {
                NMSHandler.getInterface().getItemInMainHand(player.getInventory()).setDurability((short) (duraCost));
            } else if (maxDura - dura > duraCost) {
                NMSHandler.getInterface().getItemInMainHand(player.getInventory()).setDurability((short) (dura + duraCost));
            } else if (maxDura - dura == duraCost) {
                NMSHandler.getInterface().setItemInMainHand(player.getInventory(), null);
                player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_ITEM_BREAK.value(), 0.5F, 1.0F);
            } else {
                player.sendMessage("Your Katana doesn't have enough durability to use Kotesu!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
        }

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.6, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        long duration = SkillConfigManager.getUseSetting(hero, this, "slowness-duration", 2000, false);
        int amplifier = SkillConfigManager.getUseSetting(hero, this, "slowness-amplitude", 1, false);

        if (damage > 0) {
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC, false);
        }

        SlowEffect slowEffect = new SlowEffect(this, "KotesuSlowEffect", player, duration, amplifier, applyText, expireText);

        plugin.getCharacterManager().getCharacter(target).addEffect(slowEffect);

        //player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 150, 16);
        player.getWorld().spawnParticle(Particle.CRIT_MAGIC, target.getLocation().add(0, 0.5, 0), 150, 0, 0, 0, 1);


        return SkillResult.NORMAL;
    }
}
