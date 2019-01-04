package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.api.events.EffectAddEvent;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.util.Util;

public class SkillReflect extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillReflect(Heroes plugin) {
        super(plugin, "Reflect");
        setDescription("You reflect all abilities back to your attacker for $2 seconds.");
        setUsage("/skill reflect");
        setArgumentRange(0, 0);
        setIdentifiers("skill reflect");
        setTypes(SkillType.FORCE, SkillType.SILENCEABLE, SkillType.BUFFING);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000);
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
    public String getDescription(Hero hero) {
        double amount = SkillConfigManager.getUseSetting(hero, this, "reflected-amount", .5, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        return getDescription().replace("$1", Util.stringDouble(amount * 100)).replace("$2", duration / 1000 + "");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        hero.addEffect(new ReflectShieldEffect(this, hero.getPlayer(), duration));

        return SkillResult.NORMAL;
    }

    public class ReflectShieldEffect extends ExpirableEffect {

        public ReflectShieldEffect(Skill skill, Player applier, long duration) {
            super(skill, "ReflectShield", applier, duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Bukkit.getServer().getPluginManager().registerEvents(new ReflectBuffListener(hero), plugin);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

        public class ReflectBuffListener implements Listener {
            private final Hero _hero;

            public ReflectBuffListener(Hero hero) {
                this._hero = hero;
            }

            @EventHandler(priority = EventPriority.HIGH)
            public void onEffectAdd(EffectAddEvent event) {
                if (event.getCharacter() != _hero)
                    return;
                if (!_hero.hasEffect("ReflectShield"))
                    return;

                Effect effect = event.getEffect();
                effect.removeFromHero(_hero);

                Player originalCaster = effect.getApplier();
                Hero originalCastingHero = plugin.getCharacterManager().getHero(originalCaster);

                effect.setApplier(_hero.getPlayer());
                originalCastingHero.addEffect(effect);
            }

            @EventHandler(priority = EventPriority.MONITOR)
            public void onWeaponDamage(WeaponDamageEvent event) {
                if (event.isCancelled() || !(event.getEntity() instanceof LivingEntity))
                    return;
                CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
                if (!character.hasEffect("ReflectShield"))
                    return;

                double damage = event.getDamage();
                plugin.getDamageManager().addSpellTarget(event.getDamager().getEntity(), character, skill);
                damageEntity(event.getDamager().getEntity(), character.getEntity(), damage, DamageCause.MAGIC);
            }
        }
    }
}
