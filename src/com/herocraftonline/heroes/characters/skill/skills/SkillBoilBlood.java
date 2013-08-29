package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillBoilBlood extends ActiveSkill {
    public VisualEffect fplayer = new VisualEffect();

    public SkillBoilBlood(Heroes plugin) {
        super(plugin, "BoilBlood");
        setDescription("Boil the blood of $6 enemies within $1 blocks, dealing $2 instant damage, and doing an additional $3 damage over $4 seconds. Requires $5 Blood Union to use. Reduces Blood Union by $5.");
        setUsage("/skill boilblood");
        setArgumentRange(0, 0);
        setIdentifiers("skill boilblood");
        setTypes(SkillType.DAMAGING, SkillType.AREA_OF_EFFECT, SkillType.SILENCABLE, SkillType.AGGRESSIVE);
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

        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(70));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(1.25));
        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(5));
        node.set(SkillSetting.DAMAGE_TICK.node(), Integer.valueOf(10));
        node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.15));
        node.set(SkillSetting.PERIOD.node(), Integer.valueOf(2000));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(12000));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target%'s blood begins to boil!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target%'s blood is no longer boiling.");
        node.set("blood-union-required-for-use", Integer.valueOf(3));
        node.set("max-targets", Integer.valueOf(6));

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

        // Create DoT effect
        BoilingBloodEffect bbEffect = new BoilingBloodEffect(this, period, duration, tickDamage, player, applyText, expireText);

        int targetsHit = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (maxTargets > 0 && targetsHit >= maxTargets)
                break;

            // Check to see if the entity can be damaged
            if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity))
                continue;

            LivingEntity target = (LivingEntity) entity;

            try {
                this.fplayer.playFirework(player.getWorld(), target.getLocation().add(0.0D, 1.5D, 0.0D), FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.BALL).withColor(Color.RED).withFade(Color.BLACK).build());
            }
            catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC);

            // Add DoT effect to target
            CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
            targCT.addEffect(bbEffect);

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
        private final Player applier;

        public BoilingBloodEffect(Skill skill, long period, long duration, double tickDamage, Player applier, String applyText, String expireText) {
            super(skill, "BoilingBlood", period, duration, tickDamage, applier, false);

            this.applyText = applyText;
            this.expireText = expireText;
            this.applier = applier;

            types.add(EffectType.DARK);
            types.add(EffectType.HARMFUL);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), applyText, Messaging.getLivingEntityName(monster), applier.getDisplayName());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), expireText, Messaging.getLivingEntityName(monster), applier.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }
}