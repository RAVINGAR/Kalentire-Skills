package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillBoilBlood extends ActiveSkill {

    public SkillBoilBlood(Heroes plugin) {
        super(plugin, "BoilBlood");
        setDescription("Boil the blood of $6 enemies within $1 blocks, dealing $2 instant damage, and doing an additional $3 damage over $4 seconds. Requires $5 Blood Union to use. Reduces Blood Union by $5.");
        setUsage("/skill boilblood");
        setArgumentRange(0, 0);
        setIdentifiers("skill boilblood");
        setTypes(SkillType.DAMAGING, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        int bloodUnionReq = SkillConfigManager.getUseSetting(hero, this, "blood-union-required-for-use", 3, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 70, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.25, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 10, false);
        double tickDamageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 0.15, false);
        tickDamage += (tickDamageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int maxTargets = SkillConfigManager.getUseSetting(hero, this, "max-targets", 6, false);

        // Change description to either say "all" if there is no maximum target number specified.
        String targetText;
        if (maxTargets > 0)
            targetText = "up to " + maxTargets;
        else
            targetText = "all";

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDotDamage = Util.decFormat.format((tickDamage * ((double) duration / (double) period)));

        return getDescription().replace("$6", targetText).replace("$1", radius + "").replace("$2", damage + "").replace("$3", formattedDotDamage + "").replace("$4", formattedDuration + "").replace("$5", bloodUnionReq + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 70);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.25);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set(SkillSetting.DAMAGE_TICK.node(), 10);
        node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 0.15);
        node.set(SkillSetting.PERIOD.node(), 2000);
        node.set(SkillSetting.DURATION.node(), 12000);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target%'s blood begins to boil!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target%'s blood is no longer boiling.");
        node.set("blood-union-required-for-use", 3);
        node.set("max-targets", 6);

        return node;
    }

    public SkillResult use(Hero hero, String[] args) {

        Player player = hero.getPlayer();

        // Get Blood Union Level
        int bloodUnionLevel = 0;
        if (hero.hasEffect("BloodUnionEffect")) {
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");

            bloodUnionLevel = buEffect.getBloodUnionLevel();
        }

        // Add DoT if blood union is high enough.
        int bloodUnionReq = SkillConfigManager.getUseSetting(hero, this, "blood-union-required-for-use", 3, false);

        if (bloodUnionLevel < bloodUnionReq) {

            //Messaging.send(player, "You must have at least " + bloodUnionReq + " Blood Union to use this ability!", new Object[0]);
            Messaging.send(player, "You must have at least " + bloodUnionReq + " Blood Union to use this ability!", new Object[0]);
            return SkillResult.FAIL;
        }

        // Blood Union high enough, proceed.

        broadcastExecuteText(hero);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 70, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.25, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        // Get DoT values
        double tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 10, false);
        double tickDamageIncrease = hero.getAttributeValue(AttributeType.INTELLECT) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 0.15, false);
        tickDamage += tickDamageIncrease;

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        String applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%target%'s blood begins to boil!").replace("%target%", "$1");
        String expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%target%'s blood is no longer boiling.").replace("%target%", "$1");

        int maxTargets = SkillConfigManager.getUseSetting(hero, this, "max-targets", 0, false);

        int targetsHit = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (maxTargets > 0 && targetsHit >= maxTargets)
                break;

            // Check to see if the entity can be damaged
            if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity))
                continue;

            LivingEntity target = (LivingEntity) entity;
            
            player.getWorld().spigot().playEffect(target.getLocation(), Effect.LARGE_SMOKE, 0, 0, 0, 0, 0, 0.2F, 50, 16);
            player.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.TILE_BREAK, Material.NETHER_WARTS.getId(), 0, 0, 0.1F, 0, 0.1F, 16, 16);
            player.getWorld().playSound(target.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 10.0F, 16);

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC);

            // Create and add DoT effect to target
            CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
            targCT.addEffect(new BoilingBloodEffect(this, player, period, duration, tickDamage, applyText, expireText));

            // Increase counter
            targetsHit++;
        }

        // Decrease Blood Union
        BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");
        buEffect.decreaseBloodUnion(bloodUnionReq);

        return SkillResult.NORMAL;
    }

    public class BoilingBloodEffect extends PeriodicDamageEffect {
        private final String applyText;
        private final String expireText;

        public BoilingBloodEffect(Skill skill, Player applier, long period, long duration, double tickDamage, String applyText, String expireText) {
            super(skill, "BoilingBlood", applier, period, duration, tickDamage, false);

            types.add(EffectType.DARK);
            types.add(EffectType.HARMFUL);

            this.applyText = applyText;
            this.expireText = expireText;
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + applyText, Messaging.getLivingEntityName(monster), applier.getName());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName(), applier.getName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, Messaging.getLivingEntityName(monster), applier.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName(), applier.getName());
        }
    }
}