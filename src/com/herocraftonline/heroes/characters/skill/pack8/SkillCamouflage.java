package com.herocraftonline.heroes.characters.skill.pack8;

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
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.NMSHandler;

public class SkillCamouflage extends ActiveSkill {

    private String applyText;
    private String expireText;
    private String failText;
    private CamoMoveChecker moveChecker;
    private static Set<Material> allowedMaterials = EnumSet.noneOf(Material.class);

    static {
        //FIXME Did the best I could
        allowedMaterials.add(Material.DIRT);
        allowedMaterials.add(Material.GRASS);
        allowedMaterials.add(Material.GRAVEL);
        allowedMaterials.add(Material.ACACIA_LOG);
        allowedMaterials.add(Material.BIRCH_LOG);
        allowedMaterials.add(Material.DARK_OAK_LOG);
        allowedMaterials.add(Material.JUNGLE_LOG);
        allowedMaterials.add(Material.OAK_LOG);
        allowedMaterials.add(Material.SPRUCE_LOG);
        allowedMaterials.add(Material.ACACIA_LEAVES);
        allowedMaterials.add(Material.BIRCH_LEAVES);
        allowedMaterials.add(Material.DARK_OAK_LEAVES);
        allowedMaterials.add(Material.JUNGLE_LEAVES);
        allowedMaterials.add(Material.OAK_LEAVES);
        allowedMaterials.add(Material.SPRUCE_LEAVES);
        //FIXME Not sure about this stuff
        allowedMaterials.add(Material.LONG_GRASS);
        allowedMaterials.add(Material.CACTUS);
        allowedMaterials.add(Material.HUGE_MUSHROOM_1);
        allowedMaterials.add(Material.HUGE_MUSHROOM_2);
        allowedMaterials.add(Material.MOSSY_COBBLESTONE);
        allowedMaterials.add(Material.MYCELIUM);
        allowedMaterials.add(Material.MELON);
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
        setTypes(SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.BUFFING, SkillType.STEALTHY);

        moveChecker = new CamoMoveChecker(this);
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, moveChecker, 1, 1);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 30000);
        node.set(SkillSetting.APPLY_TEXT.node(), "You blend into the terrain");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "You come back into view");
        node.set("fail-text", "The surrounding terrain isn't natural enough");
        node.set("detection-range", 1D);
        node.set("max-move-distance", 1D);

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "You blend into the terrain");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "You come back into view");
        failText = SkillConfigManager.getRaw(this, "fail-text", "The surrounding terrain isn't natural enough");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location loc = player.getLocation();

        Location blockCheckLoc = loc.clone();
        blockCheckLoc.subtract(1, 0, 1);
        boolean inSnow = true;
        {
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 3; z++) {
                    if (!blockCheckLoc.getBlock().getType().equals(Material.SNOW)) {
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
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                if (!allowedMaterials.contains(blockCheckLoc.getBlock().getType())) {
                    inNature = false;
                }
                blockCheckLoc.add(0, 0, 1);
            }
            blockCheckLoc.add(1, 0, -3);
        }

        if (!inSnow && !inNature) {
            player.sendMessage(failText);
            return SkillResult.FAIL;
        }

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);
        hero.addEffect(new InvisibleEffect(this, player, duration, applyText, expireText));

        // If any nearby monsters are targeting the player, force them to change their target.
        for (Entity entity : player.getNearbyEntities(50, 50, 50)) {
            NMSHandler.getInterface().hidePlayerFromEntity(player, entity);
        }

        moveChecker.addHero(hero);
        return SkillResult.NORMAL;
    }

    public class CamoMoveChecker implements Runnable {

        private Map<Hero, Location> oldLocations = new HashMap<>();
        private Skill skill;

        CamoMoveChecker(Skill skill) {
            this.skill = skill;
        }

        @Override
        public void run() {
            Iterator<Entry<Hero, Location>> heroes = oldLocations.entrySet().iterator();
            while (heroes.hasNext()) {
                Entry<Hero, Location> entry = heroes.next();
                Hero hero = entry.getKey();
                Location oldLoc = entry.getValue();
                if (!hero.hasEffect("Invisible")) {
                    heroes.remove();
                    continue;
                }

                Location newLoc = hero.getPlayer().getLocation();
                if (newLoc.distance(oldLoc) > SkillConfigManager.getUseSetting(hero, skill, "max-move-distance", 1D, false)) {
                    hero.removeEffect(hero.getEffect("Invisible"));
                    heroes.remove();
                    continue;
                }
                double detectRange = SkillConfigManager.getUseSetting(hero, skill, "detection-range", 1D, false);
                List<Entity> nearEntities = hero.getPlayer().getNearbyEntities(detectRange, detectRange, detectRange);
                for (Entity entity : nearEntities) {
                    if (entity instanceof Player) {
                        if (hero.hasParty()) {
                            Hero nearHero = plugin.getCharacterManager().getHero((Player) entity);
                            HeroParty heroParty = hero.getParty();
                            boolean isPartyMember = false;
                            for (Hero partyMember : heroParty.getMembers()) {
                                if (nearHero.equals(partyMember)) {
                                    isPartyMember = true;
                                    break;
                                }
                            }

                            if (isPartyMember)
                                return;
                        }

                        hero.removeEffect(hero.getEffect("Invisible"));
                        heroes.remove();
                        break;
                    }
                }
            }
        }

        public void addHero(Hero hero) {
            if (!hero.hasEffect("Invisible"))
                return;

            oldLocations.put(hero, hero.getPlayer().getLocation());
        }
    }
}
