package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

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
import com.herocraftonline.heroes.util.Util;

public class SkillUndyingWill extends ActiveSkill {
    private String expireText;

    public SkillUndyingWill(Heroes plugin) {
        super(plugin, "UndyingWill");
        setDescription("You are overcome with an undying will to survive. You cannot be killed for the next $1 seconds.");
        setUsage("/skill undyingwill");
        setArgumentRange(0, 0);
        setIdentifiers("skill undyingwill");
        setTypes(SkillType.BUFF, SkillType.COUNTER, SkillType.PHYSICAL);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    public String getDescription(Hero hero) {
        double duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(4500), false);

        // Cast to float so we can get proper decimals.
        String actualDuration = Util.formatDouble(duration / 1000.0) + "";

        return getDescription().replace("$1", actualDuration + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), Integer.valueOf(4500));
        node.set(SkillSetting.USE_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% is overcome with an undying will!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero%'s will returns to normal.");

        return node;
    }

    public void init() {
        super.init();

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero%'s will returns to normal.").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(4500), false);
        hero.addEffect(new UndyingWillEffect(this, duration));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WOLF_GROWL, 0.5F, 0.1F);

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {
        public SkillHeroListener() {
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {

            // If our target isn't a Living Entity exit
            if (!(event.getEntity() instanceof LivingEntity) || event.getDamage() == 0) {
                return;
            }

            Entity entity = event.getEntity();
            //LivingEntity livingEntity = (LivingEntity) entity;

            if (entity instanceof Player) {
                Player player = (Player) entity;
                Hero hero = plugin.getCharacterManager().getHero(player);

                // Don't let them go below 1HP.
                if (hero.hasEffect("UndyingWillEffect")) {
                    double currentHealth = player.getHealth();

                    if (event.getDamage() > currentHealth) {
                        if (currentHealth != 1.0)
                            player.setHealth(1.0);

                        event.setDamage(0.0);
                    }
                }
            }
        }
    }

    public class UndyingWillEffect extends ExpirableEffect {
        public UndyingWillEffect(Skill skill, long duration) {
            super(skill, "UndyingWillEffect", duration);

            types.add(EffectType.PHYSICAL);
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }
}