package com.herocraftonline.heroes.characters.skill.reborn;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;

public class SkillGroupTeleport extends ActiveSkill {

    public SkillGroupTeleport(Heroes plugin) {
        super(plugin, "GroupTeleport");
        setDescription("You summon your group to your location.");
        setUsage("/skill groupteleport");
        setArgumentRange(0, 0);
        setIdentifiers("skill groupteleport", "skill gteleport");
        setTypes(SkillType.TELEPORTING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.UNBINDABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        if (hero.getParty() == null || hero.getParty().getMembers().size() == 1) {
            player.sendMessage("You must actually have party members to teleport them to you!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);

        for (Hero partyHero : hero.getParty().getMembers()) {
            Player partyPlayer = partyHero.getPlayer();
            if (partyHero.equals(hero) || !partyPlayer.getWorld().equals(player.getWorld())) {
                continue;
            }
            if (partyHero.isInCombat()) {
                player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "Cannot teleport " + partyPlayer.getName() + " - they are in combat!");
                partyPlayer.sendMessage("    " + ChatComponents.GENERIC_SKILL + player.getName() + " attempted to teleport you, but were are in combat!");
                continue;
            }

            //Util.playClientEffect(partyPlayer, "enchantmenttable", new Vector(0, 0, 0), 1F, 10, true);
            player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, player.getEyeLocation(), 10, 0.5, 0.3, 0.5, 1F);
            //Util.playClientEffect(partyPlayer, "largeexplode", new Vector(0, 0, 0), 1F, 10, true);
            player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getEyeLocation(), 10, 0.5, 0.3, 0.5, 1F);
            partyPlayer.getWorld().playSound(partyPlayer.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5F, 1.0F);

            partyPlayer.teleport(player);
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5F, 1.0F);
        return SkillResult.NORMAL;
    }
}
