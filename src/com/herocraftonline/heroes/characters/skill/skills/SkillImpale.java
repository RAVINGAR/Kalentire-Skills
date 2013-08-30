package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillImpale extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillImpale(Heroes plugin) {
        super(plugin, "Impale");
        setDescription("You impale your target with your weapon, dealing $1 damage, tossing them up in the air, and slowing them for $2 seconds.");
        setUsage("/skill impale");
        setArgumentRange(0, 0);
        setIdentifiers("skill impale");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.MOVEMENT_SLOWING, SkillType.FORCE, SkillType.INTERRUPTING);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(50), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(1.0), false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(3000), false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", damage + "").replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(9));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(50));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), Double.valueOf(1.0));
        node.set("weapons", Util.shovels);
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(3000));
        node.set("amplitude", Integer.valueOf(2));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% has been slowed by %hero%'s impale!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% is no longer slowed!");
        node.set("force", Double.valueOf(0.8));

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%target% has been slowed by %hero%'s impale!").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%target% is no longer slowed!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        Material item = player.getItemInHand().getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords).contains(item.name())) {
            Messaging.send(player, "You can't use " + getName() + " with that weapon!");
            return SkillResult.FAIL;
        }

        int force = SkillConfigManager.getUseSetting(hero, this, "force", 3, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(50), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(1.0), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        // Do a little knockup
        target.setVelocity(target.getVelocity().add(new Vector(0, force, 0)));

        // Add the slow effect
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int amplitude = SkillConfigManager.getUseSetting(hero, this, "amplitude", 2, false);
        SlowEffect sEffect = new SlowEffect(this, player, duration, amplitude, false, applyText, expireText);
        plugin.getCharacterManager().getCharacter(target).addEffect(new ImpaleEffect(this, player, 300, sEffect));

        // Play sound
        player.getWorld().playSound(player.getLocation(), Sound.HURT, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class ImpaleEffect extends ExpirableEffect {

        private final Effect effect;

        public ImpaleEffect(Skill skill, Player applier, long duration, Effect afterEffect) {
            super(skill, "Impale", applier, duration);
            this.effect = afterEffect;

            types.add(EffectType.HARMFUL);
            types.add(EffectType.DISABLE);
            types.add(EffectType.SLOW);

            addMobEffect(2, (int) (duration / 1000) * 20, 20, false);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            hero.addEffect(effect);
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            monster.addEffect(effect);
        }
    }
}
