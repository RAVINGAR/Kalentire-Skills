package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillSonicBoom extends ActiveSkill {

    public SkillSonicBoom(Heroes plugin) {
        super(plugin, "SonicBoom");
        this.setDescription("You creat a clap of thunder dealing $3 magic damage and silencing everyone within $1 blocks for $2 second(s).");
        this.setUsage("/skill sonicboom");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill sonicboom");
        this.setTypes(SkillType.FORCE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.DURATION_INCREASE.node(), 0);
        node.set(SkillSetting.RADIUS.node(), 10);
        node.set(SkillSetting.RADIUS_INCREASE.node(), 0);
        node.set(SkillSetting.DAMAGE.node(), 0);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0);
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 30, false) + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE, 0, false) * hero.getHeroLevel(this));
        final int duration = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false) + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE, 0, false) * hero.getHeroLevel(this))) / 1000;
        final double damage = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 0, false) + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0, false) * hero.getHeroLevel(this)));
        String description = this.getDescription().replace("$1", radius + "").replace("$2", duration + "").replace("$3", damage + "");

        //COOLDOWN
        final int cooldown = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 0, false) - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 0, false) * hero.getHeroLevel(this))) / 1000;
        if (cooldown > 0) {
            description += " CD:" + cooldown + "s";
        }

        //MANA
        final int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA.node(), 10, false) - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE.node(), 0, false) * hero.getHeroLevel(this));
        if (mana > 0) {
            description += " M:" + mana;
        }

        //HEALTH_COST
        final int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false) - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST_REDUCE, mana, true) * hero.getHeroLevel(this));
        if (healthCost > 0) {
            description += " HP:" + healthCost;
        }

        //STAMINA
        final int staminaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA.node(), 0, false) - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA_REDUCE.node(), 0, false) * hero.getHeroLevel(this));
        if (staminaCost > 0) {
            description += " FP:" + staminaCost;
        }

        //DELAY
        final int delay = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
            description += " W:" + delay + "s";
        }

        //EXP
        final int exp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXP.node(), 0, false);
        if (exp > 0) {
            description += " XP:" + exp;
        }
        return description;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 30, false);
        radius += SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE, 0, false) * hero.getHeroLevel(this);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 0, false);
        damage += SkillConfigManager.getUseSetting(hero, this, "damage-increase", 0, false) * hero.getHeroLevel(this);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false);
        duration += SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0, false) * hero.getHeroLevel(this);
        final Player player = hero.getPlayer();
        boolean hit = false;
        for (final Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity) || !damageCheck(player, (LivingEntity) e)) {
                continue;
            }
            final CharacterTemplate character = this.plugin.getCharacterManager().getCharacter((LivingEntity) e);
            if (character instanceof Hero) {
                ((Hero) character).cancelDelayedSkill();
            }
            this.addSpellTarget(e, hero);
            damageEntity(character.getEntity(), player, damage, DamageCause.MAGIC);
            character.addEffect(new SilenceEffect(this, player, duration));
            hit = true;
        }
        if (!hit) {
            player.sendMessage(ChatColor.GRAY + "No nearby targets!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 3);
        this.broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
}
