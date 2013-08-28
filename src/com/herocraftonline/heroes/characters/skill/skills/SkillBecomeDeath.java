package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillBecomeDeath extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillBecomeDeath(Heroes plugin) {
        super(plugin, "BecomeDeath");
        setDescription("For $1 seconds you look undead. You no longer need to breath air, and are immune to poison.");
        setUsage("/skill becomedeath");
        setArgumentRange(0, 0);
        setIdentifiers("skill becomedeath");
        setTypes(SkillType.SILENCABLE, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_DARK, SkillType.FORM_ALTERING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 120000, false);

        return getDescription().replace("$1", duration / 1000 + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.USE_TEXT.node(), "");
        node.set(SkillSetting.DURATION.node(), 120000);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% shrouds themself in death!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% no longer appears as an undead!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% shrouds themself in death!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% no longer appears as an undead!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 120000, false);
        hero.addEffect(new BecomeDeathEffect(this, duration));
        for (Effect e : hero.getEffects()) {
            if (e.isType(EffectType.POISON) && e.isType(EffectType.HARMFUL)) {
                hero.removeEffect(e);
            }
        }

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ZOMBIE_IDLE, 0.7F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class BecomeDeathEffect extends ExpirableEffect {

        public BecomeDeathEffect(Skill skill, long duration) {
            super(skill, "BecomeDeathEffect", duration);
            addMobEffect(13, (int) (duration / 1000) * 20, 0, false);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.DARK);
            this.types.add(EffectType.MAGIC);
            this.types.add(EffectType.RESIST_POISON);
            this.types.add(EffectType.WATER_BREATHING);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }

    public class SkillEntityListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onEntityTarget(EntityTargetEvent event) {
            if (event.isCancelled() || !(event.getTarget() instanceof Player)) {
                return;
            }

            if (!isUndead(event.getEntity())) {
                return;
            }

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getTarget());
            if (hero.hasEffect("BecomeDeathEffect")) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || event.getDamage() == 0) {
                return;
            }

            if (!isUndead(event.getEntity()) || !(event instanceof EntityDamageByEntityEvent)) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            if (subEvent.getDamager() instanceof Player) {
                Hero hero = plugin.getCharacterManager().getHero((Player) subEvent.getDamager());
                if (hero.hasEffect("BecomeDeathEffect"))
                    hero.removeEffect(hero.getEffect("BecomeDeathEffect"));
            }
            else if (subEvent.getDamager() instanceof Projectile) {
                if (((Projectile) subEvent.getDamager()).getShooter() instanceof Player) {
                    Hero hero = plugin.getCharacterManager().getHero((Player) ((Projectile) subEvent.getDamager()).getShooter());
                    if (hero.hasEffect("BecomeDeathEffect"))
                        hero.removeEffect(hero.getEffect("BecomeDeathEffect"));
                }
            }
        }

        private boolean isUndead(Entity entity) {
            return entity instanceof Zombie || entity instanceof Skeleton || entity instanceof PigZombie || entity instanceof Ghast;
        }
    }
}
