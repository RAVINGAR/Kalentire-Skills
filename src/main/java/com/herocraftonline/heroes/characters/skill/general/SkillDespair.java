package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class SkillDespair extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillDespair(final Heroes plugin) {
        super(plugin, "Despair");
        setDescription("Blinds all enemies within $1 blocks for $2 seconds and dealing $3 dark damage.");
        setUsage("/skill despair");
        setArgumentRange(0, 0);
        setIdentifiers("skill despair");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DARK, SkillType.AREA_OF_EFFECT, SkillType.BLINDING, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 55, false);
        final double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.5, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 4000, false);
        final int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 50, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", radius + "").replace("$2", formattedDuration).replace("$3", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 55);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.5);
        node.set(SkillSetting.RADIUS.node(), 8);
        node.set(SkillSetting.DURATION.node(), 4000);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 50);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has blinded %target% with %skill%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has recovered their sight!");
        node.set(SkillSetting.REAGENT.node(), "ROTTEN_FLESH");
        node.set(SkillSetting.REAGENT_COST.node(), 1);

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has blinded %target% with %skill%!").replace("%hero%", "$2").replace("$hero$", "$2").replace("%target%", "$1").replace("$target$", "$1").replace("%skill%", "$3");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has recovered their sight!").replace("%hero%", "$2").replace("$hero$", "$2").replace("%target%", "$1").replace("$target$", "$1").replace("%skill%", "$3");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 55, false);
        final double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.5, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 4000, false);
        final int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 50, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        broadcastExecuteText(hero);

        final DespairEffect dEffect = new DespairEffect(this, player, duration);

        for (final Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity)) {
                continue;
            }
            final CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) entity);
            character.addEffect(dEffect);
            if (damage > 0) {
                addSpellTarget(entity, hero);
                damageEntity(character.getEntity(), player, damage, DamageCause.MAGIC);
            }
        }

        for (double r = 1; r < radius * 2; r++) {
            final List<Location> particleLocations = GeometryUtil.circle(player.getLocation(), 36, r / 2);
            for (final Location particleLocation : particleLocations) {
                //player.getWorld().spigot().playEffect(particleLocations.get(i), Effect.SPELL, 0, 0, 0, 0.1F, 0, 0.0F, 1, 16);
                player.getWorld().spawnParticle(Particle.SPELL, particleLocation, 1, 0, 0.1, 0, 0, null, true);
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.2F, 2.0F);

        return SkillResult.NORMAL;
    }

    public class DespairEffect extends ExpirableEffect {

        public DespairEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, "Despair", applier, duration);

            types.add(EffectType.HARMFUL);
            types.add(EffectType.DARK);
            types.add(EffectType.BLIND);
            types.add(EffectType.DISPELLABLE);

            addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) ((duration / 1000) * 20), 3), false);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName(), applier.getName(), "Despair");
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName(), applier.getName(), "Despair");
        }
    }
}