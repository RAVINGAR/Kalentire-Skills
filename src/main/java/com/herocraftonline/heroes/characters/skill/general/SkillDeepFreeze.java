package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillDeepFreeze extends TargettedSkill {

    private String applyText;
    private String expireText;
    private String shatterText;

    public SkillDeepFreeze(final Heroes plugin) {
        super(plugin, "DeepFreeze");
        setDescription("You freeze your target for $1 second(s) and $2 ice damage. Burning the target will shatter the effect for $3 extra damage.");
        setUsage("/skill deepfreeze <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill deepfreeze", "skill dfreeze");
        setTypes(SkillType.ABILITY_PROPERTY_ICE, SkillType.SILENCEABLE, SkillType.DEBUFFING, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.INTERRUPTING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.DAMAGE.node(), 1);
        node.set("shatter-damage", 11);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% was frozen in place!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has thawed from their icy prison!");
        node.set("shatter-text", "%target%'s icy prison shattered from the intense heat!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% was frozen in place!").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has thawed from their icy prison!");
        shatterText = SkillConfigManager.getRaw(this, "shatter-text", "%target%'s icy prison shattered from the intense heat!").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);

        //Deal the damage to the player
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false);
        damageEntity(target, hero.getPlayer(), damage, DamageCause.MAGIC);

        // Add the effect to the entity
        plugin.getCharacterManager().getCharacter(target).addEffect(new FreezeEffect(this, duration, hero));
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(final Hero hero) {
        final int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false);
        final int shatter = SkillConfigManager.getUseSetting(hero, this, "shatter-damage", 11, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        return getDescription().replace("$1", duration / 1000 + "").replace("$2", damage + "").replace("$3", shatter + "");
    }

    public class FreezeEffect extends RootEffect {

        private static final int period = 100;
        private final Hero applierHero;

        public FreezeEffect(final Skill skill, final long duration, final Hero applierHero) {
            super(skill, "Freeze", applierHero.getPlayer(), period, duration, applyText, null); //TODO Implicit broadcast() call - may need changes?
            this.types.add(EffectType.ICE);
            this.types.add(EffectType.UNBREAKABLE);
            this.applierHero = applierHero;
        }

        // Override the apply methods to make sure the entity isn't on fire, which would cause instant thawing with extra damage.
        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);
            monster.getEntity().setFireTicks(0);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            hero.getPlayer().setFireTicks(0);
        }

        // Override the removal methods to broadcast the expire text only if the effect expires naturally.
        @Override
        public void removeFromMonster(final Monster monster) {
            super.removeFromMonster(monster);
            if (expireText != null && expireText.length() > 0 && !monster.hasEffectType(EffectType.SILENT_ACTIONS)) {
                broadcast(monster.getEntity().getLocation(), "    " + expireText, CustomNameManager.getName(monster));
            }
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            if (expireText != null && expireText.length() > 0) {
                if (hero.hasEffectType(EffectType.SILENT_ACTIONS)) {
                    player.sendMessage("    " + expireText.replace("%target%", player.getName()));
                } else {
                    this.broadcast(player.getLocation(), "    " + expireText, player.getName());
                }
            }
        }

        public void shatter(final CharacterTemplate character) {
            if (character instanceof Hero) {
                super.removeFromHero((Hero) character);
            } else if (character instanceof Monster) {
                super.removeFromMonster((Monster) character);
            }
            final LivingEntity lEntity = character.getEntity();
            final double damage = SkillConfigManager.getUseSetting(applierHero, skill, "shatter-damage", 7, false);
            addSpellTarget(lEntity, applierHero);
            damageEntity(lEntity, applier, damage, DamageCause.MAGIC);
            broadcast(lEntity.getLocation(), shatterText, CustomNameManager.getName(lEntity));
        }
    }

    public class SkillEntityListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityCombust(final EntityCombustEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }
            final CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            if (character.hasEffect("Freeze")) {
                final FreezeEffect fEffect = (FreezeEffect) character.getEffect("Freeze");
                if (fEffect != null) {
                    fEffect.shatter(character);
                    character.removeEffect(fEffect);
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onSkillDamage(final SkillDamageEvent event) {
            if (event.isCancelled() || event.getDamage() == 0 || !event.getSkill().isType(SkillType.ABILITY_PROPERTY_FIRE)) {
                return;
            }
            final CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            if (character.hasEffect("Freeze")) {
                final FreezeEffect fEffect = (FreezeEffect) character.getEffect("Freeze");
                if (fEffect != null) {
                    fEffect.shatter(character);
                    character.removeEffect(fEffect);
                }
            }
        }
    }
}
