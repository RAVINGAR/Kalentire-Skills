package com.herocraftonline.heroes.characters.skill.general;

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

public class SkillPoisonArrow extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillPoisonArrow(final Heroes plugin) {
        super(plugin, "PoisonArrow");
        this.setDescription("Your arrows will poison their target dealing $1 damage over $2 second(s), each arrow will drain $3 mana.");
        this.setUsage("/skill parrow");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill parrow", "skill poisonarrow");
        this.setTypes(SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000); // milliseconds
        node.set(SkillSetting.PERIOD.node(), 2000); // 2 seconds in milliseconds
        node.set("mana-per-shot", 1); // How much mana for each attack
        node.set("tick-damage", 2);
        node.set(SkillSetting.USE_TEXT.node(), "%hero% imbues their arrows with poison!");
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% is poisoned!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has recovered from the poison!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.setUseText("%hero% imbues their arrows with poison!".replace("%hero%", "$1").replace("$hero$", "$1"));
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% is poisoned!").replace("%target%", "$1").replace("$target$", "$1");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has recovered from the poison!").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        if (hero.hasEffect("PoisonArrowBuff")) {
            hero.removeEffect(hero.getEffect("PoisonArrowBuff"));
            return SkillResult.SKIP_POST_USAGE;
        }
        hero.addEffect(new PoisonArrowBuff(this));
        this.broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        final int mana = SkillConfigManager.getUseSetting(hero, this, "mana-per-shot", 1, true);
        damage = (damage * duration) / period;
        return this.getDescription().replace("$1", damage + "").replace("$2", (duration / 1000) + "").replace("$3", mana + "");
    }

    public static class PoisonArrowBuff extends ImbueEffect {

        public PoisonArrowBuff(final Skill skill) {
            super(skill, "PoisonArrowBuff");
            this.types.add(EffectType.POISON);
            this.setDescription("poison");
        }
    }

    public class ArrowPoison extends PeriodicDamageEffect {

        public ArrowPoison(final Skill skill, final long period, final long duration, final double tickDamage, final Player applier) {
            super(skill, "ArrowPoison", applier, period, duration, tickDamage);
            this.types.add(EffectType.POISON);
            this.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (20 * duration / 1000), 0), true);
        }

        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);
            this.broadcast(monster.getEntity().getLocation(), SkillPoisonArrow.this.applyText, CustomNameManager.getName(monster).toLowerCase());
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), SkillPoisonArrow.this.applyText, player.getDisplayName());
        }

        @Override
        public void removeFromMonster(final Monster monster) {
            super.removeFromMonster(monster);
            this.broadcast(monster.getEntity().getLocation(), SkillPoisonArrow.this.expireText, CustomNameManager.getName(monster).toLowerCase());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), SkillPoisonArrow.this.expireText, player.getDisplayName());
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
            final Hero hero = SkillPoisonArrow.this.plugin.getCharacterManager().getHero(player);

            if (hero.hasEffect("PoisonArrowBuff")) {
                final long duration = SkillConfigManager.getUseSetting(hero, this.skill, "poison-duration", 10000, false);
                final long period = SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.PERIOD, 2000, true);
                final double tickDamage = SkillConfigManager.getUseSetting(hero, this.skill, "tick-damage", 2, false);
                SkillPoisonArrow.this.plugin.getCharacterManager().getCharacter(target).addEffect(new ArrowPoison(this.skill, period, duration, tickDamage, player));
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityShootBow(final EntityShootBowEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)) {
                return;
            }
            final Hero hero = SkillPoisonArrow.this.plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("PoisonArrowBuff")) {
                final int mana = SkillConfigManager.getUseSetting(hero, this.skill, "mana-per-shot", 1, true);
                if (hero.getMana() < mana) {
                    hero.removeEffect(hero.getEffect("PoisonArrowBuff"));
                } else {
                    hero.setMana(hero.getMana() - mana);
                }
            }
        }
    }
}
