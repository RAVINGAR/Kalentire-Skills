package com.herocraftonline.heroes.characters.skill.unusedskills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Iterator;

public class SkillLavaWall extends ActiveSkill {

	private String applyText;
	private String expireText;

	public SkillLavaWall(Heroes plugin) {
		super(plugin, "Lavawall");
		setDescription("Creates a wall of Lava in front of you. The wall is created 1$ blocks in front of you, and is $2 blocks wide, and $3 blocks high.");
		setUsage("/skill lavawall");
		setArgumentRange(0, 0);
		setIdentifiers("skill Lavawall");
		setTypes(SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.KNOWLEDGE, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.ABILITY_PROPERTY_FIRE);
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set("height", 3);
		node.set("width", 2);
		node.set(SkillSetting.MAX_DISTANCE.node(), 20);
		node.set(SkillSetting.DURATION.node(), 5000);
		node.set("block-type", "LAVA");

		return node;
	}

	public String getDescription(Hero hero) {
		int height = SkillConfigManager.getUseSetting(hero, this, "height", 3, false);
		int width = SkillConfigManager.getUseSetting(hero, this, "width", 2, false);
		int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 20, false);
		//String type = SkillConfigManager.getUseSetting(hero, this, "block-type", "LAVA");

		return getDescription().replace("$1", maxDist + "").replace("$2", width + "").replace("$3", height + "");
	}

	@Override
	public void init() {
		super.init();
		applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% conjures a wall of lava!").replace("%hero%", "$1").replace("$hero$", "$1");
		expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero%'s wall has vanished").replace("%hero%", "$1").replace("$hero$", "$1");
	}

	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();

		int height = SkillConfigManager.getUseSetting(hero, this, "Height", 3, false);
		int width = SkillConfigManager.getUseSetting(hero, this, "width", 2, false);
		int maxDist = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 5, false);
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
		Material setter = Material.valueOf(SkillConfigManager.getUseSetting(hero, this, "block-type", "LAVA"));

		Block tBlock = player.getTargetBlock((HashSet<Material>)null, maxDist);
		if (tBlock.getType() == Material.AIR) {
			return SkillResult.INVALID_TARGET;
		}

		ShieldWallEffect swEffect = new ShieldWallEffect(this, hero.getPlayer(), duration, tBlock, width, height, setter);
		hero.addEffect(swEffect);

		return SkillResult.NORMAL;
	}

	public class ShieldWallEffect extends ExpirableEffect {
		private final Block tBlock;
		private final int width;
		private final int height;
		private final HashSet<Block> wBlocks;
		private final Material setter;

		public ShieldWallEffect(Skill skill, Player applier, long duration, Block tBlock, int width, int height, Material setter) {
			super(skill, "sheildWallEffect", applier, duration);
			this.tBlock = tBlock;
			this.width = width;
			this.height = height;
			this.setter = setter;
			this.wBlocks = new HashSet<>(width * height * 2);
		}

		public void applyToHero(Hero hero) {
			super.applyToHero(hero);

			Player player = hero.getPlayer();
			if (is_X_Direction(player)) {
				for (int yDir = 0; yDir < height; yDir++) {
					for (int xDir = -width; xDir < width + 1; xDir++) {
						Block chBlock = tBlock.getRelative(xDir, yDir, 0);
						if ((chBlock.getType() == Material.AIR) || (chBlock.getType() == Material.SNOW)) {
							chBlock.setType(setter);
							wBlocks.add(chBlock);
						}
					}
				}
			}
			else {
				for (int yDir = 0; yDir < height; yDir++) {
					for (int zDir = -width; zDir < width + 1; zDir++) {
						Block chBlock = tBlock.getRelative(0, yDir, zDir);
						if ((chBlock.getType() == Material.AIR) || (chBlock.getType() == Material.SNOW)) {
							chBlock.setType(this.setter);
							wBlocks.add(chBlock);
						}
					}
				}
			}

			broadcast(player.getLocation(), "    " + applyText, player.getName());
		}

		public void removeFromHero(Hero hero) {
			super.removeFromHero(hero);

			Player player = hero.getPlayer();

			for (Block bChange : this.wBlocks) {
				if (bChange.getType() == this.setter) {
					bChange.setType(Material.AIR);
				}
			}

			broadcast(player.getLocation(), "    " + expireText, player.getName());
		}

		private boolean is_X_Direction(Player player) {
			Vector u = player.getLocation().getDirection();
			u = new Vector(u.getX(), 0.0D, u.getZ()).normalize();
			Vector v = new Vector(0, 0, -1);
			double magU = Math.sqrt(Math.pow(u.getX(), 2.0D) + Math.pow(u.getZ(), 2.0D));
			double magV = Math.sqrt(Math.pow(v.getX(), 2.0D) + Math.pow(v.getZ(), 2.0D));
			double angle = Math.acos(u.dot(v) / (magU * magV));
			angle = angle * 180.0D / Math.PI;
			angle = Math.abs(angle - 180.0D);

			return (angle <= 45.0D) || (angle > 135.0D);
		}
	}
}
