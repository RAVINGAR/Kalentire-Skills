package com.herocraftonline.heroes.characters.skill.skills;

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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillAimedShot extends TargettedSkill {

    private String applyText;
    private String expireTextFail;
    private String expireTextBadShot;
    private String expireTextSuccess;

    public SkillAimedShot(Heroes plugin) {
        super(plugin, "AimedShot");
        setDescription("Hone your aim in on a target. Once completed, your next next shot fired within $1 seconds will land "
                + ChatColor.BOLD + ChatColor.ITALIC+ "without question" + ChatColor.RESET
                + ChatColor.GOLD + ". That shot will deal up to $2 damage to the target.");
        setUsage("/skill aimedshot");
        setArgumentRange(0, 0);
        setIdentifiers("skill aimedshot");
        setTypes(SkillType.PHYSICAL, SkillType.HARMFUL, SkillType.DAMAGING, SkillType.STEALTHY);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    public String getDescription(Hero hero) {

        double gracePeriod = SkillConfigManager.getUseSetting(hero, this, "grace-period", 4000, false) / 1000;
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 250, false);

        return getDescription().replace("$1", gracePeriod + "").replace("$2", damage + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(250));
        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(40));
        node.set(SkillSetting.DELAY.node(), Integer.valueOf(3000));
        node.set("grace-period", Integer.valueOf(2000));
        node.set(SkillSetting.APPLY_TEXT.node(), String.valueOf(ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% is locked on!"));
        node.set(SkillSetting.DELAY_TEXT.node(), String.valueOf(ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% begins to hone in his aim on %target%"));
        node.set("expire-text-fail", String.valueOf(ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% has lost sight of his target."));
        node.set("expire-text-bad-shot", String.valueOf(ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% did not put enough strength into his shot!"));
        node.set("expire-text-success", String.valueOf(ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% has unleashed a powerful " + ChatColor.BOLD + "Aimed Shot" + ChatColor.RESET + ChatColor.GRAY + " on %target%!"));

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% is locked on!").replace("%hero%", "$1");
        expireTextFail = SkillConfigManager.getRaw(this, "expire-text-fail", ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% has lost sight of his target.").replace("%hero%", "$1");
        expireTextBadShot = SkillConfigManager.getRaw(this, "expire-text-bad-shot", ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% did not put enough strength into his shot!").replace("%hero%", "$1");
        expireTextSuccess = SkillConfigManager.getRaw(this, "expire-text-success", ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% has unleashed a powerful " + ChatColor.BOLD + "Aimed Shot" + ChatColor.RESET + ChatColor.GRAY + " on %target%!").replace("%hero%", "$1").replace("%target%", "$2");
    }
    
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();

        // Check line of sight, but only against other players.
        if (target instanceof Player) {

            Player targetPlayer = (Player) target;
            if (!inLineOfSight(player, targetPlayer) || !player.canSee(targetPlayer)) {
                hero.getPlayer().sendMessage("Your target is not within your line of sight!");
                return SkillResult.FAIL;
            }
        }

        int gracePeriod = SkillConfigManager.getUseSetting(hero, this, "grace-period", 2000, false);
        hero.addEffect(new AimedShotBuffEffect(this, target, gracePeriod));

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

                //                if (event.getForce() < 1) {
                //                    // Player released arrow too soon--skill failure.
                //                    asEffect.setLostSight(false);
                //                    asEffect.setBadShot(true);
                //                    hero.removeEffect(asEffect);
                //                    return;
                //                }

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
                player.getWorld().playSound(player.getLocation(), Sound.WOLF_HOWL, 0.7f, 1.0F);
                target.getWorld().playSound(target.getLocation(), Sound.WOLF_HOWL, 0.7f, 1.0F);

                // Lower damage of shot based on how drawn back the bow is.
                final double damage = event.getForce() * SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 250, false);

                // Damage the target, but add a delay based on the distance from the target.
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        skill.plugin.getDamageManager().addSpellTarget(target, hero, skill);
                        damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC);
                    }
                }, travelTime * 20);	// make the damage happen 0.055 seconds later per block.
            }
        }
    }

    // Buff effect used to keep track of warmup time
    private class AimedShotBuffEffect extends ExpirableEffect {

        private boolean badShot = false;
        private boolean lostSight = true;
        private LivingEntity target;

        public AimedShotBuffEffect(Skill skill, LivingEntity target, long duration) {
            super(skill, "AimedShotBuffEffect", duration);

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
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();

            if (isLostSight()) {
                broadcast(player.getLocation(), expireTextFail, player.getDisplayName());
                return;
            }

            if (badShot)
                broadcast(player.getLocation(), expireTextBadShot, player.getDisplayName());
            else {
                if (target instanceof Monster)
                    broadcast(player.getLocation(), expireTextSuccess, player.getDisplayName(), Messaging.getLivingEntityName((Monster) target));
                else if (target instanceof Player)
                    broadcast(player.getLocation(), expireTextSuccess, player.getDisplayName(), ((Player) target).getDisplayName());
            }
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