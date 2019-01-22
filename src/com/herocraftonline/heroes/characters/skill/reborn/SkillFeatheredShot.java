package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillFeatheredShot extends ActiveSkill {

    private String applyText;
    private String expireText;



    public SkillFeatheredShot(Heroes plugin) {
        super(plugin, "FeatheredShot");
        setDescription("The next $1 seconds, your arrows will apply jump boost and slow fall to slow down your target... ");
        setUsage("/skill featheredshot");
        setArgumentRange(0, 0);
        setIdentifiers("skill featheredshot");
        setTypes(SkillType.DEBUFFING, SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 5, false);

        String formattedDamage = Util.decFormat.format(damage);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.DAMAGE.node(), 5);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), (double) 2);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has deadly feathers attached to their bow.");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s bow no longer has deadly feathers attached to their bow.");
        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has deadly feathers attached to their bow.").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s bow no longer has deadly feathers attached to their bow.").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        hero.addEffect(new FeatheredEffect(this, hero.getPlayer(), duration));
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }


    public class FeatheredEffect extends ExpirableEffect {

        public FeatheredEffect(Skill skill, Player applier, long duration) {
            super(skill, "FeatheredArrows", applier, duration, applyText, expireText);
            types.add(EffectType.IMBUE);
            types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            for (final com.herocraftonline.heroes.characters.effects.Effect effect : hero.getEffects()) {
                if (effect.equals(this)) {
                    continue;
                }

                if (effect.isType(EffectType.IMBUE)) {
                    hero.removeEffect(effect);
                }
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }
    }

    public class SkillDamageListener implements Listener {
        private final Skill skill;

        public SkillDamageListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

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

            int potionDuration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 50, false);

            if (!hero.hasEffect("FeatheredArrows"))
                return;

            if (hero.hasEffect("FeatheredArrows")) {
                final LivingEntity target = (LivingEntity) event.getEntity();
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 80 * potionDuration / 1000, 5));
                target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 80 * potionDuration / 1000, 2));
            }

        }
    }
    }




