package com.herocraftonline.heroes.characters.skill.reborn.bard;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

public class SkillAccentedStrikes extends ActiveSkill {

    private String effectName = "Envenom";

    private String applyText;
    private String expireText;

    public SkillAccentedStrikes(Heroes plugin) {
        super(plugin, "AccentedStrikes");
        setDescription("Apply the musical idea of accents to your strikes for $1 second(s). " +
                "While active, your attacks are much stronger, dealing an extra $2 damage to the target.");
        setUsage("/skill accentedstrikes");
        setIdentifiers("skill accentedstrikes");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_POISON, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.BUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 5, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 2.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("weapons", Util.swords);
        config.set(SkillSetting.DURATION.node(), 10000);
        config.set(SkillSetting.DAMAGE.node(), 5);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), (double) 2);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has begun accenting his strikes.");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% no longer has accented strikes.");
        return config;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has begun accenting his strikes.").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% no longer has accented strikes.").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        hero.addEffect(new AccentedStrikesEffect(this, hero.getPlayer(), duration));

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public class SkillDamageListener implements Listener {
        private final Skill skill;

        public SkillDamageListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageByEntityEvent event) {
            if ((!(event.getEntity() instanceof LivingEntity))) {
                return;
            }

            // Check for both arrow shots and left click attacks. Determine player based on which we're dealing with
            boolean arrow = false;
            Player player;
            Entity damagingEntity = event.getDamager();
            if (damagingEntity instanceof Arrow) {
                if (!(((Projectile) damagingEntity).getShooter() instanceof Player))
                    return;

                player = (Player) ((Projectile) damagingEntity).getShooter();
                arrow = true;
            } else {
                if (event.getCause() != DamageCause.ENTITY_ATTACK)
                    return;

                LivingEntity target = (LivingEntity) event.getEntity();
                if (!(plugin.getDamageManager().isSpellTarget(target))) {
                    if (!(event.getDamager() instanceof Player))
                        return;

                    player = (Player) event.getDamager();
                } else
                    return;
            }

            Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.hasEffect(effectName))
                return;

            LivingEntity target = (LivingEntity) event.getEntity();

            ItemStack item = NMSHandler.getInterface().getItemInMainHand(player.getInventory());
            if (!SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.swords).contains(item.getType().name())) {
                if (arrow) {
                    dealEnvenomDamage(hero, target);
                }
            } else {
                dealEnvenomDamage(hero, target);
            }
        }

        private void dealEnvenomDamage(final Hero hero, final LivingEntity target) {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {
                    if (!(damageCheck(hero.getPlayer(), target)))
                        return;

                    double damage = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.DAMAGE, false);

                    // Damage the target
                    addSpellTarget(target, hero);
                    damageEntity(target, hero.getPlayer(), damage, DamageCause.MAGIC, false);
                }
            }, 2L);
        }
    }

    public class AccentedStrikesEffect extends ExpirableEffect {

        public AccentedStrikesEffect(Skill skill, Player applier, long duration) {
            super(skill, effectName, applier, duration, applyText, expireText);

            types.add(EffectType.IMBUE);
            types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            for (final Effect effect : hero.getEffects()) {
                if (effect.equals(this)) {
                    continue;
                }

                if (effect.isType(EffectType.IMBUE)) {
                    hero.removeEffect(effect);
                }
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }
    }
}
