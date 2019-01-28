package com.herocraftonline.heroes.characters.skill.public1;

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
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillMight extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillMight(Heroes plugin) {
        super(plugin, "Might");
        this.setDescription("You increase your party's damage with weapons by $1%!");
        this.setArgumentRange(0, 0);
        this.setUsage("/skill might");
        this.setIdentifiers("skill might");
        this.setTypes(SkillType.BUFFING, SkillType.SILENCEABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("damage-bonus", 1.25);
        node.set(SkillSetting.RADIUS.node(), 10);
        node.set(SkillSetting.APPLY_TEXT.node(), "Your muscles bulge with power!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "You feel strength leave your body!");
        node.set(SkillSetting.DURATION.node(), 600000); // in Milliseconds - 10 minutes
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "Your muscles bulge with power!");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "You feel strength leave your body!");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
        final double damageBonus = SkillConfigManager.getUseSetting(hero, this, "damage-bonus", 1.25, false);

        final MightEffect mEffect = new MightEffect(this, player, duration, damageBonus);
        if (!hero.hasParty()) {
            if (hero.hasEffect("Might")) {
                if (((MightEffect) hero.getEffect("Might")).getDamageBonus() > mEffect.getDamageBonus()) {
                    player.sendMessage(ChatColor.GRAY + "You have a more powerful effect already!");
                }
            }
            hero.addEffect(mEffect);
        } else {
            final int range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);
            final int rangeSquared = range * range;
            final Location loc = player.getLocation();
            for (final Hero pHero : hero.getParty().getMembers()) {
                final Player pPlayer = pHero.getPlayer();
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

        this.broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class MightEffect extends ExpirableEffect {

        private final double damageBonus;

        public MightEffect(Skill skill, Player applier, long duration, double damageBonus) {
            super(skill, "Might", applier, duration);
            this.damageBonus = damageBonus;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            player.sendMessage(ChatColor.GRAY + SkillMight.this.applyText);
        }

        public double getDamageBonus() {
            return this.damageBonus;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            player.sendMessage(ChatColor.GRAY + SkillMight.this.expireText);
        }
    }

    public class SkillHeroListener implements Listener {

        @EventHandler()
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getCause() != DamageCause.ENTITY_ATTACK) {
                return;
            }

            final CharacterTemplate character = event.getDamager();
            if (character.hasEffect("Might")) {
                final double damageBonus = ((MightEffect) character.getEffect("Might")).damageBonus;
                event.setDamage(event.getDamage() * damageBonus);
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        final double bonus = SkillConfigManager.getUseSetting(hero, this, "damage-bonus", 1.25, false);
        return this.getDescription().replace("$1", Util.stringDouble((bonus - 1) * 100));
    }
}
