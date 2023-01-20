package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

public class SkillAccentedStrikes extends ActiveSkill implements Listenable {
    private final String effectName = "AccentedStrikes";
    private final Listener listener;
    private String applyText;
    private String expireText;

    public SkillAccentedStrikes(final Heroes plugin) {
        super(plugin, "AccentedStrikes");
        setDescription("Apply the musical idea of accents to your strikes for $1 second(s). " +
                "While active, your attacks are much stronger, dealing an extra $2 damage to the target.");
        setUsage("/skill accentedstrikes");
        setIdentifiers("skill accentedstrikes");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_POISON, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.BUFFING);

        listener = new SkillDamageListener(this);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000.0))
                .replace("$2", Util.decFormat.format(damage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set("weapons", Util.swords);
        config.set(SkillSetting.DURATION.node(), 10000);
        config.set(SkillSetting.DAMAGE.node(), 5.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has begun accenting his strikes.");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% no longer has accented strikes.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has begun accenting his strikes.").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% no longer has accented strikes.").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        hero.addEffect(new AccentedStrikesEffect(this, hero.getPlayer(), duration));

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    public class SkillDamageListener implements Listener {
        private final Skill skill;

        public SkillDamageListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(final EntityDamageByEntityEvent event) {
            if ((!(event.getEntity() instanceof LivingEntity))) {
                return;
            }

            // Check for both arrow shots and left click attacks. Determine player based on which we're dealing with
            boolean arrow = false;
            final Player player;
            final Entity damagingEntity = event.getDamager();
            if (damagingEntity instanceof Arrow) {
                if (!(((Projectile) damagingEntity).getShooter() instanceof Player)) {
                    return;
                }

                player = (Player) ((Projectile) damagingEntity).getShooter();
                arrow = true;
            } else {
                if (event.getCause() != DamageCause.ENTITY_ATTACK) {
                    return;
                }

                final LivingEntity target = (LivingEntity) event.getEntity();
                if (!(plugin.getDamageManager().isSpellTarget(target))) {
                    if (!(event.getDamager() instanceof Player)) {
                        return;
                    }

                    player = (Player) event.getDamager();
                } else {
                    return;
                }
            }

            final Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.hasEffect(effectName)) {
                return;
            }

            final LivingEntity target = (LivingEntity) event.getEntity();

            final ItemStack item = NMSHandler.getInterface().getItemInMainHand(player.getInventory());
            if (!SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.swords).contains(item.getType().name())) {
                if (arrow) {
                    dealAccentedStrikesDamage(hero, target);
                }
            } else {
                dealAccentedStrikesDamage(hero, target);
            }
        }

        private void dealAccentedStrikesDamage(final Hero hero, final LivingEntity target) {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (!(damageCheck(hero.getPlayer(), target))) {
                    return;
                }

                final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.DAMAGE, false);

                // Damage the target
                addSpellTarget(target, hero);
                damageEntity(target, hero.getPlayer(), damage, DamageCause.MAGIC, false);
            }, 2L);
        }
    }

    public class AccentedStrikesEffect extends ExpirableEffect {
        public AccentedStrikesEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, effectName, applier, duration, applyText, expireText);

            types.add(EffectType.IMBUE);
            types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void applyToHero(final Hero hero) {
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
    }
}
