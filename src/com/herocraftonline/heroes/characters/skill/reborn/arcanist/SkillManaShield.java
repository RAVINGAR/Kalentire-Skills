package com.herocraftonline.heroes.characters.skill.reborn.arcanist;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillManaShield extends ActiveSkill {

    private static Color MANA_BLUE = Color.fromRGB(0, 191, 255);

    private final String effectName = "ManaShield";
    private String applyText;
    private String expireText;

    public SkillManaShield(Heroes plugin) {
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
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        double mitigationPercent = SkillConfigManager.getUseSetting(hero, this, "damage-mitigation-percent", 0.35, false);
        double absorbCostRatio = SkillConfigManager.getUseSetting(hero, this, "absorb-cost-percent", 1.5, false);

        return getDescription()
                .replace("$1", Util.decFormat.format((double) duration / 1000))
                .replace("$2", Util.decFormat.format(mitigationPercent * 100))
                .replace("$3", Util.decFormat.format(absorbCostRatio * 100));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
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

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% was surrounded by a shield of mana!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% lost his mana shield!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        hero.addEffect(new ManaShieldEffect(this, hero.getPlayer(), duration));

        //player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK , 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class ManaShieldEffect extends ExpirableEffect {

        private EffectManager effectManager;
        private SphereEffect visualEffect;

        public ManaShieldEffect(Skill skill, Player applier, long duration) {
            super(skill, effectName, applier, duration, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            applyVisuals(hero.getPlayer());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            if (this.effectManager != null)
                this.effectManager.dispose();
        }

        private void applyVisuals(LivingEntity target) {
            final World world = target.getWorld();
            final Location loc = target.getLocation();
            final int durationTicks = (int) this.getDuration() / 50;
            final int displayPeriod = 2;

            this.effectManager = new EffectManager(plugin);
            this.visualEffect = new SphereEffect(effectManager);

            DynamicLocation dynamicLoc = new DynamicLocation(target);
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;

            visualEffect.radius = 2.0F;
            visualEffect.color = MANA_BLUE;
            visualEffect.particle = Particle.REDSTONE;
            visualEffect.particles = 25;

            visualEffect.period = displayPeriod;
            visualEffect.iterations = durationTicks / displayPeriod;

            effectManager.start(visualEffect);
            effectManager.disposeOnTermination();
        }
    }

    public class SkillHeroListener implements Listener {

        private final Skill skill;

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onCharacterDamage(EntityDamageEvent event) {
            if (!(event.getEntity() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.hasEffect(effectName))
                return;

            double newDamage = getAdjustment(hero, event.getDamage());
            if (newDamage > -1.0)
                event.setDamage(newDamage);
        }

        private double getAdjustment(Hero hero, double damage) {
            double mitigationPercent = SkillConfigManager.getUseSetting(hero, skill, "damage-mitigation-percent", 0.35, false);
            double absorbCostRatio = SkillConfigManager.getUseSetting(hero, skill, "absorb-cost-percent", 1.5, false);

            double mitigatedDamage = damage * mitigationPercent;
            double manaCost = mitigatedDamage * absorbCostRatio;

            damage-= mitigatedDamage;
            int mana = hero.getMana();
            if (mana < manaCost) {
                Location location = hero.getPlayer().getLocation();
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
