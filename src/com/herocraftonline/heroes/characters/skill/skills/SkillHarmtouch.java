package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class SkillHarmtouch extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillHarmtouch(Heroes plugin) {
        super(plugin, "Harmtouch");
        setDescription("You deal $1 dark damage to the target, slowing them and reducing their healing by $2% for $3 seconds.");
        setUsage("/skill harmtouch");
        setArgumentRange(0, 0);
        setIdentifiers("skill harmtouch");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.MOVEMENT_SLOWING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 180, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.75, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        double heal = 1 - SkillConfigManager.getUseSetting(hero, this, "heal-multiplier", .5, true);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 12000, false);

        return getDescription().replace("$1", damage + "").replace("$2", heal * 100 + "").replace("$3", duration / 1000 + "");
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% has been harmfully touched by %hero%!").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has recovered from their harmful touch!").replace("%target%", "$1");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set(SkillSetting.DAMAGE.node(), 180);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.75);
        node.set(SkillSetting.HEALTH_COST.node(), 75);

        node.set(SkillSetting.DURATION.node(), 12000);
        node.set("heal-multiplier", .5);
        node.set("slow-level", 2);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% has been harmfully touched by %hero%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has recovered from their harmful touch!");
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 180, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.75, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);
        broadcastExecuteText(hero, target);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 12000, false);
        int amplifier = SkillConfigManager.getUseSetting(hero, this, "slow-level", 2, false);
        double healMultiplier = SkillConfigManager.getUseSetting(hero, this, "heal-multiplier", 0.5, true);
        plugin.getCharacterManager().getCharacter(target).addEffect(new HarmTouch(this, player, duration, amplifier, healMultiplier, applyText, expireText));


        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_SPAWN , 0.8F, 1.0F);
        hero.getPlayer().getWorld().spigot().playEffect(target.getLocation(), org.bukkit.Effect.EXPLOSION_LARGE, 0, 0, 0.3F, 1.2F, 0.3F, 0.0F, 15, 16);
        hero.getPlayer().getWorld().spigot().playEffect(target.getLocation(), org.bukkit.Effect.WITCH_MAGIC, 0, 0, 0.3F, 1.2F, 0.3F, 0.0F, 25, 16);
        hero.getPlayer().getWorld().spigot().playEffect(target.getLocation(), org.bukkit.Effect.SPELL, 0, 0, 0.3F, 1.2F, 0.3F, 0.0F, 25, 16);

        return SkillResult.NORMAL;
    }

    public class HarmTouch extends SlowEffect {

        private final double healMultiplier;

        public HarmTouch(Skill skill, Player applier, long duration, int amplifier, double healMultiplier, String applyText, String expireText) {
            super(skill, "HarmTouch", applier, duration, amplifier, applyText, expireText);
            this.healMultiplier = healMultiplier;

            types.add(EffectType.SLOW);
            types.add(EffectType.HARMFUL);
        }
    }

    public class SkillEntityListener implements Listener {

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onEntityRegainHealth(EntityRegainHealthEvent event) {
            if (!(event.getEntity() instanceof Player)) {
                return;
            }

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("HarmTouch")) {
                HarmTouch mEffect = (HarmTouch) hero.getEffect("HarmTouch");
                event.setAmount((event.getAmount() * mEffect.healMultiplier));
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onHeroRegainHealth(HeroRegainHealthEvent event) {
            if (event.getHero().hasEffect("HarmTouch")) {
                HarmTouch mEffect = (HarmTouch) event.getHero().getEffect("HarmTouch");
                event.setAmount((event.getAmount() * mEffect.healMultiplier));
            }
        }
    }
}
