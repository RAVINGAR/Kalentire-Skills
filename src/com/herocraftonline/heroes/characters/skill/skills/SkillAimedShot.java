package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillAimedShot extends TargettedSkill {

    private String applyText;
    private String expireTextFail;
    private String expireTextSuccess;

    public SkillAimedShot(Heroes plugin) {
        super(plugin, "AimedShot");
        setDescription("Hone your aim in on a target. Once completed, your next next shot fired within $1 seconds will land "
                + ChatColor.BOLD + ChatColor.ITALIC+ "without question" + ChatColor.RESET
                + ChatColor.GOLD + ". That shot is armor piercing and will deal up to $2 damage to the target.");
        setUsage("/skill aimedshot");
        setArgumentRange(0, 0);
        setIdentifiers("skill aimedshot");
        setTypes(SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.ARMOR_PIERCING, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.STEALTHY);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    public String getDescription(Hero hero) {

        double gracePeriod = SkillConfigManager.getUseSetting(hero, this, "grace-period", 4000, false) / 1000;
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 250, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY, 3.1, false);
        damage += hero.getAttributeValue(AttributeType.DEXTERITY) * damageIncrease;

        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", gracePeriod + "").replace("$2", formattedDamage);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.USE_TEXT.node(), "");
        node.set(SkillSetting.DAMAGE.node(), 125);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY.node(), 3.1);
        node.set(SkillSetting.MAX_DISTANCE.node(), 40);
        node.set(SkillSetting.DELAY.node(), 3000);
        node.set("grace-period", 4000);
        node.set(SkillSetting.APPLY_TEXT.node(), String.valueOf(Messaging.getSkillDenoter() + "%hero% is locked on!"));
        node.set(SkillSetting.DELAY_TEXT.node(), String.valueOf(Messaging.getSkillDenoter() + "%hero% begins to hone in his aim on %target%"));
        node.set("expire-text-fail", String.valueOf(Messaging.getSkillDenoter() + "%hero% has lost sight of his target."));
        node.set("expire-text-success", String.valueOf(Messaging.getSkillDenoter() + "%hero% has unleashed a powerful " + ChatColor.BOLD + "Aimed Shot" + ChatColor.RESET + ChatColor.GRAY + " on %target%!"));

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% is locked on!").replace("%hero%", "$1");
        expireTextFail = SkillConfigManager.getRaw(this, "expire-text-fail", Messaging.getSkillDenoter() + "%hero% has lost sight of his target.").replace("%hero%", "$1");
        expireTextSuccess = SkillConfigManager.getRaw(this, "expire-text-success", Messaging.getSkillDenoter() + "%hero% has unleashed a powerful " + ChatColor.BOLD + "Aimed Shot" + ChatColor.RESET + ChatColor.GRAY + " on %target%!").replace("%hero%", "$1").replace("%target%", "$2");
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();

        int gracePeriod = SkillConfigManager.getUseSetting(hero, this, "grace-period", 2000, false);
        hero.addEffect(new AimedShotBuffEffect(this, player, target, gracePeriod));

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityShootBow(EntityShootBowEvent event) {
            if ((!(event.getEntity() instanceof Player)) || (!(event.getProjectile() instanceof Arrow))) {
                return;
            }

            final Player player = (Player) event.getEntity();
            final Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect("AimedShotBuffEffect")) {

                // Player released arrow too soon--skill failure.
                AimedShotBuffEffect asEffect = (AimedShotBuffEffect) hero.getEffect("AimedShotBuffEffect");

                // Tell the buff that we have a successful shot and then remove it
                asEffect.setLostSight(false);
                hero.removeEffect(asEffect);

                final LivingEntity target = asEffect.getTarget();

                Vector playerLocVec = hero.getPlayer().getLocation().toVector();
                Vector targetLocVec = target.getLocation().toVector();

                double distance = playerLocVec.distance(targetLocVec);
                int travelTime = (int) (0.055 * distance);

                // Remove the standard projectile
                Arrow actualArrow = (Arrow) event.getProjectile();
                actualArrow.remove();

                // Play wolf howl at both locations
                player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_WOLF_HOWL.value(), 0.7f, 1.0F);
                target.getWorld().playSound(target.getLocation(), CompatSound.ENTITY_WOLF_HOWL.value(), 0.7f, 1.0F);

                // Lower damage of shot based on how drawn back the bow is.
                double tempDamage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 125, false);
                double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY, 3.1, false);
                tempDamage += hero.getAttributeValue(AttributeType.DEXTERITY) * damageIncrease;

                final double damage = event.getForce() * tempDamage;
                // Damage the target, but add a delay based on the distance from the target.
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        skill.plugin.getDamageManager().addSpellTarget(target, hero, skill);
                        damageEntity(target, player, damage, DamageCause.MAGIC);  // Magic so it is armor piercing.
                    }
                }, travelTime * 20);	// make the damage happen 0.055 seconds later per block.
            }
        }
    }

    // Buff effect used to keep track of warmup time
    private class AimedShotBuffEffect extends ExpirableEffect {

        private boolean lostSight = true;
        private LivingEntity target;

        public AimedShotBuffEffect(Skill skill, Player applier, LivingEntity target, long duration) {
            super(skill, "AimedShotBuffEffect", applier, duration);

            types.add(EffectType.IMBUE);
            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);

            this.target = target;
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

            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();

            if (isLostSight()) {
                broadcast(player.getLocation(), "    " + expireTextFail, player.getName());
                return;
            }

            if (target instanceof Monster)
                //broadcast(player.getLocation(), "    " + expireTextSuccess, player.getName(), Messaging.getLivingEntityName((Monster) target));
                broadcast(player.getLocation(), "    " + expireTextSuccess, player.getName(), Messaging.getLivingEntityName(target));
            else if (target instanceof Player)
                broadcast(player.getLocation(), "    " + expireTextSuccess, player.getName(), ((Player) target).getName());
        }

        public void setLostSight(boolean lostSight) {
            this.lostSight = lostSight;
        }

        public LivingEntity getTarget() {
            return target;
        }

        public boolean isLostSight() {
            return lostSight;
        }
    }
}