package com.herocraftonline.heroes.characters.skill.pack8;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;

public class SkillSmokeBomb extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillSmokeBomb(Heroes plugin) {
        super(plugin, "SmokeBomb");
        setDescription("Vanish in a smokebomb! You will not be visible to other players for the next $1 seconds. Taking damage or using abilities will cause you to reappear.");
        setUsage("/skill smoke");
        setArgumentRange(0, 0);
        setIdentifiers("skill smokebomb");
        setNotes("Note: Interacting with anything removes the effect.");
        setNotes("Note: Taking damage removes the effect.");
        setNotes("Note: Using skills removes the effect.");
        setTypes(SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.BUFFING, SkillType.STEALTHY);

        //Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 5500);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "Someone vanished in a cloud of smoke!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has reappeared!");
        node.set(SkillSetting.REAGENT.node(), "GUNPOWDER");
        node.set(SkillSetting.REAGENT_COST.node(), 1);

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "Someone vanished in a cloud of smoke!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has reappeared!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        Player player = hero.getPlayer();

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4500, false);

        //Util.playClientEffect(player, Particle.EXPLOSION_HUGE.toString(), new Vector(0, 0, 0), 1F, 10, true);
        player.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, player.getEyeLocation(), 10, 0.5, 0.3, 0.5, 1F);
        hero.addEffect(new SmokeEffect(this, player, duration));

        return SkillResult.NORMAL;
    }

    public class SmokeEffect extends InvisibleEffect {

        public SmokeEffect(Skill skill, Player applier, long duration) {
            super(skill, applier, duration, null, null);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();
            if (applyText != null && applyText.length() > 0) {
                // Override the standard invis effect message display so that we actually display a message to nearby players
                //      even though we have a "silent actions" effect type.
                broadcast(player.getLocation(), "    " + applyText, player.getName());
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            if (expireText != null && expireText.length() > 0) {
                // Override the standard invis effect message display so that we actually display a message to nearby players
                //      even though we have a "silent actions" effect type.
                broadcast(player.getLocation(), "    " + expireText, player.getName());
            }
        }
    }
}
