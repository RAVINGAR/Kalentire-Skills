package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillEnvenom extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillEnvenom(Heroes plugin) {
        super(plugin, "Envenom");
        setDescription("Apply a deadly venom to your melee weapon and arrows for $1 seconds. While active, your attacks cause great pain to your enemies, dealing an extra $2 damage to the target.");
        setUsage("/skill envenom");
        setArgumentRange(0, 0);
        setIdentifiers("skill envenom");
        setTypes(SkillType.ABILITY_PROPERTY_POISON, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.BUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(10000), false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(5), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(2.0), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("weapons", Util.swords);
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(10000));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(5));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(2));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% has coated his weapons with a deadly poison.");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero%'s weapons are no longer poisoned.");

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% has coated his weapons with a deadly poison.");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero%'s weapons are no longer poisoned.");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(10000), false);
        hero.addEffect(new EnvenomEffect(this, hero.getPlayer(), duration));

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public class SkillDamageListener implements Listener {
        private final Skill skill;

        public SkillDamageListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (!(event.getDamager() instanceof Hero) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            Hero hero = (Hero) event.getDamager();
            Player player = hero.getPlayer();

            if (!hero.hasEffect("Envenom"))
                return;

            LivingEntity target = (LivingEntity) event.getEntity();

            // Make sure they are actually dealing damage to the target.
            if (!damageCheck(player, target)) {
                return;
            }


            if (event.getAttackerEntity() instanceof Arrow) {
                dealEnvenomDamage(hero, target);
            }
            else {
                Material item = player.getItemInHand().getType();
                if (!SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.tools).contains(item.name()))
                    dealEnvenomDamage(hero, target);
            }
        }

        //        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        //        public void onEntityDamage(EntityDamageEvent event) {
        //            if ((!(event instanceof EntityDamageByEntityEvent)) || (!(event.getEntity() instanceof LivingEntity))) {
        //                return;
        //            }
        //
        //            Entity projectile = ((EntityDamageByEntityEvent) event).getDamager();
        //            if ((!(projectile instanceof Arrow)) || (!(((Projectile) projectile).getShooter() instanceof Player))) {
        //                return;
        //            }
        //            final Arrow arrow = (Arrow) projectile;
        //
        //            Hero hero = plugin.getCharacterManager().getHero((Player) arrow.getShooter());
        //            if (hero.hasEffect("Envenom"))
        //                dealEnvenomDamage(hero, (LivingEntity) event.getEntity());
        //        }

        private void dealEnvenomDamage(Hero hero, LivingEntity target) {
            double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, Integer.valueOf(5), false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(2.0), false);
            damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

            // Damage the target
            addSpellTarget(target, hero);
            damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC, false);
        }
    }

    public class EnvenomEffect extends ExpirableEffect {

        public EnvenomEffect(Skill skill, Player applier, long duration) {
            super(skill, "Envenom", applier, duration, applyText, expireText);

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
