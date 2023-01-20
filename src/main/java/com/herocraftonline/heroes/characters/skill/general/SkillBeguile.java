package com.herocraftonline.heroes.characters.skill.general;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Random;

public class SkillBeguile extends TargettedSkill {

    private static final Random random = new Random();

    private String applyText;
    private String expireText;

    public SkillBeguile(final Heroes plugin) {
        super(plugin, "Beguile");
        setDescription("You beguile the target for $1 second(s).");
        setUsage("/skill beguile");
        setArgumentRange(0, 0);
        setIdentifiers("skill beguile");
        setTypes(SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(final Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        final int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 125, false);
        duration += durationIncrease * hero.getAttributeValue(AttributeType.CHARISMA);

        final String formattedDuration = Util.decFormat.format((double) duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.PERIOD.node(), 1000);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 125);
        node.set("max-drift", 2.1);
        node.set("ncp-exemption-duration", 500);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is beguiled!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has regained his wit!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is beguiled!").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has regained his wit!").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {

        broadcastExecuteText(hero, target);

        final Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        final int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 125, false);
        duration += durationIncrease * hero.getAttributeValue(AttributeType.CHARISMA);

        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, true);

        final float maxDrift = (float) SkillConfigManager.getUseSetting(hero, this, "max-drift", 2.1, false);

        final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        final ConfuseEffect beguileEffect = new ConfuseEffect(this, player, duration, period, maxDrift);
        beguileEffect.types.add(EffectType.DISPELLABLE);
        targetCT.addEffect(beguileEffect);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class ConfuseEffect extends PeriodicExpirableEffect {

        private final float maxDrift;

        public ConfuseEffect(final Skill skill, final Player applier, final long duration, final long period, final float maxDrift) {
            super(skill, "Beguile", applier, period, duration);

            types.add(EffectType.HARMFUL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);

            this.maxDrift = maxDrift;

            addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (int) (duration / 1000) * 20, 127), false);
        }

        public void adjustVelocity(final LivingEntity lEntity) {
            final Vector velocity = lEntity.getVelocity();

            final double angle = random.nextDouble() * 2 * Math.PI;
            float xAdjustment = (float) (maxDrift * Math.cos(angle));
            float zAdjustment = (float) (maxDrift * Math.sin(angle));

            final Material belowMat = lEntity.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
            switch (belowMat) {
                case WATER:
                case LAVA:
                case SOUL_SAND:
                    xAdjustment *= 0.75;
                    zAdjustment *= 0.75;
                    break;
                default:
                    break;
            }

            velocity.setX(xAdjustment);
            velocity.setZ(zAdjustment);
            lEntity.setVelocity(velocity);
        }

        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromMonster(final Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, CustomNameManager.getName(monster));
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

        @Override
        public void tickMonster(final Monster monster) {
            adjustVelocity(monster.getEntity());
            if (monster instanceof Creature) {
                ((Creature) monster).setTarget(null);
            }
        }

        @Override
        public void tickHero(final Hero hero) {

            // Let's bypass the nocheat issues...
            NCPUtils.applyExemptions(hero.getPlayer(), () -> adjustVelocity(hero.getPlayer()), Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, skill, "ncp-exemption-duration", 500, false));
        }
    }
}
