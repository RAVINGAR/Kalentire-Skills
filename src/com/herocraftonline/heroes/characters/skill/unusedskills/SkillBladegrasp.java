package com.herocraftonline.heroes.characters.skill.unusedskills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;


public class SkillBladegrasp extends ActiveSkill {

    private String applyText;
    private String expireText;
    private String parryText;
    private String parrySkillText;

    public SkillBladegrasp(Heroes plugin) {
        super(plugin, "Bladegrasp");
        setDescription("You have a $1% chance to block incoming damage for $2 seconds.");
        setUsage("/skill bladegrasp");
        setArgumentRange(0, 0);
        setIdentifiers("skill bladegrasp", "skill bgrasp");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% tightened his grip!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% loosened his grip!");
        node.set("parry-text", "%hero% parried an attack!");
        //node.set("parry-skill-text", "%hero% has parried %target%'s %skill%.");
        node.set(SkillSetting.CHANCE_PER_LEVEL.node(), .02);
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% tightened his grip!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% loosened his grip!").replace("%hero%", "$1");
        parryText = SkillConfigManager.getRaw(this, "parry-text", "%hero% parried an attack!").replace("%hero%", "$1");
        //parrySkillText = SkillConfigManager.getRaw(this, "parry-skill-text", "%hero% has parried %target%'s %skill%.").replace("$1","%hero$").replace("$2","%target%").replace("$3","%skill");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        hero.addEffect(new BladegraspEffect(this, hero.getPlayer(), duration));

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), CompatSound.BLOCK_ANVIL_LAND.value() , 0.6F, 1.0F);
        return SkillResult.NORMAL;
    }

    public class BladegraspEffect extends ExpirableEffect {

        public BladegraspEffect(Skill skill, Player applier, long duration) {
            super(skill, "Bladegrasp", applier, duration);
            this.types.add(EffectType.PHYSICAL);
            this.types.add(EffectType.BENEFICIAL);
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

    public class SkillEntityListener implements Listener {

        private Skill skill;

        SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler()
        public void onWeaponDamage(WeaponDamageEvent event) {
            // Ignore cancelled damage events & 0 damage events for Spam Control
            if (event.getDamage() == 0 || event.isCancelled() || !(event.getEntity() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect(getName())) {
                double parryChance = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.CHANCE_PER_LEVEL, .02, false) * hero.getHeroLevel(skill);
                if (Util.nextRand() > parryChance) {
                    return;
                }

                event.setCancelled(true);
                String message = Messaging.parameterizeMessage(parryText, player.getName());
                Messaging.send(player, message);
                if (event.getDamager() instanceof Hero) {
                    Messaging.send(((Hero) event.getDamager()).getPlayer(), message);
                }
            }
        }

        @EventHandler()
        public void onSkillDamage(SkillDamageEvent event) {
            // Ignore cancelled damage events & 0 damage events for Spam Control
            if (event.getDamage() == 0 || event.isCancelled() || !event.getSkill().isType(SkillType.ABILITY_PROPERTY_PHYSICAL) || !(event.getEntity() instanceof Player)) {
                return;
            }
            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect(getName())) {
                double parryChance = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.CHANCE_PER_LEVEL, .02, false) * hero.getHeroLevel(event.getSkill());
                if (Util.nextRand() > parryChance) {
                    return;
                }

                event.setCancelled(true);
                String message = Messaging.parameterizeMessage(parrySkillText, player.getName(), CustomNameManager.getName(event.getDamager()), event.getSkill().getName());
                Messaging.send(player, message);
                if (event.getDamager() instanceof Hero) {
                    Messaging.send(((Hero) event.getDamager()).getPlayer(), message);
                }

            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        double chance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE_PER_LEVEL, .02, false);
        int level = hero.getHeroLevel(this);
        if (level < 1)
            level = 1;
        return getDescription().replace("$1", Util.stringDouble(chance * level * 100)).replace("$2", duration / 1000 + "");
    }
}