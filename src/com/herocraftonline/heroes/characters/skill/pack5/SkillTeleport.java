package com.herocraftonline.heroes.characters.skill.pack5;

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
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillTeleport extends ActiveSkill {

    public SkillTeleport(Heroes plugin) {
        super(plugin, "Teleport");
        setDescription("Teleports you somewhere close by to a party member.");
        setUsage("/skill teleport <player>");
        setArgumentRange(1, 1);
        setIdentifiers("skill teleport");
        setTypes(SkillType.TELEPORTING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.COOLDOWN.node(), 180000);
        node.set(SkillSetting.REAGENT.node(), 264);
        node.set(SkillSetting.REAGENT_COST.node(), 1);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        if (!(hero.getParty() != null && hero.getParty().getMembers().size() > 0)) {
            Messaging.send(player, "Sorry, you need to be in a party with players!");
            return SkillResult.FAIL;
        }

        Player targetPlayer = plugin.getServer().getPlayer(args[0]);
        if (targetPlayer == null)
            return SkillResult.INVALID_TARGET;
        

        if (!hero.getParty().isPartyMember(plugin.getCharacterManager().getHero(targetPlayer))) {
            Messaging.send(player, "Sorry, that player isn't in your party!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero);

        int level = hero.getLevel(this);
        Location loc1 = targetPlayer.getLocation().add(Util.nextRand() * (-50 + level - (50 - level)), 0, Util.nextRand() * (-50 + level - (50 - level)));
        Double highestBlock = (double) targetPlayer.getWorld().getHighestBlockYAt(loc1);
        loc1.setY(highestBlock);

        player.teleport(loc1);
        player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 3);
        player.getWorld().playSound(player.getLocation(), CompatSound.BLOCK_PORTAL_TRAVEL.value(), 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }
}
