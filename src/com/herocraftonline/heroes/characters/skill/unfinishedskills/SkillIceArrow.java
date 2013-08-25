package com.herocraftonline.heroes.characters.skill.unfinishedskills;

/*
package com.herocraftonline.heroes.characters.skill.skills;

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

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.ImbueEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillIceArrow extends ActiveSkill {

    private String slowApplyText;
    private String expireText;
    private String slowExpireText;

    public SkillIceArrow(Heroes plugin) {
        super(plugin, "IceArrow");
        setDescription("Your arrows will freeze their target, but drain $1 mana per shot.");
        setUsage("/skill iarrow");
        setArgumentRange(0, 0);
        setIdentifiers("skill iarrow", "skill icearrow");
        setTypes(SkillType.BUFF, SkillType.ICE, SkillType.SILENCABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000); // 5 seconds
        node.set("speed-multiplier", 2);
        node.set("mana-per-shot", 1); // How much mana for each attack
        node.set(SkillSetting.USE_TEXT.node(), "%hero% imbues their arrows with ice!");
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% is slowed by ice!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero%'s arrows are no longer imbued with ice!");
        node.set("slow-expire-message", "%target% is no longer slowed by ice!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        setUseText("%hero% imbues their arrows with ice!".replace("%hero%", "$1"));
        slowApplyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%target% is slowed by ice!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "%hero%'s arrows are no longer imbued with ice!").replace("%hero%", "$1");
        slowExpireText = SkillConfigManager.getRaw(this, "slow-expire-message", "%target% is no longer slowed by ice!").replace("%target%", "$1");
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
            types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
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

            if (hero.hasEffect("IceArrowBuff")) {
                long duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 5000, false);
                int amplifier = SkillConfigManager.getUseSetting(hero, skill, "speed-multiplier", 2, false);
                SlowEffect iceSlowEffect = new SlowEffect(skill, duration, amplifier, false, slowApplyText, slowExpireText, hero);
                LivingEntity target = (LivingEntity) event.getEntity();
                plugin.getCharacterManager().getCharacter(target).addEffect(iceSlowEffect);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityShootBow(EntityShootBowEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)) {
                return;
            }
            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("IceArrowBuff")) {
                int mana = SkillConfigManager.getUseSetting(hero, skill, "mana-per-shot", 1, true);
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
        int mana = SkillConfigManager.getUseSetting(hero, this, "mana-per-shot", 1, false);
        return getDescription().replace("$1", mana + "");
    }
}
*/