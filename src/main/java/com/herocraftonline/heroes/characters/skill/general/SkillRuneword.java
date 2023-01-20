package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class SkillRuneword extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillRuneword(final Heroes plugin) {
        super(plugin, "Runeword");
        setDescription("Your target takes $1% more magic damage!");
        setArgumentRange(0, 0);
        setUsage("/skill runeword");
        setIdentifiers("skill runeword");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.DEBUFFING, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double bonus = SkillConfigManager.getUseSetting(hero, this, "damage-bonus", 1.25, false);
        return getDescription().replace("$1", Math.round((bonus - 1D) * 100D) + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set("damage-bonus", 1.2);
        node.set(SkillSetting.DURATION.node(), 20000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been cursed by a Runeword!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "The Runeword's curse fades from %target%!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% has been cursed by a Runeword!").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "The Runeword's curse fades from %target%!").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        broadcastExecuteText(hero, target);
        final Player player = hero.getPlayer();

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
        final double damageBonus = SkillConfigManager.getUseSetting(hero, this, "damage-bonus", 1.25, false);

        final RunewordEffect effect = new RunewordEffect(this, player, duration, damageBonus);
        plugin.getCharacterManager().getCharacter(target).addEffect(effect);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5F, 1.0F);
        //player.getWorld().spigot().playEffect(player.getLocation(), Effect.WITCH_MAGIC, 1, 1, 0.1F, 1.0F, 0.1F, 0.1F, 30, 10);
        player.getWorld().spawnParticle(Particle.SPELL_WITCH, player.getLocation(), 30, 0.1, 1, 0.1, 0.1);

        final List<Location> circle = GeometryUtil.circle(target.getLocation(), 36, 1.5);
        for (final Location location : circle) {
            //target.getWorld().spigot().playEffect(circle(target.getLocation().add(0, 1, 0), 36, 1.5).get(i), org.bukkit.Effect.PORTAL, 0, 0, 0.2F, 1.0F, 0.2F, 0.4F, 10, 16);
            target.getWorld().spawnParticle(Particle.PORTAL, location, 10, 0.2, 1, 0.2, 0.4);
            //target.getWorld().spigot().playEffect(circle(target.getLocation().add(0, 1, 0), 36, 1.5).get(i), org.bukkit.Effect.FLYING_GLYPH, 0, 0, 0.2F, 1.0F, 0.2F, 0.2F, 10, 16);
            target.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, location, 10, 0.2, 1, 0.2, 0.2);
        }

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        @EventHandler
        public void onSkillDamage(final SkillDamageEvent event) {
            final Skill eventSkill = event.getSkill();
            if (eventSkill.isType(SkillType.ABILITY_PROPERTY_PHYSICAL) || !eventSkill.isType(SkillType.DAMAGING)) {
                return;
            }
            final CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            if (character.hasEffect("Runeword")) {
                final double damageBonus = ((RunewordEffect) character.getEffect("Runeword")).damageBonus;
                event.setDamage((event.getDamage() * damageBonus));
            }
        }
    }

    public class RunewordEffect extends ExpirableEffect {

        private final double damageBonus;

        public RunewordEffect(final Skill skill, final Player applier, final long duration, final double damageBonus) {
            super(skill, "Runeword", applier, duration);
            this.damageBonus = damageBonus;

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        public double getDamageBonus() {
            return damageBonus;
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), "    " + expireText, player.getName());
        }
    }
}
