package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillShadowstep extends TargettedSkill {

	private String noLineOfSightText;
	private String teleFailText;
	private String useText;
	private BlockFace[] faces = {BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST};
	
	public SkillShadowstep(Heroes plugin) {
		super(plugin, "Shadowstep");
		setDescription("You teleport behind the target within $1 blocks.");
		setUsage("/skill shadowstep <target>");
		setArgumentRange(0, 1);
		setIdentifiers("skill shadowstep");
		setTypes(SkillType.DARK, SkillType.TELEPORT, SkillType.SILENCABLE);
	}
	
	@Override
	public void init() {
		super.init();
		useText = SkillConfigManager.getRaw(this, SkillSetting.USE_TEXT, "%hero% ShadowStepped behind %target%!").replace("%hero%", "$1").replace("%target%", "$2");
		noLineOfSightText = SkillConfigManager.getRaw(this, "no-line-of-sight-text", ChatColor.GRAY.toString() + "Target is not within your line of sight!");		teleFailText = SkillConfigManager.getRaw(this, "teleport-fail-text", ChatColor.GRAY.toString() + "Failed to teleport to target.");
	}
	
	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection defaultConfig = super.getDefaultConfig();
		defaultConfig.set(SkillSetting.COOLDOWN.node(), 30000);
		defaultConfig.set(SkillSetting.MANA.node(), 55);
		defaultConfig.set(SkillSetting.STAMINA.node(), 0);
		defaultConfig.set(SkillSetting.MAX_DISTANCE.node(), 5.0);
		defaultConfig.set(SkillSetting.MAX_DISTANCE_INCREASE.node(), 0.1);
		defaultConfig.set(SkillSetting.USE_TEXT.node(), "%hero% ShadowStepped behind %target%!");
		defaultConfig.set("no-line-of-sight-text", ChatColor.GRAY.toString() + "Target is not within your line of sight!");
		defaultConfig.set("teleport-fail-text", ChatColor.GRAY.toString() + "Failed to teleport to target.");
		return defaultConfig;
	}

	@Override
	public String getDescription(Hero hero) {
		Double distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 5.0, false) +
			(SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE, 0.1, false) * hero.getSkillLevel(this));
		return getDescription().replace("$1", distance.toString());
	}
	
	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] args) {
		if(target == hero.getPlayer() || !(target instanceof Player)) {
			return SkillResult.INVALID_TARGET_NO_MSG;
		}
		if(!inLineOfSight(hero.getPlayer(), (Player)target)) {
			hero.getPlayer().sendMessage(noLineOfSightText);
			return SkillResult.FAIL;
		}
		Location targetLoc = target.getLocation();
		BlockFace targetFace = faces[Math.round(targetLoc.getYaw() / 45f) & 0x7];
		Block teleBlock = targetLoc.getBlock().getRelative(targetFace);
		Location teleLoc = teleBlock.getLocation();
		teleLoc.setYaw(targetLoc.getYaw());
		teleLoc.setPitch(targetLoc.getPitch());
		if(hero.getPlayer().teleport(teleLoc)){
			broadcast(targetLoc, useText, hero.getName(), ((Player)target).getName());
			return SkillResult.NORMAL;
		}
		else {
			Messaging.send(hero.getPlayer(), teleFailText);
			return SkillResult.FAIL;
		}
	}
}
