package com.herocraftonline.heroes.characters.skill.pack5;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

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
        if (hero.getParty() != null && hero.getParty().getMembers().size() != 1) {
            
            broadcastExecuteText(hero);
            
            for (Hero partyHero : hero.getParty().getMembers()) {
                Player partyPlayer = partyHero.getPlayer();
                if (partyHero.equals(hero) || !partyPlayer.getWorld().equals(player.getWorld())) {
                    continue;
                }
                if (partyHero.isInCombat()) {
                    Messaging.send(player, Messaging.getSkillDenoter() + "Cannot teleport " + partyPlayer.getName() + " - they are in combat!");
                    Messaging.send(partyPlayer, Messaging.getSkillDenoter() + player.getName() + " attempted to teleport you, but you are in combat!");
                    continue;
                }
                
                Util.playClientEffect(partyPlayer, "enchantmenttable", new Vector(0, 0, 0), 1F, 10, true);
                Util.playClientEffect(partyPlayer, "largeexplode", new Vector(0, 0, 0), 1F, 10, true);
                partyPlayer.getWorld().playSound(partyPlayer.getLocation(), CompatSound.ENTITY_WITHER_DEATH.value(), 0.5F, 1.0F);

                partyPlayer.teleport(player);
            }
            
            player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_WITHER_DEATH.value(), 0.5F, 1.0F);

            return SkillResult.NORMAL;
        }
        
        Messaging.send(player, "You must actually have party members to teleport them to you!");
        
        return SkillResult.FAIL;
    }
}
