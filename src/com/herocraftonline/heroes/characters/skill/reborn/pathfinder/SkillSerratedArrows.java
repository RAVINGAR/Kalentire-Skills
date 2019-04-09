package com.herocraftonline.heroes.characters.skill.reborn.pathfinder;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.Expirable;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

import javax.swing.text.html.parser.Entity;

public class SkillSerratedArrows extends PassiveSkill {

    public VisualEffect fplayer = new VisualEffect();
    public SkillSerratedArrows(Heroes plugin) {
        super(plugin, "SerratedArrows");
        setDescription("Every %1% arrow you fire will shoot a Serrated Arrow, which will deal bonus damage and pierce through your targets Armor");
        setUsage("/skill serratedarrows");
        setArgumentRange(0, 0);
        setIdentifiers("skill serratedarrows");
        setTypes(SkillType.DAMAGING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$2", formattedDamage);
    }





    public class SkillDamageListener implements Listener {

        private Skill skill;
        private int hitCount;

        SkillDamageListener(Skill skill) {
            this.skill = skill;
        }


        public void onEntityShootBow(EntityShootBowEvent event) {

        }


        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageByEntityEvent event) {


            if(!(event.getDamager() instanceof Arrow)) {
                return;
            }

            Arrow arrow = (Arrow) event.getDamager();
            if (!(arrow.getShooter() instanceof Player)) {
                return;
            }



            final Player player = (Player) arrow.getShooter();
            final Hero hero = plugin.getCharacterManager().getHero(player);
            double damage = SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.DAMAGE, 90, false);

            if(!hero.hasEffect("SerratedArrows")) {
                return;
            }

            if(hero.hasEffect("SerratedArrows")) {
                hitCount++;
                player.sendMessage("hit");
            }

            if(hitCount == 3) {
                final LivingEntity target = (LivingEntity) event.getEntity();
                addSpellTarget(target, hero);
                damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC, true);
                target.sendMessage("fuckyou");
                player.sendMessage("hi cutie");
                        try {
            fplayer.playFirework(player.getWorld(), player.getLocation().add(0,2.0,0),
            		FireworkEffect.builder().flicker(false).trail(false)
            		.with(FireworkEffect.Type.BURST)
            		.withColor(Color.WHITE)
            		.withFade(Color.GREEN)
            		.build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
            hitCount = 0;
            }

        }


    }


}
