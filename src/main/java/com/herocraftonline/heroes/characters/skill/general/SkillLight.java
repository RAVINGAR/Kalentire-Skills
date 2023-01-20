package com.herocraftonline.heroes.characters.skill.general;

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
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillLight extends ActiveSkill {

    private final BlockData newBlock = Material.GLOWSTONE.createBlockData();
    private String applyText;
    private String expireText;

    public SkillLight(final Heroes plugin) {
        super(plugin, "Light");
        setDescription("You glow brightly, illuminating blocks around you.");
        setArgumentRange(0, 0);
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE);
        setUsage("/skill light");
        setIdentifiers("skill light");
    }

    @Override
    public String getDescription(final Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 30000); // in milliseconds
        node.set(SkillSetting.PERIOD.node(), 200); // in milliseconds
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is lighting the way.");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer lighting the way");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is lighting the way.").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer lighting the way.").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {

        final Player player = hero.getPlayer();
        broadcastExecuteText(hero);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 200, false);

        hero.addEffect(new LightEffect(this, player, period, duration));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class LightEffect extends PeriodicExpirableEffect {

        private Location lastLoc = null;
        private BlockData lastBlock = null;

        public LightEffect(final Skill skill, final Player applier, final long period, final long duration) {
            super(skill, "Light", applier, period, duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.LIGHT);
            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player p = hero.getPlayer();
            broadcast(p.getLocation(), "    " + applyText, p.getName());
            final Block thisBlock = p.getLocation().getBlock().getRelative(BlockFace.DOWN);

            if (!Util.transparentBlocks.contains(thisBlock.getType())) {
                lastLoc = thisBlock.getLocation();
                lastBlock = thisBlock.getBlockData();
                p.sendBlockChange(lastLoc, newBlock);
            }
        }

        @Override
        public void tickHero(final Hero hero) {
            final Player p = hero.getPlayer();
            final Block thisBlock = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (!thisBlock.getLocation().equals(lastLoc)) {

                if (lastLoc != null) {
                    p.sendBlockChange(lastLoc, lastBlock);
                }

                if (!Util.transparentBlocks.contains(thisBlock.getType())) {

                    lastLoc = thisBlock.getLocation();
                    lastBlock = thisBlock.getBlockData();

                    p.sendBlockChange(lastLoc, newBlock);
                }

            }
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player p = hero.getPlayer();
            broadcast(p.getLocation(), "    " + expireText, p.getName());
            if (lastLoc != null) {
                p.sendBlockChange(lastLoc, lastBlock);
            }
        }

        @Override
        public void tickMonster(final Monster monster) {
        }
    }
}
