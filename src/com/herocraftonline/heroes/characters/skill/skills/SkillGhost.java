package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.scoreboard.TeamScoreboard;
import com.herocraftonline.heroes.nms.scoreboard.TeamScoreboardPacket;
import com.herocraftonline.heroes.nms.scoreboard.TeamScoreboardPacket.PacketAction;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SkillGhost extends ActiveSkill implements Listener {

    private final Set<UUID> ghosts;

    private final TeamScoreboard team;
    private final TeamScoreboardPacket teamCreatePacket;

    public SkillGhost(Heroes plugin) {
        super(plugin, "ghost");
        setDescription("Become a ghost, transparent to everyone else. If you're already a ghost, you return to normal.");
        setUsage("/skill ghost");
        setArgumentRange(0, 0);
        setIdentifiers("skill ghost");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        ghosts = new HashSet<>();

        team = NMSHandler.getInterface().generateTeamScoreboard("GhostTeam");
        team.setCanSeeFriendlyInvisibles(true);
        team.setAllowFriendlyFire(false);

        teamCreatePacket = team.generateTeamScoreboardPacket(PacketAction.CREATE_TEAM);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        if (hero.hasEffect("Ghost")) {
            hero.removeEffect(hero.getEffect("Ghost"));
        }
        else {
            hero.addEffect(new GhostEffect(this));
        }
        return SkillResult.NORMAL;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        teamCreatePacket.playPacket(player);
        team.generateTeamScoreboardPacket(PacketAction.ADD_PLAYER, player).playPacket(player);
        for (UUID ghost : ghosts) {
            team.generateTeamScoreboardPacket(PacketAction.ADD_PLAYER, plugin.getServer().getPlayer(ghost)).playPacket(player);
        }
    }

    public class GhostEffect extends Effect {

        public GhostEffect(Skill skill) {
            super(skill, "Ghost");

            addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false), false);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.INVISIBILITY);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();

            TeamScoreboardPacket packet = team.generateTeamScoreboardPacket(PacketAction.ADD_PLAYER, player);
            for (Player otherPlayer : plugin.getServer().getOnlinePlayers()) {
                if (otherPlayer.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                packet.playPacket(otherPlayer);
            }

            ghosts.add(player.getUniqueId());

            player.sendMessage(ChatColor.GRAY + "You are now a spoopy ghost!");
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();

            TeamScoreboardPacket packet = team.generateTeamScoreboardPacket(PacketAction.REMOVE_PLAYER, player);
            for (Player otherPlayer : plugin.getServer().getOnlinePlayers()) {
                if (otherPlayer.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                packet.playPacket(otherPlayer);
            }

            ghosts.remove(player.getUniqueId());

            player.sendMessage(ChatColor.GRAY + "You're back to normal.");
        }
    }
}
