package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

public class SkillAimedShot extends TargettedSkill {

    private String applyText;
    private String expireTextFail;
    private String expireTextSuccess;

    public SkillAimedShot(final Heroes plugin) {
        super(plugin, "AimedShot");
        setDescription("Hone your aim in on a target. Once completed, your next next shot fired within $1 second(s) will land "
                + ChatColor.BOLD + ChatColor.ITALIC + "without question" + ChatColor.RESET
                + ChatColor.GOLD + ". That shot is armor piercing and will deal up to $2 damage to the target.");
        setUsage("/skill aimedshot");
        setArgumentRange(0, 0);
        setIdentifiers("skill aimedshot");
        setTypes(SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.ARMOR_PIERCING, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.STEALTHY);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {

        final double gracePeriod = SkillConfigManager.getUseSetting(hero, this, "grace-period", 4000, false) / 1000.0;
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 250, false);
        final double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY, 3.1, false);
        damage += hero.getAttributeValue(AttributeType.DEXTERITY) * damageIncrease;

        final String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", gracePeriod + "").replace("$2", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.USE_TEXT.node(), "");
        node.set(SkillSetting.DAMAGE.node(), 125);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY.node(), 3.1);
        node.set(SkillSetting.MAX_DISTANCE.node(), 40);
        node.set(SkillSetting.DELAY.node(), 3000);
        node.set("grace-period", 4000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is locked on!");
        node.set(SkillSetting.DELAY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% begins to hone in his aim on %target%");
        node.set("expire-text-fail", ChatComponents.GENERIC_SKILL + "%hero% has lost sight of his target.");
        node.set("expire-text-success", ChatComponents.GENERIC_SKILL + "%hero% has unleashed a powerful " + ChatColor.BOLD + "Aimed Shot" + ChatColor.RESET + ChatColor.GRAY + " on %target%!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is locked on!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireTextFail = SkillConfigManager.getRaw(this, "expire-text-fail", ChatComponents.GENERIC_SKILL + "%hero% has lost sight of his target.").replace("%hero%", "$1").replace("$hero$", "$1");
        expireTextSuccess = SkillConfigManager.getRaw(this, "expire-text-success", ChatComponents.GENERIC_SKILL + "%hero% has unleashed a powerful " + ChatColor.BOLD + "Aimed Shot" + ChatColor.RESET + ChatColor.GRAY + " on %target%!").replace("%hero%", "$1").replace("$hero$", "$1").replace("%target%", "$2").replace("$target$", "$2");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {

        final Player player = hero.getPlayer();

        final int gracePeriod = SkillConfigManager.getUseSetting(hero, this, "grace-period", 2000, false);
        hero.addEffect(new AimedShotBuffEffect(this, player, target, gracePeriod));

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private final Skill skill;

        public SkillEntityListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityShootBow(final EntityShootBowEvent event) {
            if ((!(event.getEntity() instanceof Player)) || (!(event.getProjectile() instanceof Arrow))) {
                return;
            }

            final Player player = (Player) event.getEntity();
            final Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect("AimedShotBuffEffect")) {

                // Player released arrow too soon--skill failure.
                final AimedShotBuffEffect asEffect = (AimedShotBuffEffect) hero.getEffect("AimedShotBuffEffect");

                // Tell the buff that we have a successful shot and then remove it
                asEffect.setLostSight(false);
                hero.removeEffect(asEffect);

                final LivingEntity target = asEffect.getTarget();

                final Vector playerLocVec = hero.getPlayer().getLocation().toVector();
                final Vector targetLocVec = target.getLocation().toVector();

                final double distance = playerLocVec.distance(targetLocVec);
                final int travelTime = (int) (0.055 * distance);

                // Remove the standard projectile
                final Arrow actualArrow = (Arrow) event.getProjectile();
                actualArrow.remove();

                // Play wolf howl at both locations
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 0.7f, 1.0F);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WOLF_HOWL, 0.7f, 1.0F);

                // Lower damage of shot based on how drawn back the bow is.
                double tempDamage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 125, false);
                final double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY, 3.1, false);
                tempDamage += hero.getAttributeValue(AttributeType.DEXTERITY) * damageIncrease;

                final double damage = event.getForce() * tempDamage;
                // Damage the target, but add a delay based on the distance from the target.
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    skill.plugin.getDamageManager().addSpellTarget(target, hero, skill);
                    damageEntity(target, player, damage, DamageCause.MAGIC);  // Magic so it is armor piercing.
                }, travelTime * 20L);    // make the damage happen 0.055 seconds later per block.
            }
        }
    }

    // Buff effect used to keep track of warmup time
    private class AimedShotBuffEffect extends ExpirableEffect {

        private final LivingEntity target;
        private boolean lostSight = true;

        public AimedShotBuffEffect(final Skill skill, final Player applier, final LivingEntity target, final long duration) {
            super(skill, "AimedShotBuffEffect", applier, duration);

            types.add(EffectType.IMBUE);
            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);

            this.target = target;
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

            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);

            final Player player = hero.getPlayer();

            if (isLostSight()) {
                broadcast(player.getLocation(), "    " + expireTextFail, player.getName());
                return;
            }

            if (target instanceof Monster)
            //broadcast(player.getLocation(), "    " + expireTextSuccess, player.getName(), CustomNameManager.getName((Monster) target));
            {
                broadcast(player.getLocation(), "    " + expireTextSuccess, player.getName(), CustomNameManager.getName(target));
            } else if (target instanceof Player) {
                broadcast(player.getLocation(), "    " + expireTextSuccess, player.getName(), ((Player) target).getName());
            }
        }

        public LivingEntity getTarget() {
            return target;
        }

        public boolean isLostSight() {
            return lostSight;
        }

        public void setLostSight(final boolean lostSight) {
            this.lostSight = lostSight;
        }
    }
}