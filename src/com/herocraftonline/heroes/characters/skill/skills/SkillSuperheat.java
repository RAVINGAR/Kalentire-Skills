package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillSuperheat extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillSuperheat(Heroes plugin) {
        super(plugin, "Superheat");
        setDescription("Your pickaxe smelts ores as you mine them for $1 seconds.");
        setUsage("/skill superheat");
        setArgumentRange(0, 0);
        setIdentifiers("skill superheat");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.ABILITY_PROPERTY_EARTH, SkillType.BUFFING, SkillType.SILENCABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillPlayerListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection section = super.getDefaultConfig();

        section.set(SkillSetting.DURATION.node(), 20000);
        section.set(SkillSetting.APPLY_TEXT.node(), "%hero%'s pick has become superheated!");
        section.set(SkillSetting.EXPIRE_TEXT.node(), "%hero%'s pick has cooled down!");

        return section;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero%'s pick has become superheated!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero%'s pick has cooled down!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        hero.addEffect(new SuperheatEffect(this, hero.getPlayer(), duration));

        return SkillResult.NORMAL;
    }

    public class SkillPlayerListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onBlockBreak(BlockBreakEvent event) {            
            if (event.isCancelled()) {
                return;
            }

            Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (hero.hasEffect("Superheat")) {
                Block block = event.getBlock();
                switch (block.getType()) {
                case IRON_ORE:
                    event.setCancelled(true);
                    block.setType(Material.AIR);
                    block.getWorld().dropItem(block.getLocation(), new ItemStack(Material.IRON_INGOT, 1));
                    break;
                case GOLD_ORE:
                    event.setCancelled(true);
                    block.setType(Material.AIR);
                    block.getWorld().dropItem(block.getLocation(), new ItemStack(Material.GOLD_INGOT, 1));
                    break;
                case SAND:
                    event.setCancelled(true);
                    block.setType(Material.AIR);
                    block.getWorld().dropItem(block.getLocation(), new ItemStack(Material.GLASS, 1));
                    break;
                case COBBLESTONE:
                    event.setCancelled(true);
                    block.setType(Material.AIR);
                    block.getWorld().dropItem(block.getLocation(), new ItemStack(Material.STONE, 1));
                    break;
                default:

                }
            }
        }
    }

    public class SuperheatEffect extends ExpirableEffect {

        public SuperheatEffect(Skill skill, Player applier, long duration) {
            super(skill, "Superheat", applier, duration);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.FIRE);
            types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

    }
}
