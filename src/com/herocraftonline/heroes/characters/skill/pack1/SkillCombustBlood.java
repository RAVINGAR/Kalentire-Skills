package com.herocraftonline.heroes.characters.skill.pack1;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;

public class SkillCombustBlood extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillCombustBlood(Heroes plugin) {
        super(plugin, "CombustBlood");
        setDescription("Boil the blood of your target, dealing $1 dark damage. If you have Blood Union $2 or greater, the target will bleed, taking an additional $3 damage over $4 seconds. Increases Blood Union by 1.");
        setUsage("/skill combustblood");
        setArgumentRange(0, 0);
        setIdentifiers("skill combustblood");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_DARK);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.2);
        node.set(SkillSetting.DAMAGE.node(), 95);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.75);
        node.set("blood-union-increase", 1);
        node.set("blood-union-required-for-dot", 3);
        node.set(SkillSetting.DAMAGE_TICK.node(), 14);
        node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 0.15);
        node.set(SkillSetting.PERIOD.node(), 2500);
        node.set(SkillSetting.DURATION.node(), 7500);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is bleeding from the effects of their Combusted Blood!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer bleeding.");

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int bloodUnionReq = SkillConfigManager.getUseSetting(hero, this, "blood-union-required-for-dot", 3, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 95, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.75, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 14, false);
        double tickDamageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 0.15, false);
        tickDamage += (int) (tickDamageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDotDamage = Util.decFormat.format((tickDamage * ((double) duration / (double) period)));

        return getDescription().replace("$1", damage + "").replace("$2", bloodUnionReq + "").replace("$3", formattedDotDamage).replace("$4", formattedDuration);
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% is bleeding from the effects of their Combusted Blood!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% is no longer bleeding.").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 70, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.25, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        // Deal damage
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        broadcastExecuteText(hero, target);
        
        player.getWorld().spigot().playEffect(target.getLocation(), Effect.LAVA_POP, 0, 0, 0, 0, 0, 1, 75, 16);
        player.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.EXPLOSION_LARGE, 0, 0, 0, 0, 0, 0, 10, 16);
        player.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.TILE_BREAK, Material.NETHER_WARTS.getId(), 0, 0, 0.1F, 0, 0.1F, 16, 16);
        player.getWorld().playSound(target.getLocation(), CompatSound.ENTITY_GENERIC_EXPLODE.value(), 10.0F, 16);
        player.getWorld().playSound(target.getLocation(), CompatSound.BLOCK_FIRE_AMBIENT.value(), 10.0F, 16);

        // Get Blood Union Level
        int bloodUnionLevel = 0;
        if (hero.hasEffect("BloodUnionEffect")) {
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");

            bloodUnionLevel = buEffect.getBloodUnionLevel();
        }

        // Add DoT if blood union is high enough.
        int bloodUnionRequirement = SkillConfigManager.getUseSetting(hero, this, "blood-union-required-for-dot", 3, false);
        if (bloodUnionLevel >= bloodUnionRequirement) {

            // Get DoT values
            double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 14, false);
            double tickDamageIncrease = hero.getAttributeValue(AttributeType.INTELLECT) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 0.15, false);
            tickDamage += tickDamageIncrease;

            int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);
            int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);

            // Add DoT effect
            CombustingBloodEffect cbEffect = new CombustingBloodEffect(this, player, period, duration, tickDamage);
            CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
            targCT.addEffect(cbEffect);
        }

        // Increase Blood Union
        if (hero.hasEffect("BloodUnionEffect")) {
            int bloodUnionIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-increase", 1, false);
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");

            if (target instanceof Player)
                buEffect.addBloodUnion(bloodUnionIncrease, true);
            else
                buEffect.addBloodUnion(bloodUnionIncrease, false);
        }

        return SkillResult.NORMAL;
    }

    public class CombustingBloodEffect extends PeriodicDamageEffect {

        public CombustingBloodEffect(Skill skill, Player applier, long period, long duration, double tickDamage) {
            super(skill, "CombustingBlood", applier, period, duration, tickDamage);

            types.add(EffectType.BLEED);
            types.add(EffectType.DARK);
            types.add(EffectType.HARMFUL);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + applyText, CustomNameManager.getName(monster), applier.getName());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, CustomNameManager.getName(monster), applier.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }
    }
}
