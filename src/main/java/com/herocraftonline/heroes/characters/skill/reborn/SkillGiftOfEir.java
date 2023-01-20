package com.herocraftonline.heroes.characters.skill.reborn;

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
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class SkillGiftOfEir extends ActiveSkill {

    private String expireText;

    public SkillGiftOfEir(final Heroes plugin) {
        super(plugin, "GiftOfEir");
        setDescription("You become immobilized and invulnerable for $1 seconds. Donate $2% of your mana shared to your party members around $3 radius ");
        setUsage("/skill giftofeir");
        setArgumentRange(0, 0);
        setIdentifiers("skill giftofeir");
        setTypes(SkillType.SILENCEABLE, SkillType.BUFFING);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        final double mana = SkillConfigManager.getUseSetting(hero, this, "mana-percent-cost", 0.40, false);
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format((double) duration / 1000.0))
                .replace("$2", Util.decFormat.format(mana))
                .replace("$3", Util.decFormat.format(radius));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("mana-percent-cost", 0.40);
        node.set(SkillSetting.RADIUS.node(), 5.0);
        node.set(SkillSetting.PERIOD.node(), 1000);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is once again vulnerable!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                        ChatComponents.GENERIC_SKILL + "%hero% is once again vulnerable!")
                .replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] strings) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        // Remove any harmful effects on the caster (from invuln)
        for (final Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.HARMFUL)) {
                hero.removeEffect(effect);
            }
        }

        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5000, false);
        final double radiusSquared = radius * radius;
        hero.addEffect(new RootEffect(this, player, 100, duration, null, null));
        hero.addEffect(new ManaPoolEffect(this, player, period, duration, radiusSquared));
        hero.addEffect(new InvulnStationaryEffect(this, player, duration, null, expireText));
        final Location location = player.getLocation().clone();
        VisualEffect.playInstantFirework(FireworkEffect.builder()
                .flicker(true)
                .trail(false)
                .with(FireworkEffect.Type.BURST)
                .withColor(Color.AQUA)
                .withFade(Color.TEAL)
                .build(), location.add(0, 2.0, 0));

        return SkillResult.NORMAL;
    }

    public static class ManaPoolEffect extends PeriodicExpirableEffect {

        private final double radiusSquared;
        private double manaCost;

        public ManaPoolEffect(final Skill skill, final Player applier, final long period, final long duration, final double rSquared) {
            super(skill, "ManaPoolEFfect", applier, period, duration);
            radiusSquared = rSquared;
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.AREA_OF_EFFECT);
            types.add(EffectType.MANA_INCREASING);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            // get mana from hero
            final double mana = hero.getMana();
            final double maxMana = hero.getMaxMana();
            final double manaPercent = mana / maxMana;
            manaCost = SkillConfigManager.getUseSetting(hero, skill, "mana-percent-cost", 0.40, false);

            // calculate mana cost
            if (manaPercent < manaCost) {
                manaCost = mana;
            } else {
                manaCost = (int) (manaCost * maxMana);
            }

            // apply mana cost
            final HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, (int) manaCost, skill);
            plugin.getServer().getPluginManager().callEvent(hrmEvent);
            if (!hrmEvent.isCancelled()) {
                hero.setMana((int) (mana - manaCost));
                if (hero.isVerboseMana()) {
                    hero.getPlayer().sendMessage(ChatComponents.Bars.mana(hero.getMana(), hero.getMaxMana(), true));
                }
            }
        }

        @Override
        public void tickMonster(final Monster monster) {

        }

        @Override
        public void tickHero(final Hero hero) {
            final Player player = hero.getPlayer();
            applyVisuals(player);
            final Location heroLoc = player.getLocation();

            if (hero.getParty() == null) {
                return;
            }

            final ArrayList<Hero> heroList = new ArrayList<>();
            for (final Hero partyHero : hero.getParty().getMembers()) {
                if (hero.getPlayer().equals(partyHero.getPlayer())) {
                    continue;
                } else if (!player.getWorld().equals(partyHero.getPlayer().getWorld())) {
                    continue;
                } else if ((partyHero.getPlayer().getLocation().distanceSquared(heroLoc) <= radiusSquared)) {
                    heroList.add(partyHero);
                }
            }

            if (heroList.size() == 0) {
                return;
            }

            // give them mana
            final int manaIncreaseAmount = (int) manaCost / heroList.size();
            for (final Hero heroChosen : heroList) {
                heroChosen.getPlayer().sendMessage(ChatComponents.GENERIC_SKILL + "You have received a gift of eir!");
                final HeroRegainManaEvent hrmEvent2 = new HeroRegainManaEvent(heroChosen, manaIncreaseAmount, skill);
                plugin.getServer().getPluginManager().callEvent(hrmEvent2);
                if (!hrmEvent2.isCancelled()) {
                    heroChosen.setMana(hrmEvent2.getDelta() + heroChosen.getMana());

                    if (hero.isVerboseMana()) {
                        heroChosen.getPlayer().sendMessage(ChatComponents.Bars.mana(heroChosen.getMana(), heroChosen.getMaxMana(), true));
                    }
                }
            }
        }

        private ArrayList<Location> circle(final Location centerPoint, final int particleAmount, final double circleRadius) {
            final World world = centerPoint.getWorld();

            final double increment = (2 * Math.PI) / particleAmount;

            final ArrayList<Location> locations = new ArrayList<>();

            for (int i = 0; i < particleAmount; i++) {
                final double angle = i * increment;
                final double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
                final double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
                locations.add(new Location(world, x, centerPoint.getY(), z));
            }
            return locations;
        }


        private void applyVisuals(final Player player) {
            for (double r = 1; r < radiusSquared; r++) {
                final ArrayList<Location> particleLocations = circle(player.getLocation(), 15, 5);
                for (final Location particleLocation : particleLocations) {
                    final Particle.DustOptions dustOptions = new Particle.DustOptions(Color.AQUA, 2);
                    player.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 1, 3, 0.2, 0.5, 0.2, dustOptions);
                }
            }
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.0F, 1.2F);

        }

    }

    public static class InvulnStationaryEffect extends ExpirableEffect {
        public InvulnStationaryEffect(final Skill skill, final Player applier, final long duration, final String applyText, final String expireText) {
            super(skill, "InvulnStationaryEffect", applier, duration, applyText, expireText);
            types.add(EffectType.INVULNERABILITY);
            types.add(EffectType.UNTARGETABLE);
            types.add(EffectType.UNBREAKABLE);
            types.add(EffectType.SILENCE);
            types.add(EffectType.ROOT);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
        }
    }
}

