package com.herocraftonline.heroes.characters.skill.skills;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillLight extends ActiveSkill {

    private String applyText;
    private String expireText;
    
    public static Set<Material> allowedBlocks = EnumSet.noneOf(Material.class);
    static {
        allowedBlocks.add(Material.DIRT);
        allowedBlocks.add(Material.GRASS);
        allowedBlocks.add(Material.STONE);
        allowedBlocks.add(Material.COBBLESTONE);
        allowedBlocks.add(Material.WOOD);
        allowedBlocks.add(Material.LOG);
        allowedBlocks.add(Material.NETHERRACK);
        allowedBlocks.add(Material.SOUL_SAND);
        allowedBlocks.add(Material.SANDSTONE);
        allowedBlocks.add(Material.GLASS);
        allowedBlocks.add(Material.WOOL);
        allowedBlocks.add(Material.DOUBLE_STEP);
        allowedBlocks.add(Material.BRICK);
        allowedBlocks.add(Material.OBSIDIAN);
        allowedBlocks.add(Material.NETHER_BRICK);
        allowedBlocks.add(Material.MOSSY_COBBLESTONE);
    }

    public SkillLight(Heroes plugin) {
        super(plugin, "Light");
        setDescription("You glow brightly, illuminating blocks around you.");
        setArgumentRange(0, 0);
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE);
        setUsage("/skill light");
        setIdentifiers("skill light");
    }
    
    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 30000); // in milliseconds
        node.set(SkillSetting.PERIOD.node(), 200); // in milliseconds
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is lighting the way.");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is no longer lighting the way");
        return node;
    }
    
    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% is lighting the way.").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% is no longer lighting the way.").replace("%hero%", "$1");
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {

        Player player = hero.getPlayer();
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 200, false);

        hero.addEffect(new LightEffect(this, player, period, duration));
        player.getWorld().playSound(player.getLocation(), Sound.ORB_PICKUP, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class LightEffect extends PeriodicExpirableEffect {

        private Location lastLoc = null;
        private Byte lastData = null;
        private Material lastMat = null;

        public LightEffect(Skill skill, Player applier, long period, long duration) {
            super(skill, "Light", applier, period, duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.LIGHT);
            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player p = hero.getPlayer();
            broadcast(p.getLocation(), "    " + applyText, p.getName());
            Block thisBlock = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (allowedBlocks.contains(thisBlock.getType())) {
                lastLoc = thisBlock.getLocation();
                lastMat = thisBlock.getType();
                lastData = thisBlock.getData();
                p.sendBlockChange(lastLoc, Material.GLOWSTONE, (byte) 0);
            }
        }

        @Override
        public void tickHero(Hero hero) {
            Player p = hero.getPlayer();
            Block thisBlock = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (thisBlock.getLocation().equals(lastLoc)) {
                return;
            } else if (allowedBlocks.contains(thisBlock.getType())) {
                if (lastLoc != null) {
                    p.sendBlockChange(lastLoc, lastMat, lastData);
                }
                lastLoc = thisBlock.getLocation();
                lastMat = thisBlock.getType();
                lastData = thisBlock.getData();
                p.sendBlockChange(lastLoc, Material.GLOWSTONE, (byte) 0);
            } else if (lastLoc != null) {
                p.sendBlockChange(lastLoc, lastMat, lastData);
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player p = hero.getPlayer();
            broadcast(p.getLocation(), "    " + expireText, p.getName());
            if (lastLoc != null) {
                p.sendBlockChange(lastLoc, lastMat, lastData);
            }
        }

        @Override
        public void tickMonster(Monster monster) { }
    }
}
