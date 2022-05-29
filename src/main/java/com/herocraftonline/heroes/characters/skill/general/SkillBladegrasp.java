package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


public class SkillBladegrasp extends ActiveSkill implements Listenable {

    private String applyText;
    private String expireText;
    private String parryText;
    private String parrySkillText;
    private final Listener listener;

    public SkillBladegrasp(Heroes plugin) {
        super(plugin, "Bladegrasp");
        setDescription("After making a melee attack, if you are attacked by any physical attack within $1 seconds you parry the attack.");
        setUsage("/skill bladegrasp");
        setArgumentRange(0, 0);
        setIdentifiers("skill bladegrasp", "skill bgrasp");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.BUFFING);
        listener = new SkillEntityListener(this);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set("parry-duration", 1000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% tightened their grip!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% loosened their grip!");
        node.set("parry-text", "%hero% parried an attack!");
        node.set("parry-skill-text", "%hero% has parried %target%'s %skill%.");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% tightened his grip!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% loosened his grip!").replace("%hero%", "$1");
        parryText = SkillConfigManager.getRaw(this, "parry-text", "%hero% parried an attack!").replace("%hero%", "$1");
        parrySkillText = SkillConfigManager.getRaw(this, "parry-skill-text", "%hero% has parried %target%'s %skill%.").replace("%hero%","$1").replace("%target%", "$2");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        hero.addEffect(new BladegraspEffect(this, hero.getPlayer(), duration));

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.BLOCK_ANVIL_LAND , 0.6F, 1.0F);
        return SkillResult.NORMAL;
    }

    @Override
    public Listener getListener() {
        return listener;
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

    public class BladegraspCounter extends ExpirableEffect {

        public BladegraspCounter(Skill skill, Player applier, long duration) {
            super(skill, "BladegraspCounter", applier, duration);
            this.types.add(EffectType.PHYSICAL);
            this.types.add(EffectType.BENEFICIAL);
        }
    }

    public class SkillEntityListener implements Listener {


        private Skill skill;

        SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler()
        public void onWeaponDamage(WeaponDamageEvent event) {
            // Ignore cancelled damage events & 0/1 damage events for Spam Control
            if (event.getDamage() <= 1 || event.isCancelled() || !(event.getEntity() instanceof Player defenderPlayer)) {
                return;
            }

            CharacterTemplate attacker = event.getDamager();
            if(attacker.hasEffect(getName())) {
                int parryDuration = SkillConfigManager.getUseSetting((Hero)attacker, SkillBladegrasp.this, "parry-duration", 1000, false);
                attacker.addEffect(new BladegraspCounter(SkillBladegrasp.this, ((Hero)attacker).getPlayer(), parryDuration));
            }

            Hero defender = plugin.getCharacterManager().getHero(defenderPlayer);
            Effect counter = defender.getEffect("BladegraspCounter");
            if (counter != null) {
                counter.removeFromHero(defender);
                event.setCancelled(true);
                String message = parryText.replace("%hero%", defenderPlayer.getName());
                defenderPlayer.sendMessage(message);
                if (event.getDamager() instanceof Hero) {
                    ((Hero) event.getDamager()).getPlayer().sendMessage(message);
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
            Effect counter = hero.getEffect("BladegraspCounter");
            if (counter != null) {
                counter.removeFromHero(hero);
                event.setCancelled(true);
                String message = parrySkillText.replace("%hero%", player.getName()).replace("%target%", CustomNameManager.getName(event.getDamager())).replace("%skill%", event.getSkill().getName());
                player.sendMessage(message);
                if (event.getDamager() instanceof Hero) {
                    ((Hero) event.getDamager()).getPlayer().sendMessage(message);
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