package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillSerratedArrows extends PassiveSkill implements Listenable {
    private final Listener listener;

    public SkillSerratedArrows(Heroes plugin) {
        super(plugin, "SerratedArrows");
        setDescription("Your arrows cut deep, breaking through armor with every third shot landed within $1 seconds on a single target, dealing $2 damage.");
        setUsage("/skill serratedarrows");
        setArgumentRange(0, 0);
        setIdentifiers("skill serratedarrows");
        setTypes(SkillType.DAMAGING, SkillType.ARMOR_PIERCING);

        PassiveSkill skill = this;
        listener = new Listener() {
            private String getMultiHitEffectName(Player player) {
                return player.getName() + "-SerratedArrows";
            }

            @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
            public void onEntityDamage(EntityDamageByEntityEvent event) {
                if (!(event.getDamager() instanceof Arrow)) {
                    return;
                }

                Arrow arrow = (Arrow) event.getDamager();
                if (!(arrow.getShooter() instanceof Player)) {
                    return;
                }

                final Player player = (Player) arrow.getShooter();
                final Hero hero = plugin.getCharacterManager().getHero(player);
                double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 90, false);
                long duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 30000, false);

                if (!skill.hasPassive(hero))
                    return;

                LivingEntity target = (LivingEntity) event.getEntity();
                CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                String effectName = getMultiHitEffectName(player);

                if (!targetCT.hasEffect(effectName)) {
                    targetCT.addEffect(new SerratedArrowsHitEffect(skill, effectName, player, duration));
                    return;
                }

                SerratedArrowsHitEffect serratedEffect = (SerratedArrowsHitEffect) targetCT.getEffect(effectName);
                assert serratedEffect != null;
                serratedEffect.addHit();
                if (serratedEffect.getHitCount() != 3)
                    return;

                // Reduce damage by current bow force. // TODO: Use meta key from HDamageListener?
                if (arrow.hasMetadata("hero-fired-bow-force")) {
                    damage *= (float) arrow.getMetadata("hero-fired-bow-force").get(0).value();
                }

                addSpellTarget(target, hero);
                damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 0.5f);
                targetCT.removeEffect(serratedEffect);

                VisualEffect.playInstantFirework(FireworkEffect.builder()
                        .flicker(false)
                        .trail(false)
                        .with(FireworkEffect.Type.BURST)
                        .withColor(Color.WHITE)
                        .withFade(Color.GREEN)
                        .build(), target.getLocation().add(0, 1.0, 0));

                event.setDamage(0);
                event.setCancelled(true);
            }
        };
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000.0))
                .replace("$2", Util.decFormat.format(damage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set(SkillSetting.DAMAGE.node(), 95);
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        return config;
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    private static class SerratedArrowsHitEffect extends ExpirableEffect {
        private int hitCount = 1;

        SerratedArrowsHitEffect(Skill skill, String name, Player applier, long duration) {
            super(skill, name, applier, duration);
        }

        private int getHitCount() {
            return this.hitCount;
        }

        private void addHit() {
            this.hitCount++;
            this.setDuration(getDuration());
        }
    }
}
