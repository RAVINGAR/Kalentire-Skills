package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillWarsong extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillWarsong(Heroes plugin) {
        super(plugin, "Warsong");
        setDescription("You increase your party's damage with weapons by $1%!");
        setArgumentRange(0, 0);
        setUsage("/skill warsong");
        setIdentifiers("skill warsong");
        setTypes(SkillType.BUFF, SkillType.SILENCABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("damage-bonus", 1.25);
        node.set(Setting.RADIUS.node(), 10);
        node.set(Setting.APPLY_TEXT.node(), "Your muscles bulge with power!");
        node.set(Setting.EXPIRE_TEXT.node(), "You feel strength leave your body!");
        node.set(Setting.DURATION.node(), 600000); // in Milliseconds - 10 minutes
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "Your muscles bulge with power!");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "You feel strength leave your body!");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 600000, false);
        double damageBonus = SkillConfigManager.getUseSetting(hero, this, "damage-bonus", 1.25, false);

        MightEffect mEffect = new MightEffect(this, duration, damageBonus);
        if (!hero.hasParty()) {
            if (hero.hasEffect("Might")) {
                if (((MightEffect) hero.getEffect("Might")).getDamageBonus() > mEffect.getDamageBonus()) {
                    Messaging.send(player, "You have a more powerful effect already!");
                }
            }
            hero.addEffect(mEffect);
        } else {
            int range = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 10, false);
            int rangeSquared = range * range;
            Location loc = player.getLocation();
            for (Hero pHero : hero.getParty().getMembers()) {
                Player pPlayer = pHero.getPlayer();
                if (!pPlayer.getWorld().equals(player.getWorld())) {
                    continue;
                }
                if (pPlayer.getLocation().distanceSquared(loc) > rangeSquared) {
                    continue;
                }
                if (pHero.hasEffect("Might")) {
                    if (((MightEffect) pHero.getEffect("Might")).getDamageBonus() > mEffect.getDamageBonus()) {
                        continue;
                    }
                }
                pHero.addEffect(mEffect);
            }
        }

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class MightEffect extends ExpirableEffect {

        private final double damageBonus;

        public MightEffect(Skill skill, long duration, double damageBonus) {
            super(skill, "Might", duration);
            this.damageBonus = damageBonus;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            Messaging.send(player, applyText);
        }

        public double getDamageBonus() {
            return damageBonus;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            Messaging.send(player, expireText);
        }
    }

    public class SkillHeroListener implements Listener {

        @EventHandler()
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getCause() != DamageCause.ENTITY_ATTACK) {
                return;
            }

            CharacterTemplate character = event.getDamager();
            if (character.hasEffect("Might")) {
                double damageBonus = ((MightEffect) character.getEffect("Might")).damageBonus;
                event.setDamage((int) (event.getDamage() * damageBonus));
            }           
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double bonus = SkillConfigManager.getUseSetting(hero, this, "damage-bonus", 1.25, false);
        return getDescription().replace("$1", Util.stringDouble((bonus - 1) * 100));
    }
}
