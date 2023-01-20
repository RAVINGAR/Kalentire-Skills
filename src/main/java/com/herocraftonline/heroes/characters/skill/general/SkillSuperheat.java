package com.herocraftonline.heroes.characters.skill.general;

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

public class SkillSuperheat extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillSuperheat(final Heroes plugin) {
        super(plugin, "Superheat");
        this.setDescription("Your pickaxe smelts ores as you mine them for $1 second(s).");
        this.setUsage("/skill superheat");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill superheat");
        this.setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.ABILITY_PROPERTY_EARTH, SkillType.BUFFING, SkillType.SILENCEABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillPlayerListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection section = super.getDefaultConfig();
        section.set(SkillSetting.DURATION.node(), 20000);
        section.set(SkillSetting.APPLY_TEXT.node(), "%hero%'s pick has become superheated!");
        section.set(SkillSetting.EXPIRE_TEXT.node(), "%hero%'s pick has cooled down!");
        return section;
    }

    @Override
    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero%'s pick has become superheated!").replace("%hero%", "$1").replace("$hero$", "$1");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero%'s pick has cooled down!").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        this.broadcastExecuteText(hero);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        hero.addEffect(new SuperheatEffect(this, hero.getPlayer(), duration));

        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        return this.getDescription().replace("$1", (duration / 1000) + "");
    }

    public class SkillPlayerListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onBlockBreak(final BlockBreakEvent event) {
            if (event.isCancelled()) {
                return;
            }

            final Hero hero = SkillSuperheat.this.plugin.getCharacterManager().getHero(event.getPlayer());
            if (hero.hasEffect("Superheat")) {
                final Block block = event.getBlock();
                switch (block.getType()) {
                    case DEEPSLATE_IRON_ORE:
                    case IRON_ORE:
                        event.setCancelled(true);
                        block.setType(Material.AIR);
                        block.getWorld().dropItem(block.getLocation(), new ItemStack(Material.IRON_INGOT, 1));
                        break;
                    case COPPER_ORE:
                    case DEEPSLATE_COPPER_ORE:
                        event.setCancelled(true);
                        block.setType(Material.AIR);
                        block.getWorld().dropItem(block.getLocation(), new ItemStack(Material.COPPER_INGOT, 1));
                        break;
                    case GOLD_ORE:
                    case DEEPSLATE_GOLD_ORE:
                        event.setCancelled(true);
                        block.setType(Material.AIR);
                        block.getWorld().dropItem(block.getLocation(), new ItemStack(Material.GOLD_INGOT, 1));
                        break;
                    case SAND:
                        event.setCancelled(true);
                        block.setType(Material.AIR);
                        block.getWorld().dropItem(block.getLocation(), new ItemStack(Material.GLASS, 1));
                        break;
                    case STONE:
                    case COBBLESTONE:
                        event.setCancelled(true);
                        block.setType(Material.AIR);
                        block.getWorld().dropItem(block.getLocation(), new ItemStack(Material.STONE, 1));
                        break;
                    case DEEPSLATE:
                    case COBBLED_DEEPSLATE:
                        event.setCancelled(true);
                        block.setType(Material.AIR);
                        block.getWorld().dropItem(block.getLocation(), new ItemStack(Material.DEEPSLATE, 1));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public class SuperheatEffect extends ExpirableEffect {

        public SuperheatEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, "Superheat", applier, duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.FIRE);
            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), SkillSuperheat.this.applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), SkillSuperheat.this.expireText, player.getDisplayName());
        }

    }

}
