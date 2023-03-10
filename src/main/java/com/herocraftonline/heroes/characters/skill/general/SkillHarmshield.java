package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

public class SkillHarmshield extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillHarmshield(final Heroes plugin) {
        super(plugin, "HarmShield");
        setDescription("Shields you from harm, reducing damage by $1% for $2 seconds");
        setUsage("/skill harmshield");
        setArgumentRange(0, 0);
        setIdentifiers("skill harmshield");
        setTypes(SkillType.BUFFING, SkillType.SILENCEABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false) / 1000;
        float damageReduction = (float) SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 0.2, false);
        damageReduction *= 100F;
        damageReduction = 100F - damageReduction;

        return getDescription().replace("$1", damageReduction + "").replace("$2", duration + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.USE_TEXT.node(), "");
        node.set("damage-multiplier", 0.2);
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is shielded from harm!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% lost his harm shield!");

        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is shielded from harm!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% lost his harm shield!").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();
        broadcastExecuteText(hero);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.0F);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        hero.addEffect(new HarmShieldEffect(this, player, duration));

        final List<Location> locations = GeometryUtil.circle(player.getLocation(), 72, 1.5);
        for (final Location location : locations) {
            //player.getWorld().spigot().playEffect(locations.get(i), org.bukkit.Effect.WITCH_MAGIC, 0, 0, 0, 1.2F, 0, 0, 1, 16);
            player.getWorld().spawnParticle(Particle.SPELL_WITCH, location, 1, 0, 1.2, 0);
        }

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        private final Skill skill;

        public SkillHeroListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGH)
        public void onSkillDamage(final SkillDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
                return;
            }

            event.setDamage(getAdjustment((Player) event.getEntity(), event.getDamage()));
        }

        @EventHandler(priority = EventPriority.HIGH)
        public void onWeaponDamage(final WeaponDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
                return;
            }

            event.setDamage(getAdjustment((Player) event.getEntity(), event.getDamage()));
        }

        private double getAdjustment(final Player player, double d) {
            final Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect("HarmShield")) {
                d *= SkillConfigManager.getUseSetting(hero, skill, "damage-multiplier", 0.2, false);
            }

            return d;
        }
    }

    public class HarmShieldEffect extends ExpirableEffect {

        public HarmShieldEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, "HarmShield", applier, duration);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }
    }
}
