package com.herocraftonline.heroes.characters.skill.reborn.ninja;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.ClassChangeEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.scoreboard.TeamScoreboard;
import com.herocraftonline.heroes.nms.scoreboard.TeamScoreboardPacket;
import com.herocraftonline.heroes.nms.scoreboard.TeamScoreboardPacket.PacketAction;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SkillHide extends PassiveSkill implements Listener{

    private final Set<UUID> ghosts;

    private final TeamScoreboard team;
    private final TeamScoreboardPacket teamCreatePacket;


    public SkillHide(Heroes plugin){
        super(plugin, "Hide");
        setDescription("You are always hiding except when you take damage. It will take $1 seconds to re-sneak");
        setTypes(SkillType.STEALTHY);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        ghosts = new HashSet<>();

        team = NMSHandler.getInterface().generateTeamScoreboard("GhostTeam");
        team.setCanSeeFriendlyInvisibles(true);
        team.setAllowFriendlyFire(false);

        teamCreatePacket = team.generateTeamScoreboardPacket(PacketAction.CREATE_TEAM);

    }

    @Override
    public String getDescription(Hero hero) {
        final long stall = SkillConfigManager.getUseSetting(hero, this, "stall", 3000, false);

        return getDescription().replace("$1", stall + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig(){
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("stall", Integer.valueOf(0));

        return node;
    }

    /*
     * Listener checks for player join and when they take damage (add the effect when they join
     * remove it when they take damage)
     */

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClassChange(ClassChangeEvent event){
        Hero hero = event.getHero();
        Player player = hero.getPlayer();

        if(hero.hasAccessToSkill(this)){
            teamCreatePacket.playPacket(player);
            team.generateTeamScoreboardPacket(PacketAction.ADD_PLAYER, player).playPacket(player);
            for (UUID ghost : ghosts) {
                team.generateTeamScoreboardPacket(PacketAction.ADD_PLAYER, plugin.getServer().getPlayer(ghost)).playPacket(player);
            }

            if(hero.hasAccessToSkill(this)){
                if(hero.hasEffect("Hide")){
                    hero.removeEffect(hero.getEffect("Hide"));
                }
                player.setSneaking(true);
                hero.addEffect(new HideEffect(this));
                Messaging.send(player, "You are now hiding", new Object[0]);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event){
        final Player player = event.getPlayer();
        final Hero hero = plugin.getCharacterManager().getHero(player);

        teamCreatePacket.playPacket(player);
        team.generateTeamScoreboardPacket(PacketAction.ADD_PLAYER, player).playPacket(player);
        for (UUID ghost : ghosts) {
            team.generateTeamScoreboardPacket(PacketAction.ADD_PLAYER, plugin.getServer().getPlayer(ghost)).playPacket(player);
        }

        if(hero.hasAccessToSkill(this)){
            if(hero.hasEffect("Hide")){
                hero.removeEffect(hero.getEffect("Hide"));
            }
            player.setSneaking(true);
            hero.addEffect(new HideEffect(this));
            Messaging.send(player, "You are now hiding", new Object[0]);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent event){
        if(event.isCancelled() || (event.getDamage() == 0)){
            return;
        }

        Player player = null;
        if(event.getEntity() instanceof Player){
            player = (Player) event.getEntity();
            final Hero hero = plugin.getCharacterManager().getHero(player);
            final long stall = SkillConfigManager.getUseSetting(hero, this, "stall", 3000, true);

            if(hero.hasEffect("Hide")){
                player.setSneaking(false);
                hero.removeEffect(hero.getEffect("Hide"));

                hero.addEffect(new HideStallEffect(this, player, stall));
                Messaging.send(player, "You are no longer hiding", new Object[0]);
            }
        }
    }

    /*
     * Pretty much the 'sneak' effect but with no duration
     */
    public class HideEffect extends Effect{
        private boolean vanillaSneaking;
        private Skill skill;

        public HideEffect(Skill skill){
            super(skill, "Hide");
            this.skill = skill;

            addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false), false);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.INVISIBILITY);

        }

        public HideEffect(Skill skill, boolean vanillaSneaking){
            super(skill, "Sneak");
            this.types.add(EffectType.SNEAK);
            setVanillaSneaking(vanillaSneaking);
            this.skill = skill;

            addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false), false);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.INVISIBILITY);
        }

        public void applyToHero(Hero hero){
            super.applyToHero(hero);
            Player player  = hero.getPlayer();
            player.setSneaking(true);

            TeamScoreboardPacket packet = team.generateTeamScoreboardPacket(PacketAction.ADD_PLAYER, player);
            for (Player otherPlayer : plugin.getServer().getOnlinePlayers()) {
                if (otherPlayer.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                packet.playPacket(otherPlayer);
            }
            ghosts.add(player.getUniqueId());
        }

        public void removeFromHero(Hero hero){
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            player.setSneaking(false);

            TeamScoreboardPacket packet = team.generateTeamScoreboardPacket(PacketAction.REMOVE_PLAYER, player);
            for (Player otherPlayer : plugin.getServer().getOnlinePlayers()) {
                if (otherPlayer.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                packet.playPacket(otherPlayer);
            }
            ghosts.remove(player.getUniqueId());
        }

        public boolean isVanillaSneaking(){
            return this.vanillaSneaking;
        }

        public void setVanillaSneaking(boolean vanillaSneaking){
            this.vanillaSneaking = vanillaSneaking;
        }

    }

    /*
     * Effect to act as the 'cooldown' after taking damage. After X seconds they will re-hide
     */
    public class HideStallEffect extends ExpirableEffect {
        private Skill skill;

        public HideStallEffect(Skill skill, Player applier, long duration) {
            super(skill, "HideStall", applier, duration);
            this.skill = skill;
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            hero.addEffect(new HideEffect(this.skill));
            Messaging.send(player, "You are now hiding", new Object[0]);
        }
    }

}
