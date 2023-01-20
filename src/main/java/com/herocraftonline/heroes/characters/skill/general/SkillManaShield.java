package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.SphereEffect;
import com.herocraftonline.heroes.libs.slikey.effectlib.util.DynamicLocation;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillManaShield extends ActiveSkill {

    private static final Color MANA_BLUE = Color.fromRGB(0, 191, 255);

    private final String effectName = "ManaShield";
    private String applyText;
    private String expireText;

    public SkillManaShield(final Heroes plugin) {
        super(plugin, "ManaShield");
        setDescription("Conjure a shield of pure mana that lasts for up to $1 second(s). "
                + "While active, the shield will absorb $2% of all incoming damage at the cost of your mana at $3% rate. "
                + "Can be toggled off manually by re-casting the skill.");
        setUsage("/skill manashield");
        setArgumentRange(0, 0);
        setIdentifiers("skill manashield", "skill mshield");
        setTypes(SkillType.BUFFING, SkillType.SILENCEABLE, SkillType.MANA_DECREASING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        final double mitigationPercent = SkillConfigManager.getUseSetting(hero, this, "damage-mitigation-percent", 0.35, false);
        final double absorbCostRatio = SkillConfigManager.getUseSetting(hero, this, "absorb-cost-percent", 1.5, false);

        return getDescription()
                .replace("$1", Util.decFormat.format((double) duration / 1000))
                .replace("$2", Util.decFormat.format(mitigationPercent * 100))
                .replace("$3", Util.decFormat.format(absorbCostRatio * 100));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set("damage-mitigation-percent", 0.35);
        config.set("absorb-cost-percent", 1.5);
        config.set(SkillSetting.DURATION.node(), 20000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% was surrounded by a shield of mana!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% lost their mana shield!");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% was surrounded by a shield of mana!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% lost his mana shield!").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        hero.addEffect(new ManaShieldEffect(this, hero.getPlayer(), duration));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8F, 0.4F);

        return SkillResult.NORMAL;
    }

    public class ManaShieldEffect extends ExpirableEffect {
        private SphereEffect visualEffect;

        public ManaShieldEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, effectName, applier, duration, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            applyVisuals(hero.getPlayer());
        }

        private void applyVisuals(final LivingEntity target) {
            final int durationTicks = (int) this.getDuration() / 50;
            final int displayPeriod = 2;

            this.visualEffect = new SphereEffect(effectLib);

            final DynamicLocation dynamicLoc = new DynamicLocation(target);
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;

            visualEffect.radius = 2.0F;
            visualEffect.color = MANA_BLUE;
            visualEffect.particle = Particle.REDSTONE;
            visualEffect.particles = 25;

            visualEffect.period = displayPeriod;
            visualEffect.iterations = durationTicks / displayPeriod;

            effectLib.start(visualEffect);
        }
    }

    public class SkillHeroListener implements Listener {

        private final Skill skill;

        public SkillHeroListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onCharacterDamage(final EntityDamageEvent event) {
            if (!(event.getEntity() instanceof Player)) {
                return;
            }

            final Player player = (Player) event.getEntity();
            final Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.hasEffect(effectName)) {
                return;
            }

            final double newDamage = getAdjustment(hero, event.getDamage());
            if (newDamage > -1.0) {
                event.setDamage(newDamage);
            }
        }

        private double getAdjustment(final Hero hero, double damage) {
            final double mitigationPercent = SkillConfigManager.getUseSetting(hero, skill, "damage-mitigation-percent", 0.35, false);
            final double absorbCostRatio = SkillConfigManager.getUseSetting(hero, skill, "absorb-cost-percent", 1.5, false);

            final double mitigatedDamage = damage * mitigationPercent;
            final double manaCost = mitigatedDamage * absorbCostRatio;

            damage -= mitigatedDamage;
            int mana = hero.getMana();
            if (mana < manaCost) {
                final Location location = hero.getPlayer().getLocation();
                location.getWorld().playSound(location, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0F);
                hero.removeEffect(hero.getEffect(effectName));
                return -1.0;
            } else {
                mana -= manaCost;
                hero.setMana(mana);
                if (hero.isVerboseMana()) {
                    hero.getPlayer().sendMessage(ChatColor.BLUE + "MANA " + ChatComponents.Bars.mana(hero.getMana(), hero.getMaxMana(), false));
                }
            }
            return damage;
        }
    }
}
