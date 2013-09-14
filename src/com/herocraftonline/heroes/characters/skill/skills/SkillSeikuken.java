package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftLivingEntity;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.DisarmEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillSeikuken extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillSeikuken(Heroes plugin) {
        super(plugin, "Seikuken");
        setDescription("Hone your martial art skill to it's fullest extent for $1 seconds. During the duration, you control everything within your arms lengths and retaliate against all melee attacks, disarmimg them for $2 seconds, and dealing $3% weapon damage.");
        setUsage("/skill seikuken");
        setArgumentRange(0, 0);
        setIdentifiers("skill seikuken");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.BUFFING, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);

        int disarmDuration = SkillConfigManager.getUseSetting(hero, this, "disarm-duration", Integer.valueOf(3000), false);

        double damageMultiplier = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", Double.valueOf(0.4), false);
        double damageMultiplierIncrease = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier-increase-per-intellect", Double.valueOf(0.00875), false);
        damageMultiplier += hero.getAttributeValue(AttributeType.INTELLECT) * damageMultiplierIncrease;

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDisarmDuration = Util.decFormat.format(disarmDuration / 1000.0);
        String formattedDamageMultiplier = Util.decFormat.format(damageMultiplier * 100);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedDisarmDuration).replace("$3", formattedDamageMultiplier);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("damage-multiplier", Double.valueOf(0.4));
        node.set("damage-multiplier-increase-per-intellect", Double.valueOf(0.00875));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(5000));
        node.set(SkillSetting.DURATION_INCREASE_PER_INTELLECT.node(), Integer.valueOf(75));
        node.set("slow-amplifier", Integer.valueOf(35));
        node.set("disarm-duration", Integer.valueOf(3000));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% has created a Seikuken!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero%'s Seikuken has faded.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% has created a Seikuken!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero%'s Seikuken has faded.").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_INTELLECT, 75, false);
        duration += hero.getAttributeValue(AttributeType.INTELLECT) * durationIncrease;

        int disarmDuration = SkillConfigManager.getUseSetting(hero, this, "disarm-duration", Integer.valueOf(3000), false);
        int slowAmplifier = SkillConfigManager.getUseSetting(hero, this, "slow-amplifier", Integer.valueOf(3), false);

        hero.addEffect(new SeikukenEffect(this, player, duration, slowAmplifier, disarmDuration));

        player.getWorld().playSound(player.getLocation(), Sound.EXPLODE, 0.7F, 2.0F);

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getDamage() == 0 || event.getAttackerEntity() instanceof Projectile)
                return;

            if (event.getEntity() instanceof Player && event.getDamager() instanceof Hero) {
                Player player = (Player) event.getEntity();
                Hero hero = plugin.getCharacterManager().getHero(player);

                // Check if they are under the effects of Seikuken
                if (hero.hasEffect("Seikuken")) {
                    Hero damagerHero = (Hero) event.getDamager();
                    Player damagerPlayer = damagerHero.getPlayer();

                    SeikukenEffect bgEffect = (SeikukenEffect) hero.getEffect("Seikuken");

                    event.setCancelled(true);

                    // Make them have invuln ticks so attackers dont get machine-gunned from attacking the buffed player.
                    ((CraftLivingEntity) player).setNoDamageTicks(((CraftLivingEntity) player).getMaximumNoDamageTicks());

                    // Don't retaliate against ranged attacks, throw the arrow instead! :O
                    if (event.getAttackerEntity() instanceof Projectile) {
                        Arrow arrow = player.launchProjectile(Arrow.class);
                        arrow.setShooter(player);
                        arrow.setVelocity(arrow.getVelocity().multiply(3));
                    }
                    else {
                        double damageMultiplier = SkillConfigManager.getUseSetting(hero, skill, "damage-multiplier", Double.valueOf(0.4), false);
                        double damageMultiplierIncrease = SkillConfigManager.getUseSetting(hero, skill, "damage-multiplier-increase-per-intellect", Double.valueOf(0.00875), false);
                        damageMultiplier += hero.getAttributeValue(AttributeType.INTELLECT) * damageMultiplierIncrease;

                        double damage = plugin.getDamageManager().getHighestItemDamage(player.getItemInHand().getType(), player) * damageMultiplier;
                        addSpellTarget((Player) damagerPlayer, hero);
                        damageEntity((Player) damagerPlayer, player, damage, DamageCause.ENTITY_ATTACK);

                        damagerPlayer.getWorld().playSound(damagerPlayer.getLocation(), Sound.ITEM_BREAK, 0.8F, 1.0F);
                    }

                    // Disarm checks
                    Material heldItem = damagerPlayer.getItemInHand().getType();
                    if (!Util.isWeapon(heldItem) && !Util.isAwkwardWeapon(heldItem)) {
                        return;
                    }
                    if (damagerHero.hasEffectType(EffectType.DISARM)) {
                        return;
                    }

                    // Disarm attacker
                    long disarmDuration = bgEffect.getDisarmDuration();
                    damagerHero.addEffect(new DisarmEffect(skill, player, disarmDuration));

                    damagerPlayer.getWorld().playSound(damagerPlayer.getLocation(), Sound.HURT, 0.8F, 0.5F);
                }
            }
        }
    }

    public class SeikukenEffect extends ExpirableEffect {

        private long disarmDuration;

        public SeikukenEffect(Skill skill, Player applier, long duration, int slowAmplifier, long disarmDuration) {
            super(skill, "Seikuken", applier, duration, applyText, expireText);

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);

            this.disarmDuration = disarmDuration;

            addMobEffect(2, (int) ((duration / 1000) * 20), slowAmplifier, false);
        }

        public long getDisarmDuration() {
            return disarmDuration;
        }

        public void setDisarmDuration(long disarmDuration) {
            this.disarmDuration = disarmDuration;
        }
    }
}
