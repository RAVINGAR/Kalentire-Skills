package com.herocraftonline.heroes.characters.skill.pack5;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SkillGroupTeleport extends ActiveSkill {

    public SkillGroupTeleport(Heroes plugin) {
        super(plugin, "GroupTeleport");
        setDescription("You summon your group to your location.");
        setUsage("/skill groupteleport");
        setIdentifiers("skill groupteleport", "skill gtp");
        setArgumentRange(0, 0);
        setTypes(SkillType.TELEPORTING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.UNBINDABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("caster-combat-check", true);
        config.set("party-combat-check", true);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        if (hero.getParty() == null || hero.getParty().getMembers().size() == 1) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You need a party to teleport other players!");
            return SkillResult.FAIL;
        }

        boolean casterCombatCheck = SkillConfigManager.getUseSetting(hero, this, "caster-combat-check", true);
        if (casterCombatCheck && hero.isInCombat()) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "Cannot teleport others while you are in combat!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);

        boolean tpdAtLeastOne = false;
        boolean partyCombatCheck = SkillConfigManager.getUseSetting(hero, this, "party-combat-check", true);
        for (Hero partyHero : hero.getParty().getMembers()) {
            Player partyPlayer = partyHero.getPlayer();
            if (partyHero.equals(hero) || !partyPlayer.getWorld().equals(player.getWorld()))
                continue;

            if (partyCombatCheck && partyHero.isInCombat()) {
                player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "Cannot teleport " + partyPlayer.getName() + " - they are in combat!");
                partyPlayer.sendMessage("    " + ChatComponents.GENERIC_SKILL + player.getName() + " attempted to teleport you, but you are in combat!");
                continue;
            }

            Util.playClientEffect(partyPlayer, "enchantmenttable", new Vector(0, 0, 0), 1F, 10, true);
            Util.playClientEffect(partyPlayer, "largeexplode", new Vector(0, 0, 0), 1F, 10, true);
            partyPlayer.getWorld().playSound(partyPlayer.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5F, 1.0F);

            partyPlayer.teleport(player);
            tpdAtLeastOne = true;
        }

        if (!tpdAtLeastOne) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You failed to find any valid party members to teleport. You spell was wasted!");
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }
}
