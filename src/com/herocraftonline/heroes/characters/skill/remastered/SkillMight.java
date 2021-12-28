package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillMight extends ActiveSkill {
    public static final String SKILL_MESSAGE_PREFIX_SPACES = "    ";

    private String applyText;
    private String expireText;

    public SkillMight(Heroes plugin) {
        super(plugin, "Might");
        setDescription("You increase your party's damage with weapons by $1%!");
        setArgumentRange(0, 0);
        setUsage("/skill might");
        setIdentifiers("skill might");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double bonus = SkillConfigManager.getUseSetting(hero, this, "damage-bonus", 1.20, false);

        String formattedBonus = Util.decFormat.format((bonus - 1) * 100);

        return getDescription().replace("$1", formattedBonus);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("damage-bonus", 1.20);
        node.set(SkillSetting.COOLDOWN.node(), 60000);
        node.set(SkillSetting.RADIUS.node(), 10);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "Your muscles bulge with power!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "You feel strength leave your body!");
        node.set(SkillSetting.DURATION.node(), 180000);

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SKILL_MESSAGE_PREFIX_SPACES + SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "Your muscles bulge with power!");
        expireText = SKILL_MESSAGE_PREFIX_SPACES + SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "You feel strength leave your body!");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        double damageBonus = SkillConfigManager.getUseSetting(hero, this, "damage-bonus", 1.20, false);

        MightEffect mEffect = new MightEffect(this, player, duration, damageBonus);
        if (!hero.hasParty()) {
            if (hero.hasEffect("Might")) {
                if (((MightEffect) hero.getEffect("Might")).getDamageBonus() > mEffect.getDamageBonus()) {
                    player.sendMessage("You have a more powerful effect already!");
                }
            }
            hero.addEffect(mEffect);
        } else {
            double range = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
            double rangeSquared = range * range;
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

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.0F);

        return SkillResult.NORMAL;
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
                event.setDamage((event.getDamage() * damageBonus));
            }
        }
    }

    public class MightEffect extends ExpirableEffect {

        private final double damageBonus;

        public MightEffect(Skill skill, Player applier, long duration, double damageBonus) {
            super(skill, "Might", applier, duration);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);

            this.damageBonus = damageBonus;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            player.sendMessage(applyText);
        }

        public double getDamageBonus() {
            return damageBonus;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            player.sendMessage(expireText);
        }
    }

}
