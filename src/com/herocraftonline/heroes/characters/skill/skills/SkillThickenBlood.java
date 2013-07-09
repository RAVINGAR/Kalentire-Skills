package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
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

public class SkillThickenBlood extends TargettedSkill {
    public VisualEffect fplayer = new VisualEffect();

    public SkillThickenBlood(Heroes plugin) {
        super(plugin, "ThickenBlood");
        setDescription("Thicken the blood of your target, causing them to degenerate a large amount of stamina over $1 seconds.");
        setUsage("/skill thickenblood");
        setArgumentRange(0, 0);
        setIdentifiers("skill thickenblood");
        setTypes(SkillType.SILENCABLE, SkillType.HARMFUL, SkillType.DEBUFF);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("hunger-value", 80);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.APPLY_TEXT.node(), "§7[§2Skill§7] %target%'s blood begins to thicken!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "§7[§2Skill§7] %target%'s blood returns to normal.");

        return node;
    }

    public String getDescription(Hero hero) {

        double duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false) / 1000;

        return getDescription().replace("$1", duration + "");
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;

        Player player = hero.getPlayer();

        // Get Debuff values
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        int hungerValue = SkillConfigManager.getUseSetting(hero, this, "hunger-value", 80, false);
        String applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "§7[§2Skill§7] %target%'s blood begins to thicken!").replace("%target%", "$1");
        String expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "§7[§2Skill§7] %target%'s blood returns to normal.").replace("%target%", "$1");

        broadcastExecuteText(hero, target);

        // Add effect
        ThickenBloodEffect tbEffect = new ThickenBloodEffect(this, hungerValue, duration, player, applyText, expireText);
        CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
        targCT.addEffect(tbEffect);

        return SkillResult.NORMAL;
    }

    public class ThickenBloodEffect extends ExpirableEffect {

        private final String applyText;
        private final String expireText;
        private final Player applier;

        public ThickenBloodEffect(Skill skill, int hungerValue, long duration, Player applier, String applyText, String expireText) {
            super(skill, "ThickenBloodEffect", duration);

            this.applyText = applyText;
            this.expireText = expireText;
            this.applier = applier;

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);

            final int tickDuration = (int) (duration / 1000) * 20;
            addMobEffect(17, tickDuration, hungerValue, false);
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