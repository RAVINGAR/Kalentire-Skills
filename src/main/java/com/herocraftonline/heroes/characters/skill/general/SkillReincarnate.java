package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillReincarnate extends ActiveSkill {

    public SkillReincarnate(Heroes plugin) {
        super(plugin, "Reincarnate");
        setDescription("Reincarnate your target, teleporting them to their place of death.");
        setUsage("/skill reincarnate <target>");
        setArgumentRange(1, 1);
        setIdentifiers("skill reincarnate");
        setTypes(SkillType.RESURRECTING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DELAY.node(), 8000);
        node.set(SkillSetting.REAGENT.node(), "POPPY");
        node.set(SkillSetting.REAGENT_COST.node(), 1);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        if (args.length < this.getMinArguments() || args.length > this.getMaxArguments()) {
            return SkillResult.INVALID_TARGET;
        }

        Player player = hero.getPlayer();
        Player target = plugin.getServer().getPlayer(args[0]);

        if (target == null)
            return SkillResult.INVALID_TARGET;

        String targetName = target.getName();
        if (!Util.deaths.containsKey(targetName)) {
            player.sendMessage(targetName +" has not died recently.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Location deathLoc = Util.deaths.get(targetName);
        Location playerLoc = player.getLocation();
        if (!playerLoc.getWorld().equals(deathLoc.getWorld()) || playerLoc.distance(deathLoc) > 50.0) {
            player.sendMessage("You are out of range.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        if (target.isDead()) {
            player.sendMessage(targetName+ " is still dead.");
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
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }
}
