/*package com.herocraftonline.heroes.characters.skill.skills;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillBlaze extends ActiveSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    public SkillBlaze(Heroes plugin) {
        super(plugin, "Blaze");
        setDescription("You ignite all nearby enemies on fire dealing $1 fire damage.");
        setUsage("/skill blaze");
        setArgumentRange(0, 0);
        setIdentifiers("skill blaze");
        setTypes(SkillType.FIRE, SkillType.DAMAGING, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 30000);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set(SkillSetting.DAMAGE.node(), 4);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0.0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        List<Entity> entities = hero.getPlayer().getNearbyEntities(range, range, range);
        int fireTicks = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false) / 50;
        boolean damaged = false;
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            LivingEntity lEntity = (LivingEntity) entity;

            if (!damageCheck(player, lEntity)) {
                continue;
            }

            damaged = true;
            lEntity.setFireTicks(fireTicks);
            // this is our fireworks shit
            try {
                fplayer.playFirework(player.getWorld(), lEntity.getLocation(), FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BURST).withColor(Color.ORANGE).withFade(Color.RED).build());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            plugin.getCharacterManager().getCharacter(lEntity).addEffect(new CombustEffect(this, player));
        }

        if (!damaged) {
            Messaging.send(player, "No targets in range!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

	@Override
	public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false);
        damage += (int) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(this));
        return getDescription().replace("$1", damage + "");
    }
}
*/