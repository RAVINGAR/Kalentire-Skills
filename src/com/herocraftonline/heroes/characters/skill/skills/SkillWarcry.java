package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.QuickenEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillWarcry
        extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillWarcry(Heroes plugin) {
        super(plugin, "Warcry");
        setDescription("The Barbarian readies himself for battle, increasing his movement speed and armor for $1 second(s).");
        setUsage("/skill warcry");
        setArgumentRange(0, 0);
        setIdentifiers(new String[]{"skill warcry"});
        setTypes(new SkillType[]{SkillType.BUFFING, SkillType.MOVEMENT_INCREASING, SkillType.SILENCEABLE});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", Integer.valueOf(2));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(10000));
        node.set(SkillSetting.MANA.node(), Integer.valueOf(75));
        node.set(SkillSetting.COOLDOWN.node(), Integer.valueOf(60));
        node.set("apply-text", "%hero% gained a burst of speed and armor!");
        node.set("expire-text", "%hero% returned to normal speed and armor!");
        return node;
    }

    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% gained a burst of speed and armor!").replace("%hero%", "$1");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% returned to normal armor!").replace("%hero%", "$1");

    }

    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
        if (multiplier > 20) {
            multiplier = 20;
        }

        hero.addEffect(new QuickenEffect(this, getName(), player, duration, multiplier, this.applyText, this.expireText));
        hero.addEffect(new armorEffect(this, player, duration));
        return SkillResult.NORMAL;
    }

    public class armorEffect
            extends ExpirableEffect {
        public armorEffect(Skill skill, Player applier, long duration) {
            super(skill, "Armor", applier, duration);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.DISPELLABLE);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

        }


        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();

        }


    }

    public class SkillListener
            implements Listener {
        private final Skill skill;

        public SkillListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                return;
            }
            Entity e = event.getEntity();
            if (!(e instanceof Player))
                return;
            Player p = (Player) e;
            Hero hero = SkillWarcry.this.plugin.getCharacterManager().getHero(p);
            if (hero.hasEffect("Armor")) {

                event.setDamage(event.getDamage() - (event.getDamage() * .12));

            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 75, false);
        int cd = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 60, false);
        return getDescription().replace("$1", duration / 1000 + "").replace("$2", mana + "").replace("$3", cd + "");
    }
}
