package com.herocraftonline.heroes.characters.skill.skills;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillCamouflage extends ActiveSkill {

    private String applyText;
    private String expireText;
    private String failText;
    private CamoMoveChecker moveChecker;
    private static Set<Material> allowedMaterials = EnumSet.noneOf(Material.class);

    static {
        allowedMaterials.add(Material.DIRT);
        allowedMaterials.add(Material.GRASS);
        allowedMaterials.add(Material.GRAVEL);
        allowedMaterials.add(Material.LOG);
        allowedMaterials.add(Material.LEAVES);
        allowedMaterials.add(Material.MYCEL);
        allowedMaterials.add(Material.MELON_BLOCK);
        allowedMaterials.add(Material.PUMPKIN);
        allowedMaterials.add(Material.SAND);
        allowedMaterials.add(Material.SANDSTONE);
        allowedMaterials.add(Material.SNOW);
        allowedMaterials.add(Material.SNOW_BLOCK);
    }

    public SkillCamouflage(Heroes plugin) {
        super(plugin, "Camouflage");
        setDescription("You attempt to hide in the surrounding terrain");
        setUsage("/skill camouflage");
        setArgumentRange(0, 0);
        setIdentifiers("skill camouflage", "skill camo");
        setNotes("Note: Taking damage, moving, or causing damage removes the effect");
        setTypes(SkillType.ILLUSION, SkillType.BUFF, SkillType.COUNTER, SkillType.STEALTHY);
        moveChecker = new CamoMoveChecker(this);
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, moveChecker, 1, 1);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 30000);
        node.set(Setting.APPLY_TEXT.node(), "You blend into the terrain");
        node.set(Setting.EXPIRE_TEXT.node(), "You come back into view");
        node.set("fail-text", "The surrounding terrain isn't natural enough");
        node.set("detection-range", 1D);
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "You blend into the terrain");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "You come back into view");
        failText = SkillConfigManager.getRaw(this, "fail-text", "The surrounding terrain isn't natural enough");
    }



    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location loc = player.getLocation();

        Location blockCheckLoc = loc.clone();
        blockCheckLoc.subtract(1, 0, 1);
        boolean inSnow = true; {
            for(int x = 0; x < 3; x++) {
                for(int z = 0; z < 3; z++) {
                    if(!blockCheckLoc.getBlock().getType().equals(Material.SNOW)) {
                        inSnow = false;
                    }
                    blockCheckLoc.add(0, 0, 1);
                }
                blockCheckLoc.add(1, 0, -3);
            }
        }

        blockCheckLoc = loc.clone();
        blockCheckLoc.subtract(1, 1, 1);
        boolean inNature = true;
        for(int x = 0; x < 3; x++) {
            for(int z = 0; z < 3; z++) {
                if(!allowedMaterials.contains(blockCheckLoc.getBlock().getType())) {
                    inNature = false;
                }
                blockCheckLoc.add(0, 0, 1);
            }
            blockCheckLoc.add(1, 0, -3);
        }

        if(!inSnow && !inNature) {
            Messaging.send(player, failText);
            return SkillResult.FAIL;
        }

        long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 30000, false);
        player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.SMOKE, 4);
        hero.addEffect(new InvisibleEffect(this, duration, applyText, expireText));
        moveChecker.addHero(hero);
        return SkillResult.NORMAL;
    }

    public class CamoMoveChecker implements Runnable{

        private Map<Hero, Location> oldLocations = new HashMap<Hero, Location>();
        private Skill skill;

        CamoMoveChecker(Skill skill) {
            this.skill = skill;
        }

        @Override
        public void run() {
            Iterator<Entry<Hero, Location>> heroes = oldLocations.entrySet().iterator();
            while(heroes.hasNext()) {
                Entry<Hero, Location> entry = heroes.next();
                Hero hero = entry.getKey();
                Location oldLoc = entry.getValue();
                if(!hero.hasEffect("Invisible")) {
                    heroes.remove();
                    continue;
                }
                
                Location newLoc = hero.getPlayer().getLocation();
                if(newLoc.distance(oldLoc) > 1) {
                    hero.removeEffect(hero.getEffect("Invisible"));
                    heroes.remove();
                    continue;
                }
                double detectRange = SkillConfigManager.getUseSetting(hero, skill, "detection-range", 1D, false);
                List<Entity> nearEntities = hero.getPlayer().getNearbyEntities(detectRange, detectRange, detectRange);
                for(Entity entity : nearEntities) {
                    if(entity instanceof Player) {
                        hero.removeEffect(hero.getEffect("Invisible"));
                        heroes.remove();
                        break;
                    }
                }
            }
        }

        public void addHero(Hero hero) {
            if(!hero.hasEffect("Invisible"))
                return;
            oldLocations.put(hero, hero.getPlayer().getLocation());
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

}
