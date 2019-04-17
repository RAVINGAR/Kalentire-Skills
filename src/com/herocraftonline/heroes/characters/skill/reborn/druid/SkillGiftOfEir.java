package com.herocraftonline.heroes.characters.skill.reborn.druid;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.InvulnerabilityEffect;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;

// player turns invuln for 4 seconds can't mode
// costs 30-40%
public class SkillGiftOfEir extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillGiftOfEir(Heroes plugin) {
        super(plugin, "GiveOfEir");
        setDescription("You become immobilized and invulnerable for $1 seconds. Donate $2% of your mana shared to your party members around $3 radius ");
        setUsage("/skill giftofeir");
        setArgumentRange(0, 0);
        setIdentifiers("skill giftofeir");
        setTypes(SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 40, false);
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format((double) duration / 1000.0))
                .replace("$2", Util.decFormat.format(mana))
                .replace("$3", Util.decFormat.format(radius));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MANA.node(), 40);
        node.set(SkillSetting.RADIUS.node(), 5.0);
        node.set(SkillSetting.PERIOD.node(), 100);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has become invulnerable!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is once again vulnerable!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has become invulnerable!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is once again vulnerable!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        // Remove any harmful effects on the caster (from invuln)
        for (Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.HARMFUL)) {
                hero.removeEffect(effect);
            }
        }

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5000, false);
        double radiusSquared = radius * radius;
        hero.addEffect(new InvulnStationaryEffect(this, player, duration, radius, radiusSquared));



        return SkillResult.NORMAL;
    }


    /*
    public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius) {
        World world = centerPoint.getWorld();

        double increment = (2 * Math.PI) / particleAmount;

        ArrayList<Location> locations = new ArrayList<Location>();

        for (int i = 0; i < particleAmount; i++) {
            double angle = i * increment;
            double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
            double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
            locations.add(new Location(world, x, centerPoint.getY(), z));
        }
        return locations;
    }
    public class ManaShareEffect extends PeriodicExpirableEffect {

        public ManaShareEffect(Skill skill, Player applier, long duration, int period) {
            super(skill, "ManaShareEffect", applier, duration, period);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.AREA_OF_EFFECT);
            //Maybe
            types.add(EffectType.MANA_REGEN_INCREASING);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }

        @Override
        public void tickMonster(Monster monster) {

        }

        @Override
        public void tickHero(Hero hero) {

        }
    }
     */

    public class InvulnStationaryEffect extends ExpirableEffect {
        private final double radius = 0;
        private final double radiusSquared = 0;

        public InvulnStationaryEffect(Skill skill, Player applier, long duration, double radius, double radiusSquared) {
            super(skill, "InvulnStationaryEffect", applier, duration);
//            types.add(EffectType.DISABLE);
            types.add(EffectType.INVULNERABILITY);
            types.add(EffectType.UNTARGETABLE);
            types.add(EffectType.UNBREAKABLE);
            types.add(EffectType.SILENCE);
            types.add(EffectType.ROOT);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            // get players and give them mana
            Player player = hero.getPlayer();
            Location heroLoc = player.getLocation();
            if (hero.getParty() == null) {
                return;
            }

            for (final Hero partyHero : hero.getParty().getMembers()) {
                if (!player.getWorld().equals(partyHero.getPlayer().getWorld()))
                    continue;
                if ((partyHero.getPlayer().getLocation().distanceSquared(heroLoc) <= radiusSquared)) {

                }
            }
        }
    }
}

