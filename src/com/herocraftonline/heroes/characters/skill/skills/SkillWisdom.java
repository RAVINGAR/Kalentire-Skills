package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
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

public class SkillWisdom extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillWisdom(Heroes plugin) {
        super(plugin, "Wisdom");
        setDescription("You party benefits from $1% increased mana regeneration.");
        setArgumentRange(0, 0);
        setUsage("/skill wisdom");
        setIdentifiers("skill wisdom");
        setTypes(SkillType.BUFF, SkillType.MANA, SkillType.SILENCABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("regen-multiplier", 1.2);
        node.set(Setting.RADIUS.node(), 10);
        node.set(Setting.DURATION.node(), 600000); // in Milliseconds - 10 minutes
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "You feel a bit wiser!");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "You no longer feel as wise!");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 600000, false);
        double manaMultiplier = SkillConfigManager.getUseSetting(hero, this, "regen-multiplier", 1.2, false);

        WisdomEffect mEffect = new WisdomEffect(this, duration, manaMultiplier);
        if (!hero.hasParty()) {
            if (hero.hasEffect("Wisdom")) {
                if (((WisdomEffect) hero.getEffect("Wisdom")).getManaMultiplier() > mEffect.getManaMultiplier()) {
                    Messaging.send(player, "You have a more powerful effect already!");
                }
            }
            hero.addEffect(mEffect);
        } else {
            int rangeSquared = (int) Math.pow(SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 10, false), 2);
            for (Hero pHero : hero.getParty().getMembers()) {
                Player pPlayer = pHero.getPlayer();
                if (!pPlayer.getWorld().equals(player.getWorld())) {
                    continue;
                }
                if (pPlayer.getLocation().distanceSquared(player.getLocation()) > rangeSquared) {
                    continue;
                }
                if (pHero.hasEffect("Wisdom")) {
                    if (((WisdomEffect) pHero.getEffect("Wisdom")).getManaMultiplier() > mEffect.getManaMultiplier()) {
                        continue;
                    }
                }
                pHero.addEffect(mEffect);
            }
        }

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        @EventHandler()
        public void onHeroRegainMana(HeroRegainManaEvent event) {
            if (event.isCancelled()) {
                return;
            }

            if (event.getHero().hasEffect("Wisdom")) {
                event.setAmount((int) (event.getAmount() * ((WisdomEffect) event.getHero().getEffect("Wisdom")).getManaMultiplier()));
            }
        }
    }

    public class WisdomEffect extends ExpirableEffect {

        private final double manaMultiplier;

        public WisdomEffect(Skill skill, long duration, double manaMultiplier) {
            super(skill, "Wisdom", duration);
            this.manaMultiplier = manaMultiplier;
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

        public double getManaMultiplier() {
            return manaMultiplier;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            Messaging.send(player, expireText);
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double mult = SkillConfigManager.getUseSetting(hero, this, "regen-multiplier", 1.2, false);
        return getDescription().replace("$1", Util.stringDouble((mult - 1) * 100));
    }
}
