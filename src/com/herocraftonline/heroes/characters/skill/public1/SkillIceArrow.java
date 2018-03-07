package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.ImbueEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
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

public class SkillIceArrow extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillIceArrow(Heroes plugin) {
        super(plugin, "IceArrow");
        setDescription("Your arrows will freeze their target, but drain $1 mana per shot.");
        setUsage("/skill iarrow");
        setArgumentRange(0, 0);
        setIdentifiers("skill iarrow", "skill icearrow");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_ICE, SkillType.SILENCEABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000); // 5 seconds
        node.set("speed-multiplier", 2);
        node.set("mana-per-shot", 1); // How much mana for each attack
        node.set(SkillSetting.USE_TEXT.node(), "%hero% imbues their arrows with ice!");
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% is slowed by ice!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero%'s arrows are no longer imbued with ice!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        setUseText("%hero% imbues their arrows with ice!".replace("%hero%", "$1"));
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%target% is slowed by %hero%!").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "%hero%'s arrows are no longer imbued with ice!");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        if (hero.hasEffect("IceArrowBuff")) {
            hero.removeEffect(hero.getEffect("IceArrowBuff"));
            return SkillResult.SKIP_POST_USAGE;
        }
        hero.addEffect(new IceArrowBuff(this));
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class IceArrowBuff extends ImbueEffect {

        public IceArrowBuff(Skill skill) {
            super(skill, "IceArrowBuff");
            types.add(EffectType.ICE);
            setDescription("ice arrow");
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText.replace("%hero%", player.getDisplayName()));
        }
    }

    public class SkillDamageListener implements Listener {

        private final Skill skill;

        public SkillDamageListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
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

            if (hero.hasEffect("IceArrowBuff")) {
                final long duration = SkillConfigManager.getUseSetting(hero, skill, "duration", 5000, false);
                final int amplifier = SkillConfigManager.getUseSetting(hero, skill, "speed-multiplier", 2, false);
                final SlowEffect iceSlowEffect = new SlowEffect(skill, hero.getPlayer(), duration, amplifier, applyText, "$1 is no longer slowed."); //TODO Implicit broadcast() call - may need changes?
                final LivingEntity target = (LivingEntity) event.getEntity();
                plugin.getCharacterManager().getCharacter(target).addEffect(iceSlowEffect);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityShootBow(EntityShootBowEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)) {
                return;
            }
            final Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("IceArrowBuff")) {
                final int mana = SkillConfigManager.getUseSetting(hero, skill, "mana-per-shot", 1, true);
                if (hero.getMana() < mana) {
                    hero.removeEffect(hero.getEffect("IceArrowBuff"));
                } else {
                    hero.setMana(hero.getMana() - mana);
                }
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        final int mana = SkillConfigManager.getUseSetting(hero, this, "mana-per-shot", 1, false);
        return getDescription().replace("$1", mana + "");
    }
}
