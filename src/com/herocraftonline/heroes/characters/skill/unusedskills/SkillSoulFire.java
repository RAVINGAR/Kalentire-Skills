package com.herocraftonline.heroes.characters.skill.unusedskills;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillSoulFire extends ActiveSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    private String applyText;
    private String expireText;
    private String igniteText;

    public SkillSoulFire(Heroes plugin) {
        super(plugin, "SoulFire");
        setDescription("Your attacks have a $1% chance to ignite their target.");
        setUsage("/skill soulfire");
        setArgumentRange(0, 0);
        setIdentifiers("skill soulfire");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.BUFFING, SkillType.SILENCEABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("weapons", Util.swords);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero%'s weapon is sheathed in flame!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero%'s weapon is no longer aflame!");
        node.set(SkillSetting.DURATION.node(), 600000);
        node.set("ignite-chance", 0.20);
        node.set("ignite-duration", 5000);
        node.set("ignite-text", "%hero% has lit %target% on fire with soulfire!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero%'s weapon is sheathed in flame!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero%'s weapon is no longer aflame!").replace("%hero%", "$1");
        igniteText = SkillConfigManager.getRaw(this, "ignite-text", "%hero% has lit %target% on fire with soulfire!").replace("%hero%", "$1").replace("%target%", "$2");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
        hero.addEffect(new SoulFireEffect(this, hero.getPlayer(), duration));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ZOMBIE_REMEDY , 0.5F, 1.0F); 
        // this is our fireworks shit
        Player player = hero.getPlayer();
        try {
            fplayer.playFirework(player.getWorld(), player.getLocation().add(0,1.5,0), 
            		FireworkEffect.builder().flicker(false).trail(false)
            		.with(FireworkEffect.Type.STAR)
            		.withColor(Color.RED)
            		.withFade(Color.FUCHSIA)
            		.build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }

    public class SkillDamageListener implements Listener {

        private final Skill skill;

        public SkillDamageListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof LivingEntity) || !(event instanceof EntityDamageByEntityEvent) || event.getDamage() == 0) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            if (!(subEvent.getDamager() instanceof Player)) {
                return;
            }

            Player player = (Player) subEvent.getDamager();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (!SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.swords).contains(player.getItemInHand().getType().name()) || !hero.hasEffect("SoulFire")) {
                return;
            }

            double chance = SkillConfigManager.getUseSetting(hero, skill, "ignite-chance", .2, false);
            if (Util.nextRand() >= chance) {
                return;
            }

            int fireTicks = SkillConfigManager.getUseSetting(hero, skill, "ignite-duration", 5000, false) / 50;
            LivingEntity target = (LivingEntity) event.getEntity();
            target.setFireTicks(fireTicks);
            plugin.getCharacterManager().getCharacter(target).addEffect(new CombustEffect(skill, player));
            broadcast(player.getLocation(), igniteText, player.getName(), Messaging.getLivingEntityName(target));
        }
    }

    public class SoulFireEffect extends ExpirableEffect {

        public SoulFireEffect(Skill skill, Player applier, long duration) {
            super(skill, "SoulFire", applier, duration);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.FIRE);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double chance = SkillConfigManager.getUseSetting(hero, this, "ignite-chance", .2, false);
        return getDescription().replace("$1", Util.stringDouble(chance * 100));
    }
}
