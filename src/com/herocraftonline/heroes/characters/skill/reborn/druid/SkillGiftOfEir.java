package com.herocraftonline.heroes.characters.skill.reborn.druid;

import com.comphenix.protocol.PacketType;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;

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
        double mana = SkillConfigManager.getUseSetting(hero, this, "mana-percent-cost", 0.40, false);
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format((double) duration / 1000.0))
                .replace("$2", Util.decFormat.format(mana))
                .replace("$3", Util.decFormat.format(radius));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("mana-percent-cost", 0.40);
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
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 100, false);
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5000, false);
        double radiusSquared = radius * radius;
        hero.addEffect(new RootEffect(this, player, period, duration));
        long period2 = 1000;
        hero.addEffect(new ManaPoolEffect(this, player, period2, duration, radiusSquared));
        hero.addEffect(new InvulnStationaryEffect(this, player, duration, applyText, expireText, radiusSquared));

        return SkillResult.NORMAL;
    }

    public class ManaPoolEffect extends PeriodicExpirableEffect {
        private double radiusSquared;
        private double manaCost;

        public ManaPoolEffect(Skill skill, Player applier, long period, long duration, double rSquared) {
            super(skill, "ManaPoolEFfect", applier, period, duration);
            radiusSquared = radiusSquared;
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.AREA_OF_EFFECT);
            types.add(EffectType.MANA_INCREASING);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            // get mana from hero
            double mana = hero.getMana();
            double maxMana = hero.getMaxMana();
            double manaPercent = mana / maxMana;
            manaCost = SkillConfigManager.getUseSetting(hero, skill, "mana-percent-cost", 0.40, false);

            // calculate mana cost
            if (manaPercent < manaCost) {
                manaCost = mana;
            } else {
                manaCost = (int) (manaCost * maxMana);
            }

            // apply mana cost
            HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, (int) manaCost, skill);
            plugin.getServer().getPluginManager().callEvent(hrmEvent);
            if (!hrmEvent.isCancelled()) {
                hero.setMana((int) (mana - manaCost));
                if (hero.isVerboseMana())
                    hero.getPlayer().sendMessage(ChatComponents.Bars.mana(hero.getMana(), hero.getMaxMana(), true));
            }
        }

        public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius)
        {
            World world = centerPoint.getWorld();

            double increment = (2 * Math.PI) / particleAmount;

            ArrayList<Location> locations = new ArrayList<Location>();

            for (int i = 0; i < particleAmount; i++)
            {
                double angle = i * increment;
                double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
                double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
                locations.add(new Location(world, x, centerPoint.getY(), z));
            }
            return locations;
        }

        @Override
        public void tickMonster(Monster monster) {

        }

        @Override
        public void tickHero(Hero hero) {
            // get players and give them mana
            Player player = hero.getPlayer();
            player.sendMessage("tick");
            Location heroLoc = player.getLocation();

            for (double r = 1; r < radiusSquared; r++)
            {
                ArrayList<Location> particleLocations = circle(player.getEyeLocation(), 45,  5);
                for (int i = 0; i < particleLocations.size(); i++)
                {
                    player.getWorld().spawnParticle(Particle.CLOUD, particleLocations.get(i), 1, 0, 0.1, 0, 0.1);
                }
            }
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GUARDIAN_AMBIENT, 1.0F, 1.2F);

            /*
            if (hero.getParty() == null) {
                return;
            }
             */


            player.sendMessage("finding party members");
            // find part member within radius
            ArrayList<Hero> heroList = new ArrayList<>();
            for (Hero partyHero : hero.getParty().getMembers()) {
                if (hero.getPlayer().equals(partyHero.getPlayer())) {
                    player.sendMessage("canot add self");
                } else if (!player.getWorld().equals(partyHero.getPlayer().getWorld())) {
                    player.sendMessage("party member is not a member of your world");
                } else if ((partyHero.getPlayer().getLocation().distanceSquared(heroLoc) <= radiusSquared)) {
                    player.sendMessage("found a party member");
                    heroList.add(partyHero);
                }
            }


            // give them mana
            if (heroList.size() == 0) {
                player.sendMessage("no party members found");
                return;
            }

            // give them mana
            int manaIncreaseAmount = (int) manaCost / heroList.size();
            player.sendMessage("party members to give mana: " + heroList.size());
            for (Hero heroChosen : heroList) {
                heroChosen.getPlayer().sendMessage("mana given by druid: " + manaIncreaseAmount);
                HeroRegainManaEvent hrmEvent2 = new HeroRegainManaEvent(heroChosen, manaIncreaseAmount, skill);
                plugin.getServer().getPluginManager().callEvent(hrmEvent2);
                if (!hrmEvent2.isCancelled()) {
                    heroChosen.getPlayer().sendMessage("received mana: " + manaIncreaseAmount);
                    heroChosen.setMana(hrmEvent2.getDelta() + heroChosen.getMana());

                    if (hero.isVerboseMana())
                        heroChosen.getPlayer().sendMessage(ChatComponents.Bars.mana(heroChosen.getMana(), heroChosen.getMaxMana(), true));
                }
            }

            player.sendMessage("done");
        }
    }

    public class InvulnStationaryEffect extends ExpirableEffect {
        public InvulnStationaryEffect(Skill skill, Player applier, long duration, String applyText, String expireText, double rSquared) {
            super(skill, "InvulnStationaryEffect", applier, duration, applyText, expireText);
            types.add(EffectType.INVULNERABILITY);
            types.add(EffectType.UNTARGETABLE);
            types.add(EffectType.UNBREAKABLE);
            types.add(EffectType.SILENCE);
            types.add(EffectType.ROOT);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            /*
            VisualEffect.playInstantFirework(FireworkEffect.builder()
                    .flicker(false)
                    .trail(false)
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(Color.AQUA)
                    .withFade(Color.BLUE)
                    .build(), hero.getPlayer().getLocation().add(0, 2.0, 0));
             */
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }
    }
}

