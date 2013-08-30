package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillFeignDeath extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillFeignDeath(Heroes plugin) {
        super(plugin, "FeignDeath");
        setDescription("You feign your death, displaying a deceptive message of death to nearby players. After feigning, you are invisible for $1 seconds. Moving will break the effect however.");
        setUsage("/skill feigndeath");
        setArgumentRange(0, 0);
        setIdentifiers("skill feigndeath");
        setTypes(SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.SILENCABLE, SkillType.STEALTHY, SkillType.BUFFING);
    }

    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, "smoke-duration", 6000, false) / 1000;

        return getDescription().replace("$2", duration + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 6000);

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "You feign death!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "You appear to be living!");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);

        String playerName = player.getName();
        LivingEntity lastCombatTarget = hero.getCombatEffect().getLastCombatant();
        if (lastCombatTarget instanceof Player) {
            String targetName = ((Player) lastCombatTarget).getName();
            String deathMessage = "[" + ChatColor.GREEN + "PVP" + ChatColor.DARK_GRAY + "]" + ChatColor.DARK_AQUA + playerName + ChatColor.DARK_GRAY + " was dominated by " + ChatColor.BLUE + targetName + ChatColor.DARK_GRAY + "!";
            broadcast(player.getLocation(), deathMessage);
        }
        else {
            String targetName = Messaging.getLivingEntityName(lastCombatTarget);
            String deathMessage = "[" + ChatColor.GREEN + "PVE" + ChatColor.DARK_GRAY + "]" + ChatColor.DARK_AQUA + playerName + ChatColor.DARK_GRAY + " was dominated by " + ChatColor.BLUE + targetName + ChatColor.DARK_GRAY + "!";
            broadcast(player.getLocation(), deathMessage);
        }

        // Feign Death
        hero.addEffect(new InvisibleEffect(this, "FeignDeathed", player, duration, "", ""));

        return SkillResult.NORMAL;
    }

    // Buff effect used to keep track of warmup time
    public class FeignDeathEffect extends ExpirableEffect {

        public FeignDeathEffect(Skill skill, Player applier, long duration) {
            super(skill, "FeignDeathEffect", applier, duration);

            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.INVIS);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();

            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (onlinePlayer.equals(player) || onlinePlayer.hasPermission("heroes.admin.seeinvis")) {
                    continue;
                }
                onlinePlayer.hidePlayer(player);
            }

            if (applyText != null && applyText.length() > 0)
                Messaging.send(player, applyText, new Object[0]);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (onlinePlayer.equals(player)) {
                    continue;
                }
                onlinePlayer.showPlayer(player);
            }

            if (expireText != null && expireText.length() > 0)
                Messaging.send(player, expireText, new Object[0]);
        }
    }
}
