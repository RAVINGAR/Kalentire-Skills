package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.effects.common.ImbueEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillRuptureShot extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillRuptureShot(final Heroes plugin) {
        super(plugin, "RuptureShot");
        setDescription("Your arrows will rupture their target dealing $1 damage over $2 seconds, each arrow will drain $3 mana.");
        setUsage("/skill ruptureshot");
        setArgumentRange(0, 0);
        setIdentifiers("skill ruptureshot", "skill ruptureshot");
        setTypes(SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
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
        setUseText("%hero% imbues their arrows with rupture!".replace("%hero%", "$1").replace("$hero$", "$1"));
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% is ruptured!")
                .replace("%target%", "$1").replace("$target$", "$1").replace("%hero%", "$2").replace("$hero$", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has recovered from the rupture!").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        if (hero.hasEffect("RuptureShotBuff")) {
            hero.removeEffect(hero.getEffect("RuptureShotBuff"));
            return SkillResult.SKIP_POST_USAGE;
        }
        hero.addEffect(new RuptureShotBuff(this));
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        final int mana = SkillConfigManager.getUseSetting(hero, this, "mana-per-shot", 1, true);
        damage = damage * duration / period;
        return getDescription().replace("$1", damage + "").replace("$2", duration / 1000 + "").replace("$3", mana + "");
    }

    public static class RuptureShotBuff extends ImbueEffect {

        public RuptureShotBuff(final Skill skill) {
            super(skill, "RuptureShotBuff");

            types.add(EffectType.POISON);
            types.add(EffectType.BENEFICIAL);
        }
    }

    public class ArrowRupture extends PeriodicDamageEffect {
        private final int staminaDrain;
        private final int manaDrain;

        public ArrowRupture(final Skill skill, final long period, final long duration, final double tickDamage, final Player applier, final int sDrain, final int mDrain) {
            super(skill, "ArrowRupture", applier, period, duration, tickDamage);
            this.types.add(EffectType.BLEED);
            staminaDrain = sDrain;
            manaDrain = mDrain;
            addPotionEffect(new PotionEffect(PotionEffectType.WITHER, (int) (duration / 1000) * 20, 0), true);
            //addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (duration / 1000) * 20, 5), true);
        }

        @Override
        public void tickHero(final Hero hero) {
            final int targetStam = hero.getStamina();
            if (targetStam > staminaDrain) {
                hero.setStamina(targetStam - staminaDrain);
                hero.getPlayer().sendMessage("you lost " + staminaDrain + " due to rupture");
            } else {
                hero.setStamina(0);
            }

            final int targetMana = hero.getMana();
            if (targetMana > manaDrain) {
                hero.getPlayer().sendMessage("you lost " + manaDrain + " due to rupture");
                hero.setMana(targetMana - manaDrain);
            } else {
                hero.setStamina(0);
            }
        }

        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + applyText, CustomNameManager.getName(monster), applier.getName());
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName(), applier.getName());
        }

        @Override
        public void removeFromMonster(final Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, CustomNameManager.getName(monster));
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }
    }

    public class SkillDamageListener implements Listener {

        private final Skill skill;

        public SkillDamageListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(final EntityDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof LivingEntity) || !(event instanceof EntityDamageByEntityEvent)) {
                return;
            }

            final LivingEntity target = (LivingEntity) event.getEntity();
            final EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;

            if (!(subEvent.getDamager() instanceof Arrow)) {
                return;
            }

            final Arrow arrow = (Arrow) subEvent.getDamager();
            if (!(arrow.getShooter() instanceof Player)) {
                return;
            }

            final Player player = (Player) arrow.getShooter();
            final Hero hero = plugin.getCharacterManager().getHero(player);

            if (hero.hasEffect("RuptureShotBuff")) {
                final long duration = SkillConfigManager.getUseSetting(hero, skill, "rupture-duration", 10000, false);
                final long period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 2000, true);
                final int tickDamage = SkillConfigManager.getUseSetting(hero, skill, "tick-damage", 2, false);
                final int staminaDrain = SkillConfigManager.getUseSetting(hero, skill, "stamina-drain", 100, false);
                final int manaDrain = SkillConfigManager.getUseSetting(hero, skill, "mana-drain", 50, false);
                plugin.getCharacterManager().getCharacter(target).addEffect(new ArrowRupture(skill, period, duration, tickDamage, player, staminaDrain, manaDrain));
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityShootBow(final EntityShootBowEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)) {
                return;
            }
            final Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("RuptureShotBuff")) {
                final int mana = SkillConfigManager.getUseSetting(hero, skill, "mana-per-shot", 1, true);
                if (hero.getMana() < mana) {
                    hero.removeEffect(hero.getEffect("RuptureShotBuff"));
                } else {
                    hero.setMana(hero.getMana() - mana);
                }
            }
        }
    }
}
