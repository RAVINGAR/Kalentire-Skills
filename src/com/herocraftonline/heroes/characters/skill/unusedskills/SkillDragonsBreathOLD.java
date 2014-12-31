package com.herocraftonline.heroes.characters.skill.unusedskills;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Util;

public class SkillDragonsBreathOLD extends ActiveSkill {
    public VisualEffect fplayer = new VisualEffect();

    public SkillDragonsBreathOLD(Heroes plugin) {
        super(plugin, "DragonsBreath");
        setDescription("You unleash the furious breath of a dragon for the next $1 seconds. Enemies caught in the line of fire are continuously dealt $2 damage.");
        setUsage("/skill dragonsbreath");
        setArgumentRange(0, 0);
        setIdentifiers("skill dragonsbreath");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_FIRE, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedDamage);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 3);
        node.set(SkillSetting.DURATION.node(), 6000);
        node.set(SkillSetting.PERIOD.node(), 750);
        node.set(SkillSetting.DAMAGE.node(), 13);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.425);
        node.set(SkillSetting.RADIUS.node(), 3);
        node.set("fire-spray-move-delay", 1);

        return node;
    }

    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 750, false);

        hero.addEffect(new DragonsBreathEffect(this, player, period, duration));

        return SkillResult.NORMAL;
    }

    public class DragonsBreathEffect extends PeriodicExpirableEffect {

        private int distance;
        private int radius;
        private int moveDelay;

        public DragonsBreathEffect(Skill skill, Player applier, int period, long duration) {
            super(skill, "DragonsBreath", applier, period, duration, null, null);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.FIRE);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            distance = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.MAX_DISTANCE, 10, false);
            radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 2, false);
            moveDelay = SkillConfigManager.getUseSetting(hero, skill, "fire-spray-move-delay", 1, false);
        }

        @Override
        public void tickMonster(Monster monster) {}

        @Override
        public void tickHero(final Hero hero) {
            final Player player = hero.getPlayer();

            Block tempBlock;
            BlockIterator iter = null;
            try {
                iter = new BlockIterator(player, distance);
            }
            catch (IllegalStateException e) {
                return;
            }

            double tempDamage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 90, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
            tempDamage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));
            final double damage = tempDamage;

            int numBlocks = 0;
            while (iter.hasNext()) {
                tempBlock = iter.next();

                if ((Util.transparentBlocks.contains(tempBlock.getType())
                && (Util.transparentBlocks.contains(tempBlock.getRelative(BlockFace.UP).getType())
                || Util.transparentBlocks.contains(tempBlock.getRelative(BlockFace.DOWN).getType())))) {

                    final Location targetLocation = tempBlock.getLocation().clone().add(new Vector(.5, .5, .5));

                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                        public void run() {
                            try {
                                fplayer.playFirework(targetLocation.getWorld(), targetLocation, FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BURST).withColor(Color.MAROON).withFade(Color.ORANGE).build());
                            }
                            catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }

                            final List<Entity> nearbyEntities = player.getNearbyEntities(distance * 2, distance * 2, distance * 2);
                            for (Entity entity : nearbyEntities) {
                                // Check to see if the entity can be damaged
                                if (!(entity instanceof LivingEntity) || entity.getLocation().distance(targetLocation) > radius)
                                    continue;

                                if (!damageCheck(player, (LivingEntity) entity))
                                    continue;

                                // Damage target
                                LivingEntity target = (LivingEntity) entity;

                                addSpellTarget(target, hero);
                                damageEntity(target, player, damage, DamageCause.MAGIC, false);
                            }
                        }

                    }, numBlocks * moveDelay);

                    numBlocks++;
                }
                else
                    break;
            }
        }
    }
}
