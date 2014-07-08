package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillDevourMagic extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillDevourMagic(Heroes plugin) {
        super(plugin, "DevourMagic");
        setDescription("Description: Devour harmful magic targeted on you for $1 seconds, reducing any incoming spell damage by $2% and restoring mana based on the resisted portion at a $3% rate.");
        setUsage("/skill devourmagic");
        setArgumentRange(0, 0);
        setIdentifiers("skill devourmagic");
        setTypes(SkillType.MANA_INCREASING, SkillType.SILENCEABLE, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_DARK);

        Bukkit.getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double duration = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false) / 1000.0);
        double resistValue = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, "resist-value", 0.2, false) * 100.0);
        double manaConversionRate = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, "mana-per-damage", 0.8, false) * 100.0);

        return getDescription().replace("$1", duration + "").replace("$2", resistValue + "").replace("$3", manaConversionRate + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 5000);
        node.set("resist-value", 0.2);
        node.set("mana-per-damage", 0.8);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is devouring incoming magic!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is no longer devouring magic.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% is devouring incoming magic!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% is no longer devouring magic.").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {

        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
        player.getWorld().playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.5F, 2.0F);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        hero.addEffect(new DevourMagicEffect(this, player, duration));

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {
        private final Skill skill;

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            Skill eventSkill = event.getSkill();
            if (eventSkill.isType(SkillType.ABILITY_PROPERTY_PHYSICAL) || !eventSkill.isType(SkillType.DAMAGING))
                return;

            if (!(event.getEntity() instanceof Player))
                return;

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("DevourMagic")) {
                double resistValue = 1.0 - SkillConfigManager.getUseSetting(hero, skill, "resist-value", 0.2, false);
                double newDamage = event.getDamage() * resistValue;

                // Give them mana
                double manaConversionRate = SkillConfigManager.getUseSetting(hero, skill, "mana-per-damage", 0.8, false);
                int manaRegain = (int) ((event.getDamage() - newDamage) * manaConversionRate);
                hero.setMana(hero.getMana() + manaRegain);

                // Reduce damage
                event.setDamage(newDamage);
            }
        }
    }

    public class DevourMagicEffect extends ExpirableEffect {
        public DevourMagicEffect(Skill skill, Player applier, long duration) {
            super(skill, "DevourMagic", applier, duration, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DARK);
            types.add(EffectType.MAGIC);
            types.add(EffectType.DISPELLABLE);
        }
    }
}
