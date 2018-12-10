package com.herocraftonline.heroes.characters.skill.public1;


import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

public class SkillGroupHeal extends ActiveSkill {

    public SkillGroupHeal(Heroes plugin) {
        super(plugin, "GroupHeal");
        this.setDescription("You restore $1 health to all nearby party members.");
        this.setUsage("/skill groupheal");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill groupheal", "skill gheal");
        this.setTypes(SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("heal-amount", 2);
        node.set(SkillSetting.RADIUS.node(), 5);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();
        final double healAmount = SkillConfigManager.getUseSetting(hero, this, "heal-amount", 2, false);
        if (hero.getParty() == null) {
            // Heal just the caster if he's not in a party
            final HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(hero, healAmount, this, hero);
            this.plugin.getServer().getPluginManager().callEvent(hrhEvent);
            if (hrhEvent.isCancelled()) {
                player.sendMessage(ChatColor.GRAY + "Unable to heal the target at this time!");
                return SkillResult.CANCELLED;
            }
            hero.heal(hrhEvent.getDelta());
        } else {
            final int radiusSquared = (int) Math.pow(SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false), 2);
            final Location heroLoc = player.getLocation();
            // Heal party members near the caster
            for (final Hero partyHero : hero.getParty().getMembers()) {
                if (!player.getWorld().equals(partyHero.getPlayer().getWorld())) {
                    continue;
                }
                if (partyHero.getPlayer().getLocation().distanceSquared(heroLoc) <= radiusSquared) {
                    final HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(partyHero, healAmount, this, hero);
                    this.plugin.getServer().getPluginManager().callEvent(hrhEvent);
                    if (hrhEvent.isCancelled()) {
                        player.sendMessage(ChatColor.GRAY + "Unable to heal the target at this time!");
                        return SkillResult.CANCELLED;
                    }
                    partyHero.heal(hrhEvent.getDelta());
                }
            }
        }

        this.broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        final double heal = SkillConfigManager.getUseSetting(hero, this, "heal-amount", 2, false);
        return this.getDescription().replace("$1", heal + "");
    }
}
