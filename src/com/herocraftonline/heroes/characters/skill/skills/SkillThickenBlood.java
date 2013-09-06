package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillThickenBlood extends TargettedSkill {
    public VisualEffect fplayer = new VisualEffect();
    private String applyText;
    private String expireText;

    public SkillThickenBlood(Heroes plugin) {
        super(plugin, "ThickenBlood");
        setDescription("Thicken the blood of your target, causing them to be inable to use stamina for $1 seconds.");
        setUsage("/skill thickenblood");
        setArgumentRange(0, 0);
        setIdentifiers("skill thickenblood");
        setTypes(SkillType.SILENCABLE, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_DARK, SkillType.STAMINA_FREEZING, SkillType.DEBUFFING);
    }

    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(3000), false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(7));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(2000));
        node.set(SkillSetting.DURATION_INCREASE_PER_INTELLECT.node(), Integer.valueOf(75));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target%'s blood has thickened!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target%'s blood returns to normal.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%target%'s blood has thickened!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%target%'s blood returns to normal.").replace("%target%", "$1");
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;

        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(4), false);

        // Get Debuff values
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(2000), false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_INTELLECT, Integer.valueOf(75), false);
        duration += hero.getAttributeValue(AttributeType.INTELLECT) * durationIncrease;

        // Famine the first target
        ThickenBloodEffect tbEffect = new ThickenBloodEffect(this, player, duration);
        CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
        targCT.addEffect(tbEffect);

        // Famine the rest
        for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity) {
                if (!damageCheck(player, (LivingEntity) entity)) {
                    continue;
                }

                CharacterTemplate newTargCT = plugin.getCharacterManager().getCharacter((LivingEntity) entity);
                newTargCT.addEffect(tbEffect);
            }
        }

        try {
            fplayer.playFirework(target.getWorld(),
                                 target.getLocation(),
                                 FireworkEffect.builder()
                                               .flicker(false).trail(true)
                                               .with(FireworkEffect.Type.BURST)
                                               .withColor(Color.GREEN)
                                               .withFade(Color.BLACK)
                                               .build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
    }

    public class ThickenBloodEffect extends ExpirableEffect {
        private int originalStamina;

        public ThickenBloodEffect(Skill skill, Player applier, int duration) {
            super(skill, "ThickenedBlood", applier, duration);

            types.add(EffectType.MAGIC);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.HUNGER);
            types.add(EffectType.STAMINA_FREEZING);

            addMobEffect(17, (int) (duration / 1000) * 20, 0, false);
        }

        @Override
        public void applyToMonster(Monster monster) {}

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            this.originalStamina = hero.getStamina();
            hero.setStamina(0);

            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromMonster(Monster monster) {}

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            hero.setStamina(originalStamina);

            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }
}