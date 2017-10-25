package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillHarrow extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    private String notBehindPlayerText;

    public SkillHarrow(Heroes plugin) {
        super(plugin, "Harrow");
        setDescription("You harrow your victim from behind, dealing $1 damage and restoring $2 of own health.");
        setUsage("/skill harrow");
        setArgumentRange(0, 0);
        setIdentifiers("skill harrow");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.8, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", 0.9, false);

        String formattedHeal = Util.decFormat.format(damage * healMult);

        return getDescription().replace("$1", damage + "").replace("$2", formattedHeal);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 6);
        node.set(SkillSetting.DAMAGE.node(), 90);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.8);
        node.set("heal-mult", 0.9);
        node.set("not-behind-player-text", ChatComponents.GENERIC_SKILL + "You are not behind the target!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        notBehindPlayerText = SkillConfigManager.getRaw(this, "not-behind-player-text", ChatComponents.GENERIC_SKILL + "You are not behind the target!");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (target.getLocation().getDirection().dot(hero.getPlayer().getLocation().getDirection()) <= 0) {
            Messaging.send(hero.getPlayer(), notBehindPlayerText);
            return SkillResult.FAIL;
        }

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.8, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", 0.9, false);

        HeroRegainHealthEvent hrEvent = new HeroRegainHealthEvent(hero, (damage * healMult), this, hero);
        plugin.getServer().getPluginManager().callEvent(hrEvent);
        if (!hrEvent.isCancelled())
            hero.heal(hrEvent.getDelta());

        broadcastExecuteText(hero, target);

        // this is our fireworks shit
        /*try {
            fplayer.playFirework(player.getWorld(), target.getLocation(), FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.BURST).withColor(Color.GREEN).withFade(Color.PURPLE).build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }*/

        return SkillResult.NORMAL;
    }
}