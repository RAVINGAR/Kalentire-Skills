package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SkillWisdom extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillWisdom(Heroes plugin) {
        super(plugin, "Wisdom");
        this.setDescription("You party benefits from $1% increased mana regeneration.");
        this.setArgumentRange(0, 0);
        this.setUsage("/skill wisdom");
        this.setIdentifiers("skill wisdom");
        this.setTypes(SkillType.BUFFING, SkillType.MANA_INCREASING, SkillType.SILENCEABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("regen-multiplier", 1.2);
        node.set(SkillSetting.RADIUS.node(), 10);
        node.set(SkillSetting.DURATION.node(), 600000); // in Milliseconds - 10 minutes
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "You feel a bit wiser!");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "You no longer feel as wise!");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
        final double manaMultiplier = SkillConfigManager.getUseSetting(hero, this, "regen-multiplier", 1.2, false);

        final WisdomEffect mEffect = new WisdomEffect(this, player, duration, manaMultiplier);
        if (!hero.hasParty()) {
            if (hero.hasEffect("Wisdom")) {
                if (((WisdomEffect) hero.getEffect("Wisdom")).getManaMultiplier() > mEffect.getManaMultiplier()) {
                    player.sendMessage(ChatColor.GRAY + "You have a more powerful effect already!");
                }
            }
            hero.addEffect(mEffect);
        } else {
            final int rangeSquared = (int) Math.pow(SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false), 2);
            for (final Hero pHero : hero.getParty().getMembers()) {
                final Player pPlayer = pHero.getPlayer();
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

        this.broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        @EventHandler()
        public void onHeroRegainMana(HeroRegainManaEvent event) {
            if (event.isCancelled()) {
                return;
            }

            if (event.getHero().hasEffect("Wisdom")) {
                event.setDelta((int) (event.getDelta() * ((WisdomEffect) event.getHero().getEffect("Wisdom")).getManaMultiplier()));
            }
        }
    }

    public class WisdomEffect extends ExpirableEffect {

        private final double manaMultiplier;

        public WisdomEffect(Skill skill, Player applier, long duration, double manaMultiplier) {
            super(skill, "Wisdom", applier, duration);
            this.manaMultiplier = manaMultiplier;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            player.sendMessage(ChatColor.GRAY + SkillWisdom.this.applyText);
        }

        public double getManaMultiplier() {
            return this.manaMultiplier;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            player.sendMessage(ChatColor.GRAY + SkillWisdom.this.expireText);
        }
    }

    @Override
    public String getDescription(Hero hero) {
        final double mult = SkillConfigManager.getUseSetting(hero, this, "regen-multiplier", 1.2, false);
        return this.getDescription().replace("$1", Util.stringDouble((mult - 1) * 100));
    }
}
