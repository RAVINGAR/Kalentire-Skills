
package com.herocraftonline.heroes.characters.skill.skills;
// old src  http://pastie.org/private/gfhf451ziiv1tbnkpufcwq

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

public class SkillChainLightning extends TargettedSkill {

    public SkillChainLightning(Heroes plugin) {
        super(plugin, "ChainLightning");
        setDescription("Strikes lightning at target location, dealing $1 damage. "
                + "Every $2 seconds, the spell will attempt to bounce to another player within $3 blocks, "
                + "dealing $4% damage on bounce. "
                + "Every successive hit will decrease the caster's cooldown of this skill by $5 seconds.");
        setIdentifiers("skill chainlightning");
        this.setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_LIGHTNING, SkillType.SILENCEABLE);
    }
    @Override
    public SkillResult use(Hero hero, String[] args) {

        if (hero.hasEffectType(EffectType.BLIND)) {
            Messaging.send(hero.getPlayer(), "You can't target anything while blinded!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        final LivingEntity target = getTarget(hero, hero.hasEffect("PowerLocusEffect") ? 100 : 16, args);

        if (target == null) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        else if ((args.length > 1) && (target != null)) {
            args = Arrays.copyOfRange(args, 1, args.length);
        }

        if (target != null && (target instanceof Player)) {
            final Hero tHero = plugin.getCharacterManager().getHero((Player) target);
            if (tHero.hasEffectType(EffectType.UNTARGETABLE)) {
                Messaging.send(hero.getPlayer(), "You cannot currently target this player!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
            else if (tHero.hasEffectType(EffectType.UNTARGETABLE)) {
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
        }

        final SkillResult result = use(hero, target, args);
        if (this.isType(SkillType.INTERRUPTING) && result.equals(SkillResult.NORMAL) && (target instanceof Player)) {
            final Hero tHero = plugin.getCharacterManager().getHero((Player) target);
            if (tHero.getDelayedSkill() != null) {
                tHero.cancelDelayedSkill();
                tHero.setCooldown("global", Heroes.properties.globalCooldown + System.currentTimeMillis());
            }
        }
        return result;
    }
    private LivingEntity getTarget(Hero hero, int maxDistance, String[] args) {
        final Player player = hero.getPlayer();
        LivingEntity target = null;
        if (args.length > 0) {
            target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                Messaging.send(player, "Invalid target!");
                return null;
            }
            if (!target.getLocation().getWorld().equals(player.getLocation().getWorld())) {
                Messaging.send(player, "Target is in a different dimension.");
                return null;
            }
            final int distSq = maxDistance * maxDistance;
            if (target.getLocation().distanceSquared(player.getLocation()) > distSq) {
                Messaging.send(player, "Target is too far away.");
                return null;
            }
            if (!inLineOfSight(player, (Player) target)) {
                Messaging.send(player, "Sorry, target is not in your line of sight!");
                return null;
            }
            if (target.isDead() || (target.getHealth() == 0)) {
                Messaging.send(player, "You can't target the dead!");
                return null;
            }
        }
        if (target == null) {
            target = getPlayerTarget(player, maxDistance);
            if (this.isType(SkillType.HEALING)) {
                if ((target instanceof Player) && hero.hasParty() && hero.getParty().isPartyMember((Player) target)) {
                    return target;
                }
                else if (target instanceof Player) {
                    return null;
                }
                else {
                    target = null;
                }
            }
        }
        if (target == null) {
            // don't self-target harmful skills
            if (this.isType(SkillType.DAMAGING)) {
                return null;
            }
            target = player;
        }

        // Do a PvP check automatically for any harmful skill
        if (this.isType(SkillType.DAMAGING)) {
            if (player.equals(target) || hero.getSummons().contains(target) || !damageCheck(player, target)) {
                Messaging.send(player, "Sorry, You can't damage that target!");
                return null;
            }
        }
        return target;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (target == null || hero.getEntity() == target) {
            if (target == null) {
                hero.getPlayer().sendMessage(ChatColor.GRAY + "[" + ChatColor.GREEN + "Skill" + ChatColor.GRAY + "] No Target!");
            } else {
                hero.getPlayer().sendMessage(ChatColor.GRAY + "[" + ChatColor.GREEN + "Skill" + ChatColor.GRAY + "] Cannot Target Yourself!");
            }
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100, false);
        long bounceTime = (long) (SkillConfigManager.getUseSetting(hero, this, "bounceTime", 2000, false));
        int bounceRadius = (int) SkillConfigManager.getUseSetting(hero, this, "bounceRadius", 5, false);
        double damageReductionPercent = SkillConfigManager.getUseSetting(hero, this, "bounceDamageMultiplier", 0.75, false);
        long cdr = (long) (SkillConfigManager.getUseSetting(hero, this, "bounceCooldownReduction", 1000, false));
        int maxBounce = (int) SkillConfigManager.getUseSetting(hero, this, "maxBounce", 5, false);
        CharacterTemplate cT = plugin.getCharacterManager().getCharacter(target);
        cT.addEffect(new MultiboltEffect(this, plugin, bounceTime, damage, hero, damageReductionPercent, bounceRadius, cdr, maxBounce));
        broadcast(hero.getPlayer().getLocation(), ChatColor.GRAY + "[" + ChatColor.GREEN + "Skill" + ChatColor.GRAY + "] " + hero.getName() + " used ChainLightning!");
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        DecimalFormat dF = new DecimalFormat("##.##");
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100, false);
        int bounceTime = (int) (SkillConfigManager.getUseSetting(hero, this, "bounceTime", 2000, false) * 0.001);
        int bounceRadius = SkillConfigManager.getUseSetting(hero, this, "bounceRadius", 5, false);
        double damageReductionPercent = SkillConfigManager.getUseSetting(hero, this, "bounceDamageMultiplier", 0.75, false);
        int cdr = (int) (SkillConfigManager.getUseSetting(hero, this, "bounceCooldownReduction", 1000, false) * 0.001);
        return getDescription().replace("$1",dF.format(damage))
                .replace("$2", bounceTime + "")
                .replace("$3", bounceRadius + "")
                .replace("$4", dF.format(damageReductionPercent * 100))
                .replace("$5", cdr + "");
    }

    public class MultiboltEffect extends ExpirableEffect {

        private double damage;
        private Hero caster;
        private double bouncePercent;
        private int bounceRadius;
        private long cdr;
        private long bounceTime;
        private int remaining;
        public MultiboltEffect(Skill skill, Heroes plugin, long duration, double damage, Hero caster, double bouncePercent, int bounceRadius, long cdr, int remaining) {
            this(skill, "MultiboltEffect" + System.currentTimeMillis(), plugin , duration, damage, caster, bouncePercent, bounceRadius, cdr, remaining);
        }
        public MultiboltEffect(Skill skill, String name, Heroes plugin, long duration, double damage, Hero caster, double bouncePercent, int bounceRadius, long cdr, int remaining) {
            super(skill, plugin, name, caster.getPlayer(), duration);
            this.damage = damage;
            this.caster = caster;
            this.bouncePercent = bouncePercent;
            this.bounceRadius = bounceRadius;
            this.cdr = cdr;
            this.bounceTime = duration;
            this.remaining = remaining;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.HARMFUL);
        }

        @Override
        public void apply(CharacterTemplate cT) {
            super.apply(cT);
            addSpellTarget(cT.getEntity(),caster);
            Skill.damageEntity(cT.getEntity(), caster.getEntity(), damage, DamageCause.MAGIC);
            if (cT instanceof Hero) {
                ((Player)(cT.getEntity())).sendMessage(ChatColor.GRAY + "[" + ChatColor.GREEN + "Skill" + ChatColor.GRAY + "] Hit by Arcane Multibolt from " + caster.getName() + "!");
            }
            cT.getEntity().getLocation().getWorld().strikeLightningEffect(cT.getEntity().getLocation());
            cT.getEntity().getWorld().playSound(cT.getEntity().getLocation(), Sound.AMBIENCE_THUNDER, getLightningVolume(caster), 1.0F);
        }

        @Override
        public void remove(CharacterTemplate cT) {
            super.remove(cT);
            remaining--;
            if (remaining <= 0) {
                return;
            }
            LivingEntity target = null;
            List<Entity> nearby = cT.getEntity().getNearbyEntities(bounceRadius, bounceRadius, bounceRadius);
            for (Entity e : nearby) {
                if (e instanceof Player) {
                    if (((LivingEntity)e).equals(cT.getEntity())) {
                        continue;
                    }
                    if (((Player)e).equals(caster.getEntity())) {
                        continue;
                    }
                    if (Skill.damageCheck(caster.getPlayer(), (LivingEntity)e)) {
                        target = (Player)e;
                        break;
                    }
                }
            }
            if (target == null) {
                for (Entity e : nearby) {
                    if (e instanceof LivingEntity) {
                        if (((LivingEntity)e).equals(cT.getEntity())) {
                            continue;
                        }
                        if (((LivingEntity)e).equals(caster.getEntity())) {
                            continue;
                        }
                        if (Skill.damageCheck(caster.getPlayer(), (LivingEntity)e)) {
                            target = (LivingEntity)e;
                        }
                    }
                }
            }
            if (target != null) {
                Long cd = caster.getCooldown(skill.getName());
                if (cd != null) {
                    caster.setCooldown(skill.getName(), cd-cdr);
                }
                CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                targetCT.addEffect(new MultiboltEffect(skill,plugin,bounceTime,damage,caster,bouncePercent,bounceRadius,cdr,remaining));
                return;
            } else {
                return;
            }
        }

    }
    public float getLightningVolume(Hero h) {
        return (float) SkillConfigManager.getUseSetting(h, this, "lightning-volume", 0.0F, false);
    }
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), (double) 100);
        node.set("bounceTime", (long) 2000);
        node.set("bounceRadius", 5);
        node.set("bounceDamageMultiplier", 0.75);
        node.set("bounceCooldownReduction", (long) 1000);
        node.set("lightning-volume", 0.0F);
        return node;

    }

}