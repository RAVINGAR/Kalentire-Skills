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
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillFeignDeath extends TargettedSkill {
    private String applyText;
    private String expireText;

    public SkillFeignDeath(Heroes plugin) {
        super(plugin, "FeignDeath");
        setDescription("You feign your death, displaying a deceptive message of death to nearby players. While feigned, but instead go invisible for $1s.");
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

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }
        if (((Player) target).equals(player)) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        Player targetPlayer = (Player) target;
        if (!damageCheck(player, targetPlayer)) {
            return SkillResult.INVALID_TARGET;
        }

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);

        //String tn = tHero.getPlayer().getDisplayName();
        //String pn = player.getDisplayName();

        //hero.addEffect(new InvisibleEffect(this, duration, this.applyText, this.expireText));
        String playerName = player.getName();
        String targetName = targetPlayer.getName();

        String deathMessage = "[" + ChatColor.GREEN + "PVP" + ChatColor.DARK_GRAY + "]" + ChatColor.DARK_AQUA + playerName + ChatColor.DARK_GRAY + " was dominated by " + ChatColor.BLUE + targetName + ChatColor.DARK_GRAY + "!";

        broadcast(player.getLocation(), deathMessage, new Object[0]);

        // Feign Death
        hero.addEffect(new FeignDeathEffect(this, duration));

        return SkillResult.NORMAL;
    }

    // Buff effect used to keep track of warmup time
    public class FeignDeathEffect extends ExpirableEffect {

        public FeignDeathEffect(Skill skill, long duration) {
            super(skill, "FeignDeathEffect", duration);

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
