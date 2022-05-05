package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.effects.common.ImbueEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class SkillRuptureShot extends ActiveSkill implements Listenable {

    private String applyText;
    private String expireText;
    private final Listener listener;

    public SkillRuptureShot(Heroes plugin) {
        super(plugin, "RuptureShot");
        setDescription("Your arrows rupture targets, dealing $1 damage over $2 seconds, and draining $3 mana and $4 stamina.");
        setUsage("/skill ruptureshot");
        setArgumentRange(0, 0);
        setIdentifiers("skill ruptureshot", "skill ruptureshot");
        setTypes(SkillType.BUFFING);
        listener = new SkillDamageListener(this);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000); // milliseconds
        node.set(SkillSetting.PERIOD.node(), 2000); // 2 seconds in milliseconds
        node.set("mana-per-shot", 1); // How much mana for each attack
        node.set("mana-drain", 50); // How much mana to drain on hit
        node.set("stamina-drain", 100); // How much stamina to drain on hit
        node.set("tick-damage", 2);
        node.set(SkillSetting.USE_TEXT.node(), "%hero% imbues their arrows with rupture!");
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% is ruptured!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has recovered from the rupture!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        setUseText("%hero% imbues their arrows with rupture!".replace("%hero%", "$1"));
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% is ruptured!")
                .replace("%target%", "$1").replace("%hero%","$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has recovered from the rupture!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        if (hero.hasEffect("RuptureShotBuff")) {
            hero.removeEffect(hero.getEffect("RuptureShotBuff"));
            return SkillResult.SKIP_POST_USAGE;
        }
        hero.addEffect(new RuptureShotBuff(this));
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    public class ArrowRupture extends PeriodicDamageEffect {
        private int staminaDrain;
        private int manaDrain;

        public ArrowRupture(Skill skill, long period, long duration, double tickDamage, Player applier, int sDrain, int mDrain) {
            super(skill, "ArrowRupture", applier, period, duration, tickDamage);
            this.types.add(EffectType.BLEED);
            staminaDrain = sDrain;
            manaDrain = mDrain;
            //addPotionEffect(new PotionEffect(PotionEffectType.WITHER, (int) (duration / 1000) * 20, 0)); fixme well theres no such thing as 'fake' effects anymore
            //addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (duration / 1000) * 20, 5), true);
        }
        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
            int targetStam = hero.getStamina();
            if (targetStam > staminaDrain) {
                hero.setStamina(targetStam - staminaDrain);
            } else {
                hero.setStamina(0);
            }

            int targetMana = hero.getMana();
            if (targetMana > manaDrain) {
                hero.setMana(targetMana - manaDrain);
            } else {
                hero.setStamina(0);
            }
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + applyText, CustomNameManager.getName(monster), applier.getName());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName(), applier.getName());
            Messaging.sendSkillMessage(player, "You feel your essence being ruptured..");
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, CustomNameManager.getName(monster));
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
            Messaging.sendSkillMessage(player, "You no longer feel that you are being ruptured");
        }
    }

    public class RuptureShotBuff extends ImbueEffect {

        public RuptureShotBuff(Skill skill) {
            super(skill, "RuptureShotBuff");

            types.add(EffectType.BENEFICIAL);
            setDescription("Ruptured Shots");
        }
    }

    public class SkillDamageListener implements Listener {

        private final Skill skill;

        public SkillDamageListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof LivingEntity) || !(event instanceof EntityDamageByEntityEvent)) {
                return;
            }

            LivingEntity target = (LivingEntity) event.getEntity();
            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;

            if (!(subEvent.getDamager() instanceof Arrow)) {
                return;
            }

            Arrow arrow = (Arrow) subEvent.getDamager();
            if (!(arrow.getShooter() instanceof Player)) {
                return;
            }

            Player player = (Player) arrow.getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);

            if (hero.hasEffect("RuptureShotBuff")) {
                long duration = SkillConfigManager.getUseSetting(hero, skill, "rupture-duration", 10000, false);
                long period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 2000, true);
                int tickDamage = SkillConfigManager.getUseSetting(hero, skill, "tick-damage", 2, false);
                int staminaDrain = SkillConfigManager.getUseSetting(hero, skill, "stamina-drain", 100, false);
                int manaDrain = SkillConfigManager.getUseSetting(hero, skill, "mana-drain", 50, false);
                plugin.getCharacterManager().getCharacter(target).addEffect(new ArrowRupture(skill, period, duration, tickDamage, player, staminaDrain, manaDrain));
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityShootBow(EntityShootBowEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)) {
                return;
            }
            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("RuptureShotBuff")) {
                int mana = SkillConfigManager.getUseSetting(hero, skill, "mana-per-shot", 1, true);
                if (hero.getMana() < mana) {
                    hero.removeEffect(hero.getEffect("RuptureShotBuff"));
                } else {
                    hero.setMana(hero.getMana() - mana);
                }
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        int mana = SkillConfigManager.getUseSetting(hero, this, "mana-drain", 1, true);
        int stamina = SkillConfigManager.getUseSetting(hero, this, "stamina-drain", 1, true);
        damage = damage * duration / period;
        return getDescription().replace("$1", damage + "").replace("$2", duration / 1000 + "").replace("$3", mana + "").replace("$4", stamina + "");
    }
}
