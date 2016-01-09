package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class SkillDynamite extends ActiveSkill {

    public SkillDynamite(Heroes plugin) {
        super(plugin, "Dynamite");
        setDescription("Spawns $1 primed TNT that does not harm entities but can break blocks.");
        setUsage("/skill dynamite");
        setArgumentRange(0, 0);
        setIdentifiers("skill dynamite");
        setTypes(SkillType.BLOCK_REMOVING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillTNTListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        int tntCount = SkillConfigManager.getUseSetting(hero, this, "tnt-count", 1, false);

        return getDescription().replace("$1", tntCount + "");

    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("tnt-count", 1);
        node.set("harmless-to-entities", true);
        node.set("throw", false);
        node.set("velocity-multiplier", 1.5);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        Location loc = player.getEyeLocation();
        World world = loc.getWorld();

        int tntCount = SkillConfigManager.getUseSetting(hero, this, "tnt-count", 1, false);
        boolean harmless = SkillConfigManager.getUseSetting(hero, this, "harmless-to-entities", true);
        boolean throwTNT = SkillConfigManager.getUseSetting(hero, this, "throw", false);
        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 1.5, false);

        for(int i = 0; i < tntCount; i++) {
            TNTPrimed tnt = world.spawn(loc, TNTPrimed.class);
            if(throwTNT) {
                tnt.setVelocity(loc.getDirection().multiply(mult));
            }
            if(harmless) {
                tnt.setMetadata("HarmlessToEntities", new FixedMetadataValue(plugin, true));
            }
        }

        return SkillResult.NORMAL;
    }

    public class SkillTNTListener implements Listener {

        @EventHandler/*(priority = EventPriority.HIGHEST)*/
        public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
            if(!event.isCancelled()) {
                if(event.getDamager() instanceof TNTPrimed) {
                    TNTPrimed tnt = (TNTPrimed) event.getDamager();

                    if(!tnt.hasMetadata("HarmlessToEntities")) {
                        return;
                    }

                    // For the sake of making things simple, we'll assume if it has the metadata, then it's harmless.
                    // This code would validate it's proper, and set to true, if you wanted to be certain.
                    // Since explosions cause more damage events than one blank potion throw landing, that'd be running a lot more.
                    /*List<MetadataValue> meta = tnt.getMetadata("HarmlessToEntities");
                    if (meta.size() != 1) {
                        Heroes.log(Level.WARNING, "Heroes Skill TNT Listener encountered an error with metadata - has something else been manipulating it?");
                        tnt.removeMetadata("HarmlessToEntities", plugin);
                        return;
                    }
                    else {
                        if (!meta.get(0).asBoolean()) {
                            tnt.removeMetadata("HarmlessToEntities", plugin);
                            return;
                        }

                    }*/

                    event.setCancelled(true);
                }
            }
        }

    }

}
