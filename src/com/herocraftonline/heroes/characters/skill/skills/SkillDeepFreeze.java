package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillDeepFreeze extends TargettedSkill {

    private String applyText;
    private String expireText;
    private String shatterText;

    public SkillDeepFreeze(Heroes plugin) {
        super(plugin, "DeepFreeze");
        setDescription("You freeze your target for $1 seconds and $2 ice damage. Burning the target will shatter the effect for $3 extra damage.");
        setUsage("/skill deepfreeze <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill deepfreeze", "skill dfreeze");
        setTypes(SkillType.ICE, SkillType.SILENCABLE, SkillType.DEBUFF, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.INTERRUPT);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
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
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% was frozen in place!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has thawed from their icy prison!").replace("%target%", "$1");
        shatterText = SkillConfigManager.getRaw(this, "shatter-text", "%target%'s icy prison shattered from the intense heat!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        
        //Deal the damage to the player
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false);
        damageEntity(target, hero.getPlayer(), damage, DamageCause.MAGIC);
        
        // Add the effect to the entity
        plugin.getCharacterManager().getCharacter(target).addEffect(new FreezeEffect(this, duration, hero));        
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public class FreezeEffect extends PeriodicExpirableEffect {

        private static final long period = 100;
        private final Hero applier;
        private double x, y, z;

        public FreezeEffect(Skill skill, long duration, Hero applier) {
            super(skill, "Freeze", period, duration);
            this.applier = applier;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.ICE);
            this.types.add(EffectType.ROOT);
            this.types.add(EffectType.MAGIC);
            this.types.add(EffectType.UNBREAKABLE);
            int effectDuration = (int) duration / 1000 * 20;
            this.addMobEffect(2, effectDuration, 5, false);
            this.addMobEffect(8, effectDuration, -5, false);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            monster.getEntity().setFireTicks(0);
            Location location = monster.getEntity().getLocation();
            x = location.getX();
            y = location.getY();
            z = location.getZ();

            broadcast(location, applyText, Messaging.getLivingEntityName(monster));
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            player.setFireTicks(0);
            Location location = player.getLocation();
            x = location.getX();
            y = location.getY();
            z = location.getZ();

            broadcast(location, applyText, player.getDisplayName());
        }

        public Hero getApplier() {
            return applier;
        }
        
        public void shatter(CharacterTemplate character) {
            if(character instanceof Hero) {
                super.removeFromHero((Hero) character);
            } else if (character instanceof Monster) {
                super.removeFromMonster((Monster) character);
            }
            LivingEntity lEntity = character.getEntity();
            int damage = SkillConfigManager.getUseSetting(applier, skill, "shatter-damage", 7, false);
            addSpellTarget(lEntity, applier);
            damageEntity(lEntity, applier.getPlayer(), damage, DamageCause.MAGIC);
            broadcast(lEntity.getLocation(), shatterText, Messaging.getLivingEntityName(lEntity));
        }
        
        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), expireText, Messaging.getLivingEntityName(monster));
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

        @Override
        public void tickMonster(Monster monster) {
            Location initial = monster.getEntity().getLocation();
            Location location = initial;
            if (location.getX() != x || location.getY() != y || location.getZ() != z) {
                location.setX(x);
                location.setY(y);
                location.setZ(z);
                location.setYaw(initial.getYaw());
                location.setPitch(initial.getPitch());
                monster.getEntity().teleport(location);
            }
        }
        
        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();
            Location location = player.getLocation();
            if (location.getX() != x || location.getY() != y || location.getZ() != z) {
                location.setX(x);
                location.setY(y);
                location.setZ(z);
                location.setYaw(player.getLocation().getYaw());
                location.setPitch(player.getLocation().getPitch());
                player.teleport(location);
            }
        }
    }

    public class SkillEntityListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityCombust(EntityCombustEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }
            CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            if (character.hasEffect("Freeze")) {
                FreezeEffect fEffect = (FreezeEffect) character.getEffect("Freeze");
                fEffect.shatter(character);
                character.manualRemoveEffect(fEffect);
            }
        }
        
        @EventHandler(priority = EventPriority.MONITOR)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.isCancelled() || event.getDamage() == 0 || !event.getSkill().isType(SkillType.FIRE)) {
                return;
            }
            CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            if (character.hasEffect("Freeze")) {
                FreezeEffect fEffect = (FreezeEffect) character.getEffect("Freeze");
                fEffect.shatter(character);
                character.manualRemoveEffect(fEffect);
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false);
        int shatter = SkillConfigManager.getUseSetting(hero, this, "shatter-damage", 11, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        return getDescription().replace("$1", duration / 1000 + "").replace("$2", damage + "").replace("$3", shatter + "");
    }
}
