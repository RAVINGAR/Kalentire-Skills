package com.herocraftonline.heroes.characters.skill.remastered.ranger;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.common.SpeedEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import com.herocraftonline.heroes.attributes.AttributeType;

public class SkillNimbleDraw extends ActiveSkill {

    public SkillNimbleDraw(Heroes plugin) {
        super(plugin, "NimbleDraw");
        setDescription("You are able to move quickly while drawing bows.");
        setUsage("/skill NimbleDraw");
        setArgumentRange(0, 0);
        setIdentifiers("skill nimbledraw");
        setTypes(SkillType.BUFFING, SkillType.MOVEMENT_INCREASING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, (Skill)this, SkillSetting.DURATION, 10000, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0D);
        return getDescription().replace("$1", formattedDuration);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", 12);
        node.set(SkillSetting.DURATION.node(), 20000);
        node.set(SkillSetting.COOLDOWN.node(), 1200);
        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        //FIXME If possible (not sure it is entirely), make this speed boost only effective if the user is drawing a bow.
        // E.g. possibly using interact event? see https://www.spigotmc.org/threads/detect-if-the-player-is-pulling-their-bow-back.158483
        // Or see https://www.spigotmc.org/threads/1-12-2-serverbound-packet-for-cancel-bow-draw.332520/
        // As without this, it is just a (slight?) speed boosting effect requiring you to hold a bow at first.

        Player player = hero.getPlayer();
        if(player.getInventory().getItemInMainHand().getType() == Material.BOW)
        {
            int duration = SkillConfigManager.getUseSetting(hero, SkillNimbleDraw.this, SkillSetting.DURATION, 10000, false);
            int multiplier = SkillConfigManager.getUseSetting(hero, SkillNimbleDraw.this, "speed-multiplier", 12, false);
            NimbleDrawEffect nimEffect = new NimbleDrawEffect(SkillNimbleDraw.this, player, duration, multiplier);
            hero.addEffect(nimEffect);
            return SkillResult.NORMAL;
        }
        player.sendMessage("You cannot use this skill with that weapon!");
        return SkillResult.FAIL;
    }

    public class SkillEntityListener implements Listener {

        public double getSpeed(double tox, double fromx, double toz, double fromz)
        {
            double diffx = (tox - fromx);
            double diffz = (toz - fromz);
            if(diffx < 0) {
                diffx = diffx * -1;
            }
            if(diffz < 0) {
                diffz = diffz * -1;
            }
            return diffx + diffz;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
        public void onMove(PlayerMoveEvent event)
        {
            Player player = event.getPlayer();
            Hero hero = SkillNimbleDraw.this.plugin.getCharacterManager().getHero(player);
            if (!hero.hasEffect("NimbleDraw"))
                return; // Skip players without this effect. This event will run frequently, and we want to reduce the time we're here.

            final Location from = event.getFrom();
            final Location to = event.getTo();
            if (to == null)
                return; // Not sure what this means, not moving? But we can't check anyway, so skip.

            int multiplier = SkillConfigManager.getUseSetting(hero, SkillNimbleDraw.this, "speed-multiplier", 12, false);
            double speedMult = multiplier * 0.2 + 1;
            if (getSpeed(to.getX(), from.getX(), to.getZ(), from.getZ()) > ((speedMult * 0.117) + (hero.getAttributeValue(AttributeType.DEXTERITY))*0.00152)) {
                hero.removeEffect(hero.getEffect("NimbleDraw"));
            }
        }
    }
    public class NimbleDrawEffect extends SpeedEffect {
        public NimbleDrawEffect(Skill skill, Player applier, int duration, int multiplier) {
            super(skill, "NimbleDraw", applier, duration, multiplier, null, null);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        public void removeFromHero(final Hero hero) {
            Player player = hero.getPlayer();
            if (player.hasPotionEffect(PotionEffectType.POISON) || player.hasPotionEffect(PotionEffectType.WITHER) || player
                    .hasPotionEffect(PotionEffectType.HARM)) {
                Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable() {
                    public void run() {
                        SkillNimbleDraw.NimbleDrawEffect.this.removeFromHero(hero);
                    }
                },  2L);
            } else {
                super.removeFromHero(hero);
            }
        }
    }
}