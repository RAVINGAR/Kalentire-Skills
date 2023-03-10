package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CustomNameManager;
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
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillSoulFire extends ActiveSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    private String applyText;
    private String expireText;
    private String igniteText;

    public SkillSoulFire(final Heroes plugin) {
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
        final ConfigurationSection node = super.getDefaultConfig();
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
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero%'s weapon is sheathed in flame!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero%'s weapon is no longer aflame!").replace("%hero%", "$1").replace("$hero$", "$1");
        igniteText = SkillConfigManager.getRaw(this, "ignite-text", "%hero% has lit %target% on fire with soulfire!");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
        hero.addEffect(new SoulFireEffect(this, hero.getPlayer(), duration));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5F, 1.0F);
        // this is our fireworks shit
        final Player player = hero.getPlayer();
        /*try {
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
        }*/
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(final Hero hero) {
        final double chance = SkillConfigManager.getUseSetting(hero, this, "ignite-chance", .2, false);
        return getDescription().replace("$1", Util.stringDouble(chance * 100));
    }

    public class SkillDamageListener implements Listener {

        private final Skill skill;

        public SkillDamageListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(final EntityDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof LivingEntity) || !(event instanceof EntityDamageByEntityEvent) || event.getDamage() == 0) {
                return;
            }

            final EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            if (!(subEvent.getDamager() instanceof Player)) {
                return;
            }

            final Player player = (Player) subEvent.getDamager();
            final Hero hero = plugin.getCharacterManager().getHero(player);
            if (!SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.swords).contains(NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType().name()) || !hero.hasEffect("SoulFire")) {
                return;
            }

            final double chance = SkillConfigManager.getUseSetting(hero, skill, "ignite-chance", .2, false);
            if (Util.nextRand() >= chance) {
                return;
            }

            final int fireTicks = SkillConfigManager.getUseSetting(hero, skill, "ignite-duration", 5000, false) / 50;
            final LivingEntity target = (LivingEntity) event.getEntity();
            target.setFireTicks(fireTicks);
            plugin.getCharacterManager().getCharacter(target).addEffect(new CombustEffect(skill, player));
            broadcast(player.getLocation(), igniteText.replace("%hero%", player.getName()).replace("%target%", CustomNameManager.getName(target)));
        }
    }

    public class SoulFireEffect extends ExpirableEffect {

        public SoulFireEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, "SoulFire", applier, duration, applyText, expireText); //TODO Implicit broadcast() call - may need changes?
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.FIRE);
        }
    }
}
