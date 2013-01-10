package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Setting;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Builder;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.meta.FireworkMeta;

public class SkillArcaneblast extends TargettedSkill
{
  public SkillArcaneblast(Heroes plugin)
  {
    super(plugin, "Arcaneblast");
    setDescription("You arcaneblast the target for $1 light damage.");
    setUsage("/skill arcaneblast");
    setArgumentRange(0, 0);
    setIdentifiers(new String[] { "skill arcaneblast" });
    setTypes(new SkillType[] { SkillType.DAMAGING, SkillType.LIGHT, SkillType.SILENCABLE, SkillType.HARMFUL });
  }

  public ConfigurationSection getDefaultConfig()
  {
    ConfigurationSection node = super.getDefaultConfig();
    node.set(Setting.DAMAGE.node(), Integer.valueOf(10));
    return node;
  }

  public SkillResult use(Hero hero, LivingEntity target, String[] args)
  {
    Player player = hero.getPlayer();

    int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 10, false);
    addSpellTarget(target, hero);
    int n = 5;

    damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC);
    broadcastExecuteText(hero, target);
    Firework firework = (Firework)target.getWorld().spawnEntity(target.getLocation(), EntityType.FIREWORK);
    FireworkMeta meta = firework.getFireworkMeta();
    meta.addEffect(FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BALL_LARGE).withColor(Color.BLUE).withFade(Color.WHITE).build());
    meta.setPower(0);
    firework.setFireworkMeta(meta);
    return SkillResult.NORMAL;
  }

  public String getDescription(Hero hero)
  {
    int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 10, false);
    return getDescription().replace("$1", damage + "");
  }
}