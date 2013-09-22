package com.herocraftonline.heroes.characters.skill.skills;

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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
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

public class SkillDarkBlade extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillDarkBlade(Heroes plugin) {
        super(plugin, "DarkBlade");
        setDescription("Enchant your blade with powerful dark magic for the next $1 seconds. While enchanted, your melee attacks will deal an additional $2 physical damage, and drain $3 mana from the target, returning it to you.");
        setUsage("/skill envenom");
        setArgumentRange(0, 0);
        setIdentifiers("skill envenom");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.ABILITY_PROPERTY_DARK, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.MANA_DECREASING, SkillType.MANA_INCREASING, SkillType.BUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(10000), false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(5), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(2.0), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("weapons", Util.axes);
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(10000));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(5));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), Double.valueOf(2));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% has coated his weapons with a deadly poison.");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero%'s weapons are no longer poisoned.");

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% has coated his weapons with a deadly poison.").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero%'s weapons are no longer poisoned.").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(10000), false);
        hero.addEffect(new DarkBladeEffect(this, hero.getPlayer(), duration));

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public class SkillDamageListener implements Listener {
        private final Skill skill;

        public SkillDamageListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if ((!(event instanceof EntityDamageByEntityEvent)) || (!(event.getEntity() instanceof LivingEntity))) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;

            // Check for both arrow shots and left click attacks. Determine player based on which we're dealing with
            boolean arrow = false;
            Player player;
            Entity damagingEntity = ((EntityDamageByEntityEvent) event).getDamager();
            if (damagingEntity instanceof Arrow) {
                if (!(((Projectile) damagingEntity).getShooter() instanceof Player))
                    return;

                player = (Player) ((Projectile) damagingEntity).getShooter();
                arrow = true;
            }
            else {
                if (event.getCause() != DamageCause.ENTITY_ATTACK)
                    return;

                LivingEntity target = (LivingEntity) event.getEntity();
                if (!(plugin.getDamageManager().isSpellTarget(target))) {
                    if (!(subEvent.getDamager() instanceof Player))
                        return;

                    player = (Player) subEvent.getDamager();
                }
                else
                    return;
            }

            Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.hasEffect("DarkBlade"))
                return;

            LivingEntity target = (LivingEntity) event.getEntity();

            ItemStack item = player.getItemInHand();
            if (!SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.axes).contains(item.getType().name())) {
                if (arrow == true)
                    darkBladeAttack(hero, target);
            }
            else
                darkBladeAttack(hero, target);

            return;
        }

        private void darkBladeAttack(Hero hero, LivingEntity target) {
            double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, Integer.valueOf(5), false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(2.0), false);
            damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

            // Damage the target
            addSpellTarget(target, hero);
            damageEntity(target, hero.getPlayer(), damage, DamageCause.ENTITY_ATTACK, false);

            if (target instanceof Player) {
                Player targetPlayer = (Player) target;
                // Get the target hero
                Hero tHero = plugin.getCharacterManager().getHero((Player) target);

                // Burn their mana
                int manaBurn = SkillConfigManager.getUseSetting(hero, skill, "mana-burn-amount", 10, false);
                int burnedAmount = 0;
                if (tHero.getMana() > manaBurn) {
                    // Burn the target's mana
                    int newMana = tHero.getMana() - manaBurn;
                    burnedAmount = manaBurn;
                    tHero.setMana(newMana);
                }
                else {
                    // Burn all of their remaining mana
                    burnedAmount = Math.abs(0 - tHero.getMana());
                    tHero.setMana(0);
                }

                if (tHero.isVerboseMana())
                    Messaging.send(targetPlayer, Messaging.createManaBar(tHero.getMana(), tHero.getMaxMana()));

                HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, burnedAmount, skill);
                plugin.getServer().getPluginManager().callEvent(hrmEvent);
                if (!hrmEvent.isCancelled()) {
                    hero.setMana(hrmEvent.getAmount() + hero.getMana());

                    if (hero.isVerboseMana())
                        Messaging.send(targetPlayer, Messaging.createManaBar(hero.getMana(), hero.getMaxMana()));
                }
            }
        }
    }

    public class DarkBladeEffect extends ExpirableEffect {

        public DarkBladeEffect(Skill skill, Player applier, long duration) {
            super(skill, "DarkBlade", applier, duration, applyText, expireText);

            types.add(EffectType.IMBUE);
            types.add(EffectType.DARK);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MANA_INCREASING);
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
