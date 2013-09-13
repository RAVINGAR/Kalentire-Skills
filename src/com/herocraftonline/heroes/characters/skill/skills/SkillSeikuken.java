package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
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
        setDescription("Hone your martial art skill to it's fullest extent for $1 seconds. During the duration, you control everything within your arms lengths and retaliate against all melee attacks, disarmimg them for $2 seconds, and dealing $3 physical damage.");
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

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(30), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(0.5), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDisarmDuration = Util.decFormat.format(disarmDuration / 1000.0);
        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedDisarmDuration).replace("$3", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(45));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), Double.valueOf(0.625));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(7500));
        node.set("slow-amplifier", Integer.valueOf(3));
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

                    double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, Integer.valueOf(30), false);
                    double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(0.5), false);
                    damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

                    Location damagerPlayerLoc = damagerPlayer.getLocation();
                    Location originalLoc = player.getLocation();
                    Location flippedLoc = new Location(originalLoc.getWorld(), originalLoc.getX(), originalLoc.getY(), originalLoc.getZ(), damagerPlayerLoc.getYaw() * -1, damagerPlayerLoc.getPitch() * -1);
                    player.teleport(flippedLoc);

                    event.setCancelled(true);
                    
                    addSpellTarget((Player) damagerPlayer, hero);
                    damageEntity((Player) damagerPlayer, player, damage, DamageCause.ENTITY_ATTACK);

                    damagerPlayer.getWorld().playSound(damagerPlayer.getLocation(), Sound.ITEM_BREAK, 0.8F, 1.0F);
                    damagerPlayer.getWorld().playSound(damagerPlayer.getLocation(), Sound.HURT, 0.8F, 0.5F);

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
