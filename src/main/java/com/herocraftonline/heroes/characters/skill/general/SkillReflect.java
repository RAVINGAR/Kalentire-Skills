package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroesDamageEvent;
import com.herocraftonline.heroes.api.events.ProjectileDamageEvent;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

public class SkillReflect extends ActiveSkill implements Listenable {
    private final Listener listener;
    private String applyText;
    private String expireText;

    public SkillReflect(Heroes plugin) {
        super(plugin, "Reflect");
        setDescription("You reflect $1% of all damage back to your attacker for $2 second(s).");
        setUsage("/skill reflect");
        setArgumentRange(0, 0);
        setIdentifiers("skill reflect");
        setTypes(SkillType.FORCE, SkillType.SILENCEABLE, SkillType.BUFFING);
        listener = new SkillHeroListener(this);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set("grants-immunity", false);
        node.set("reflected-amount", 0.5);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% put up a reflective shield!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% lost his reflective shield!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% put up a reflective shield!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% lost his reflective shield!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        double reflectAmount = SkillConfigManager.getUseSetting(hero, this, "reflected-amount", 0.5, false);
        boolean grantsImmunity = SkillConfigManager.getUseSetting(hero, this, "grants-immunity", false);
        hero.addEffect(new ReflectEffect(this, hero.getPlayer(), duration, reflectAmount, grantsImmunity));

        return SkillResult.NORMAL;
    }

    @NotNull
    @Override
    public Listener getListener() {
        return listener;
    }

    public class ReflectEffect extends ExpirableEffect {

        private final double reflectAmount;
        private final boolean grantsImmunity;

        public ReflectEffect(Skill skill, Player applier, long duration, double reflectAmount, boolean grantsImmunity) {
            super(skill, "Reflect", applier, duration);
            this.reflectAmount = reflectAmount;
            this.grantsImmunity = grantsImmunity;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getName());
        }

    }

    public class SkillHeroListener implements Listener {

        private final Skill skill;
        private final BukkitScheduler scheduler = plugin.getServer().getScheduler();

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            handleEvent(event);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            CharacterTemplate character = event.getDefender();
            if(character.hasEffect("Reflect")) {
                ReflectEffect effect = (((ReflectEffect) character.getEffect("Reflect")));
                double damage = event.getDamage() * effect.reflectAmount;
                event.setDamage(effect.grantsImmunity ? 0 : Math.max(0, event.getDamage() - damage));

                if(event.getDamage() == 0) {
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onProjectileDamage(ProjectileDamageEvent event) {
            handleEvent(event);
        }

        private void handleEvent(HeroesDamageEvent event) {
            CharacterTemplate character = event.getDefender();
            if (character.hasEffect("Reflect")) {
                ReflectEffect effect = (((ReflectEffect) character.getEffect("Reflect")));
                double damage = event.getDamage() * effect.reflectAmount;
                event.setDamage(effect.grantsImmunity ? 0 : Math.max(0, event.getDamage() - damage));

                if(event.getDamage() == 0) {
                    event.setCancelled(true);
                }
                LivingEntity attacker = event.getAttacker().getEntity();
                if(!plugin.getCharacterManager().getCharacter(attacker).hasEffect("Reflect") && damage > 0) {
                    scheduler.scheduleSyncDelayedTask(plugin, () -> {
                        plugin.getDamageManager().addSpellTarget(attacker, character, skill);
                        damageEntity(attacker, character.getEntity(), damage, EntityDamageEvent.DamageCause.MAGIC);
                    });
                }
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double amount = SkillConfigManager.getUseSetting(hero, this, "reflected-amount", .5, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        return getDescription().replace("$1", Util.stringDouble(amount * 100)).replace("$2", duration / 1000 + "");
    }
}
