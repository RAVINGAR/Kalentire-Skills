package com.herocraftonline.heroes.characters.skill.public1;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;

public class SkillAwaken extends ActiveSkill {

    public SkillAwaken(Heroes plugin) {
        super(plugin, "Awaken");
        setDescription("Sacrifice a portion of your health to awaken your target, bringing them back to life.");
        setUsage("/skill awaken <target>");
        setArgumentRange(1, 1);
        setIdentifiers("skill awaken");
        setTypes(SkillType.RESURRECTING, SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DELAY.node(), 7500);
        node.set(SkillSetting.REAGENT.node(), 38);
        node.set(SkillSetting.REAGENT_COST.node(), 0);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Player target = plugin.getServer().getPlayer(args[0]);

        if (target == null)
            return SkillResult.INVALID_TARGET;

        String targetName = target.getName();
        if (!Util.deaths.containsKey(targetName)) {
            player.sendMessage(targetName + " has not died recently.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Location deathLoc = Util.deaths.get(targetName);
        Location playerLoc = player.getLocation();
        if (!playerLoc.getWorld().equals(deathLoc.getWorld()) || playerLoc.distance(deathLoc) > 50.0) {
            player.sendMessage("You are out of range.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        if (target.isDead()) {
            player.sendMessage(targetName + " is still dead.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Hero targetHero = plugin.getCharacterManager().getHero(target);
        if (!hero.hasParty() || !hero.getParty().isPartyMember(targetHero)) {
            player.sendMessage("The person needs to be in your party to do that!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero);

        target.teleport(playerLoc);

        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_WITHER_SPAWN.value(), 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }
}
