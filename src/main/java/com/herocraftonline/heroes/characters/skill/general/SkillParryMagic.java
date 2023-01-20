package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


public class SkillParryMagic extends ActiveSkill {

    private String applyText;
    private String expireText;
    private String parrySkillText;

    public SkillParryMagic(final Heroes plugin) {
        super(plugin, "ParryMagic");
        setDescription("You parry the next magical attack within $1 second(s).");
        setUsage("/skill parrymagic");
        setArgumentRange(0, 0);
        setIdentifiers("skill parrymagic", "skill pmagic");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% raised their guard!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% lowered their guard!");
        node.set("parry-text", "%hero% parried an attack!");
        node.set("parry-skill-text", "%hero% has parried %target%'s %skill%.");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% raised their guard!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% lowered their guard!").replace("%hero%", "$1").replace("$hero$", "$1");
        parrySkillText = SkillConfigManager.getRaw(this, "parry-skill-text", "%hero% has parried %target%'s %skill%.").replace("%hero%", "$1").replace("%target%", "$2").replace("$target$", "$2").replace("%skill%", "$3");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        broadcastExecuteText(hero);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        hero.addEffect(new ParryMagicEffect(this, hero.getPlayer(), duration));

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 1.0F);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }

    public class ParryMagicEffect extends ExpirableEffect {

        public ParryMagicEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, "ParryMagic", applier, duration);
            this.types.add(EffectType.MAGIC);
            this.types.add(EffectType.BENEFICIAL);
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

    public class SkillEntityListener implements Listener {

        private final Skill skill;

        SkillEntityListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler()
        public void onSkillDamage(final SkillDamageEvent event) {
            // Ignore cancelled damage events & 0 damage events for Spam Control
            if (event.getDamage() == 0 || event.isCancelled() || !event.getSkill().isType(SkillType.ABILITY_PROPERTY_MAGICAL) || !(event.getEntity() instanceof Player)) {
                return;
            }
            final Player player = (Player) event.getEntity();
            final Hero hero = plugin.getCharacterManager().getHero(player);

            final Effect parry = hero.getEffect(getName());
            if (parry != null) {
                parry.removeFromHero(hero);
                event.setCancelled(true);
                final String message = parrySkillText.replace("%hero%", player.getName()).replace("%target%", CustomNameManager.getName(event.getDamager())).replace("%skill%", event.getSkill().getName());
                player.sendMessage(message);
                if (event.getDamager() instanceof Hero) {
                    ((Hero) event.getDamager()).getPlayer().sendMessage(message);
                }

            }
        }
    }
}