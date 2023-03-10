package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
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
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillFireblade extends ActiveSkill {

    private String applyText;
    private String expireText;
    private String igniteText;

    public SkillFireblade(final Heroes plugin) {
        super(plugin, "Fireblade");
        setDescription("Your attacks have a $1% chance to ignite their target.");
        setUsage("/skill fireblade");
        setArgumentRange(0, 0);
        setIdentifiers("skill fireblade");
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
        node.set("ignite-text", "%hero% has lit %target% aflame!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero%'s weapon is sheathed in flame!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero%'s weapon is no longer aflame!").replace("%hero%", "$1").replace("$hero$", "$1");
        igniteText = SkillConfigManager.getRaw(this, "ignite-text", "%hero% has lit %target% aflame!");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
        hero.addEffect(new FirebladeEffect(this, hero.getPlayer(), duration));

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.BLOCK_ANVIL_USE, 0.6F, 1.0F);
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

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(final WeaponDamageEvent event) {
            if (!(event.getEntity() instanceof LivingEntity) || !(event.getDamager() instanceof Hero) || event.getDamage() == 0 || event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                return; // With this, we know a Hero damaged some other sort of LivingEntity with damage that counts as ENTITY_ATTACK (i.e. not a Bow or a Skill)
            }

            final Hero hero = (Hero) event.getDamager();
            final Player player = hero.getPlayer();
            if (!hero.hasEffect("Fireblade") || !SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.swords).contains(NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType().name())) {
                return; // With this, we know this Hero has the FireBlade Effect and is holding a suitable weapon.
            }

            final double chance = SkillConfigManager.getUseSetting(hero, skill, "ignite-chance", .2, false);
            if (Util.nextRand() >= chance) {
                return; // With this, we know the RNG roll for the Fire effect has passed.
            }

            final int fireTicks = SkillConfigManager.getUseSetting(hero, skill, "ignite-duration", 5000, false) / 50;

            final LivingEntity target = (LivingEntity) event.getEntity();
            target.setFireTicks(fireTicks);
            plugin.getCharacterManager().getCharacter(target).addEffect(new CombustEffect(skill, player));

            broadcast(player.getLocation(), igniteText.replace("%hero%", player.getName()).replace("%target%", CustomNameManager.getName(target)));
        }
    }

    public class FirebladeEffect extends ExpirableEffect {

        public FirebladeEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, "Fireblade", applier, duration, applyText, expireText); //TODO Implicit broadcast() call - may need changes?
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.FIRE);
        }
    }
}
