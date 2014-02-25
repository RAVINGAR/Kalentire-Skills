package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
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
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillDespair extends ActiveSkill {
    private String applyText;
    private String expireText;

    public VisualEffect fplayer = new VisualEffect(); //fireworks

    public SkillDespair(Heroes plugin) {
        super(plugin, "Despair");
        setDescription("Blinds all enemies within $1 blocks for $2 seconds and dealing $3 dark damage.");
        setUsage("/skill despair");
        setArgumentRange(0, 0);
        setIdentifiers("skill despair");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.AREA_OF_EFFECT, SkillType.BLINDING, SkillType.DAMAGING, SkillType.SILENCABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 55, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.5, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 4000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 50, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", radius + "").replace("$2", formattedDuration).replace("$3", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(55));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.5));
        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(8));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(4000));
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), Integer.valueOf(50));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% has blinded %target% with %skill%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% has recovered their sight!");
        node.set(SkillSetting.REAGENT.node(), Integer.valueOf(367));
        node.set(SkillSetting.REAGENT_COST.node(), Integer.valueOf(1));

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% has blinded %target% with %skill%!").replace("%hero%", "$2").replace("%target%", "$1").replace("%skill%", "$3");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% has recovered their sight!").replace("%hero%", "$2").replace("%target%", "$1").replace("%skill%", "$3");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 55, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.5, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 4000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 50, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        broadcastExecuteText(hero);

        DespairEffect dEffect = new DespairEffect(this, player, duration);

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity)) {
                continue;
            }
            CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) entity);
            character.addEffect(dEffect);
            if (damage > 0) {
                addSpellTarget(entity, hero);
                damageEntity(character.getEntity(), player, damage, DamageCause.MAGIC);
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.GHAST_SCREAM, 1.2F, 2.0F);

        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), player.getLocation().add(0, 2.5, 0), FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.BURST).withColor(Color.NAVY).withFade(Color.BLACK).build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
    }

    public class DespairEffect extends ExpirableEffect {

        public DespairEffect(Skill skill, Player applier, long duration) {
            super(skill, "Despair", applier, duration);

            types.add(EffectType.HARMFUL);
            types.add(EffectType.DARK);
            types.add(EffectType.BLIND);
            types.add(EffectType.DISPELLABLE);

            addMobEffect(15, (int) ((duration / 1000) * 20), 3, false);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName(), applier.getName(), "Despair");
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName(), applier.getName(), "Despair");
        }
    }
}