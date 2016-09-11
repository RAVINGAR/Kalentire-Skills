package com.herocraftonline.heroes.characters.skill.pack6;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
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


public class SkillParryMagic extends ActiveSkill {

    private String applyText;
    private String expireText;
    private String parrySkillText;

    public SkillParryMagic(Heroes plugin) {
        super(plugin, "ParryMagic");
        setDescription("You parry the next magical attack within $1 seconds.");
        setUsage("/skill parrymagic");
        setArgumentRange(0, 0);
        setIdentifiers("skill parrymagic", "skill pmagic");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% raised their guard!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% lowered their guard!");
        node.set("parry-text", "%hero% parried an attack!");
        //node.set("parry-skill-text", "%hero% has parried %target%'s %skill%.");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% raised their guard!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% lowered their guard!").replace("%hero%", "$1");
        //parrySkillText = SkillConfigManager.getRaw(this, "parry-skill-text", "%hero% has parried %target%'s %skill%.").replace("$1","%hero$").replace("$2","%target%").replace("$3","%skill");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        hero.addEffect(new ParryMagicEffect(this, hero.getPlayer(), duration));

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), CompatSound.ENTITY_PLAYER_LEVELUP.value() , 0.6F, 1.0F);
        return SkillResult.NORMAL;
    }

    public class ParryMagicEffect extends ExpirableEffect {

        public ParryMagicEffect(Skill skill, Player applier, long duration) {
            super(skill, "ParryMagic", applier, duration);
            this.types.add(EffectType.MAGIC);
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

        @SuppressWarnings("unused")
        private Skill skill;

        SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler()
        public void onSkillDamage(SkillDamageEvent event) {
            // Ignore cancelled damage events & 0 damage events for Spam Control
            if (event.getDamage() == 0 || event.isCancelled() || !event.getSkill().isType(SkillType.ABILITY_PROPERTY_MAGICAL) || !(event.getEntity() instanceof Player)) {
                return;
            }
            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect(getName())) {
                hero.getEffect(getName()).removeFromHero(hero);
                event.setCancelled(true);
                String message = Messaging.parameterizeMessage(parrySkillText, player.getName(), Messaging.getLivingEntityName(event.getDamager()), event.getSkill().getName());
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
        return getDescription().replace("$1", duration / 1000 + "");
    }
}