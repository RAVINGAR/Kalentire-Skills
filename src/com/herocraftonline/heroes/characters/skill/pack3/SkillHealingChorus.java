package com.herocraftonline.heroes.characters.skill.pack3;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;

public class SkillHealingChorus extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillHealingChorus(Heroes plugin) {
        super(plugin, "HealingChorus");
        setDescription("You sing a chorus of healing, affecting party members within $1 blocks. The chorus heals them for $2 health over $3 seconds. You are only healed for $4 health from this effect.");
        setUsage("/skill healingchorus");
        setIdentifiers("skill healingchorus");
        setTypes(SkillType.AREA_OF_EFFECT, SkillType.BUFFING, SkillType.HEALING, SkillType.ABILITY_PROPERTY_SONG);
        setArgumentRange(0, 0);
    }

    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 15, false);

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 3000, false);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 17, false);
        healing = getScaledHealing(hero, healing);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_CHARISMA, 0.175, false);
        healing += (hero.getAttributeValue(AttributeType.CHARISMA) * healingIncrease);

        String formattedHealing = Util.decFormat.format(healing * ((double) duration / (double) period));
        String formattedSelfHealing = Util.decFormat.format((healing * ((double) duration / (double) period)) * Heroes.properties.selfHeal);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", radius + "").replace("$2", formattedHealing).replace("$3", formattedDuration).replace("$4", formattedSelfHealing);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 3000);
        node.set(SkillSetting.RADIUS.node(), 12);
        node.set(SkillSetting.PERIOD.node(), 1500);
        node.set(SkillSetting.HEALING_TICK.node(), 17);
        node.set(SkillSetting.HEALING_INCREASE_PER_CHARISMA.node(), 0.175);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "You are gifted with %hero%'s chorus of healing.");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s chorus of healing has ended.");
        node.set(SkillSetting.DELAY.node(), 1000);

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 15, false);
        int radiusSquared = radius * radius;
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 17, false);
        healing = getScaledHealing(hero, healing);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_CHARISMA, 0.175, false);
        healing += (hero.getAttributeValue(AttributeType.CHARISMA) * healingIncrease);

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 3000, false);

        broadcastExecuteText(hero);

        // Check if the hero has a party
        if (hero.hasParty()) {
            Location playerLocation = player.getLocation();
            // Loop through the player's party members and add the effect as necessary
            for (Hero member : hero.getParty().getMembers()) {
                // Ensure the party member is in the same world.
                if (member.getPlayer().getLocation().getWorld().equals(playerLocation.getWorld())) {
                    // Check to see if they are close enough to the player to receive the buff
                    if (member.getPlayer().getLocation().distanceSquared(playerLocation) <= radiusSquared) {
                        // Add the effect
                        member.addEffect(new HealingChorusEffect(this, player, period, duration, healing));
                    }
                }
            }
        }
        else {
            // Add the effect to just the player
            hero.addEffect(new HealingChorusEffect(this, player, period, duration, healing));
        }

        //FIXME No idea what to do here
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);

        return SkillResult.NORMAL;
    }

    public class HealingChorusEffect extends PeriodicHealEffect
    {

        public HealingChorusEffect(Skill skill, Player applier, long period, long duration, double healing) {
            super(skill, "HealingChorus", applier, period, duration, healing, null, null);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();
            final Player p = player;

            if (player == this.getApplier())
            {
                new BukkitRunnable() {

                    private double time = 0;

                    @Override
                    public void run()
                    {
                        Location location = p.getLocation();
                        if (time < 0.5)
                        {
                            //p.getWorld().spigot().playEffect(location, Effect.NOTE, 0, 0, 6.3F, 1.0F, 6.3F, 0.0F, 1, 16);
                            p.getWorld().spawnParticle(Particle.NOTE, location, 1, 6.3, 1, 6.3, 1);
                        }
                        else
                        {
                            cancel();
                        }
                        time += 0.01;
                    }
                }.runTaskTimer(plugin, 1, 4);
            }

            player.sendMessage("    " + applyText.replace("%hero%", applier.getName()));
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();

            player.sendMessage("    " + expireText.replace("%hero%", applier.getName()));
        }

        public void tickHero(Hero hero)
        {
            super.tickHero(hero);
        }
    }
}