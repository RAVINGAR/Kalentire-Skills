package com.herocraftonline.heroes.characters.skill.skills;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillDoomwave extends ActiveSkill {

    private Map<EnderPearl, Long> doomPearls = new LinkedHashMap<EnderPearl, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        @Override
        protected boolean removeEldestEntry(Entry<EnderPearl, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    private boolean ncpEnabled = false;

    public SkillDoomwave(Heroes plugin) {
        super(plugin, "Doomwave");
        setDescription("Unleash a wave of doom around you. Doomwave will launch $1 fiery ender pearls in all directions around you. Each pearl will deal $2 damage to targets hit, and teleport you to each location.");
        setUsage("/skill doomwave");
        setArgumentRange(0, 0);
        setTypes(SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_FIRE, SkillType.AREA_OF_EFFECT, SkillType.SILENCABLE);
        setIdentifiers("skill doomwave");
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null)
            ncpEnabled = true;

    }

    @Override
    public String getDescription(Hero hero) {

        int numEnderPearls = SkillConfigManager.getUseSetting(hero, this, "enderpearls-launched", 12, false);
        double numEnderPearlsIncrease = SkillConfigManager.getUseSetting(hero, this, "enderpearls-launched-per-intellect", 0.2, false);
        numEnderPearls += (int) (numEnderPearlsIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.25, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", numEnderPearls + "").replace("$2", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("enderpearls-launched", Integer.valueOf(12));
        node.set("enderpearls-launched-per-intellect", Double.valueOf(0.325));
        node.set("velocity-multiplier", Double.valueOf(1.0));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(90));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(1.5));
        node.set(SkillSetting.REAGENT.node(), Integer.valueOf(289));
        node.set(SkillSetting.REAGENT_COST.node(), Integer.valueOf(1));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int numEnderPearls = SkillConfigManager.getUseSetting(hero, this, "enderpearls-launched", 12, false);
        double numEnderPearlsIncrease = SkillConfigManager.getUseSetting(hero, this, "enderpearls-launched-per-intellect", 0.325, false);
        numEnderPearls += (int) (numEnderPearlsIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        broadcastExecuteText(hero);

        // Let's bypass the nocheat issues...
        if (ncpEnabled) {
            if (!player.isOp()) {
                NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this);
                hero.addEffect(ncpExemptEffect);
            }
        }

        long time = System.currentTimeMillis();
        Random ranGen = new Random((int) ((time / 2.0) * 12));

        double velocityMultiplier = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", Double.valueOf(0.75), false);

        for (double i = 0; i < numEnderPearls; i++) {
            EnderPearl doomPearl = player.launchProjectile(EnderPearl.class);
            doomPearl.setFireTicks(100);

            double randomX = ranGen.nextGaussian();
            double randomY = ranGen.nextGaussian();
            double randomZ = ranGen.nextGaussian();

            Vector vel = new Vector(randomX, randomY, randomZ);
            doomPearl.setVelocity(vel.multiply(velocityMultiplier));

            doomPearls.put(doomPearl, time);
        }

        // Let's bypass the nocheat issues...
        if (ncpEnabled) {
            if (!player.isOp()) {
                if (hero.hasEffect("NCPExemptionEffect_FIGHT"))
                    hero.removeEffect(hero.getEffect("NCPExemptionEffect_FIGHT"));
            }
        }

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler()
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            Entity projectile = subEvent.getDamager();
            if (!(projectile instanceof EnderPearl) || !doomPearls.containsKey(projectile)) {
                return;
            }

            doomPearls.remove(projectile);
            LivingEntity targetLE = (LivingEntity) subEvent.getEntity();
            Entity dmger = ((EnderPearl) projectile).getShooter();
            if (dmger instanceof Player) {
                Hero hero = plugin.getCharacterManager().getHero((Player) dmger);

                if (!damageCheck((Player) dmger, targetLE)) {
                    event.setCancelled(true);
                    return;
                }

                // Check if entity is immune to further firewave hits
                if (plugin.getCharacterManager().getCharacter(targetLE).hasEffect("DoomWaveAntiMultiEffect")) {
                    event.setCancelled(true);
                    return;
                }

                // Ignite the player
                targetLE.setFireTicks(SkillConfigManager.getUseSetting(hero, skill, "fire-ticks", 50, false));
                plugin.getCharacterManager().getCharacter(targetLE).addEffect(new CombustEffect(skill, (Player) dmger));

                // Damage the player
                double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 80, false);
                double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.5, false);
                damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

                // Damage the target
                addSpellTarget(targetLE, hero);
                damageEntity(targetLE, hero.getPlayer(), damage, DamageCause.MAGIC);
                event.setCancelled(true);

                //Adds an Effect to Prevent Multihit
                plugin.getCharacterManager().getCharacter(targetLE).addEffect(new ExpirableEffect(skill, "DoomWaveAntiMultiEffect", (Player) dmger, 500));
            }
        }
    }

    private class NCPExemptionEffect extends Effect {

        public NCPExemptionEffect(Skill skill) {
            super(skill, "NCPExemptionEffect_FIGHT");
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.FIGHT);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.FIGHT);
        }
    }
}