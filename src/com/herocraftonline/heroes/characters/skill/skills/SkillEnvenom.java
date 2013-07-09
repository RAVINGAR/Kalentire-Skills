package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillEnvenom extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillEnvenom(Heroes plugin) {
        super(plugin, "Envenom");
        setDescription("Apply a poison to a melee weapon$1. Your next $5 then poison them, causing $2 damage over $3 seconds. If you do not attack a target for $4 seconds, your weapon loses the effect. $1");
        setUsage("/skill envenom");
        setArgumentRange(0, 0);
        setIdentifiers("skill envenom");
        setTypes(SkillType.PHYSICAL, SkillType.HARMFUL, SkillType.DAMAGING, SkillType.BUFF);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        double buffDuration = SkillConfigManager.getUseSetting(hero, this, "buff-duration", 600000, false) / 1000.0;
        int damage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 19, false);
        double period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false) / 1000.0;
        double duration = SkillConfigManager.getUseSetting(hero, this, "poison-duration", 10000, false) / 1000.0;

        // calculate actual total damage
        damage = (int) ((duration / period) * damage);

        boolean rangedAttacksEnabled = SkillConfigManager.getUseSetting(hero, this, "ranged-attacks-enabled", false);

        String rangedEnabledString = "";
        if (rangedAttacksEnabled)
            rangedEnabledString = " or arrow";

        String numAttacksString = "attack";
        int numAttacks = SkillConfigManager.getUseSetting(hero, this, "attacks", Integer.valueOf(1), false);
        if (numAttacks > 1)
            numAttacksString = numAttacks + " attacks";

        return getDescription().replace("$1", rangedEnabledString + "").replace("$5", numAttacksString + "").replace("$2", damage + "").replace("$3", duration + "").replace("$4", buffDuration + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("weapons", Util.swords);
        node.set("buff-duration", Integer.valueOf(20000));
        node.set("poison-duration", Integer.valueOf(18000));
        node.set(SkillSetting.PERIOD.node(), Integer.valueOf(6000));
        node.set("tick-damage", Integer.valueOf(25));
        node.set("attacks", Integer.valueOf(1));
        node.set("ranged-attacks-enabled", Boolean.valueOf(false));

        node.set(SkillSetting.APPLY_TEXT.node(), "%target% is poisoned!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has recovered from the poison!");
        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% is poisoned!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has recovered from the poison!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        long duration = SkillConfigManager.getUseSetting(hero, this, "buff-duration", Integer.valueOf(6000), false);
        int numAttacks = SkillConfigManager.getUseSetting(hero, this, "attacks", Integer.valueOf(1), false);
        boolean rangedAttacksEnabled = SkillConfigManager.getUseSetting(hero, this, "ranged-attacks-enabled", false);

        hero.addEffect(new EnvenomBuffEffect(this, duration, numAttacks, rangedAttacksEnabled));

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public class SkillDamageListener implements Listener {
        private final Skill skill;

        public SkillDamageListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if ((!(event instanceof EntityDamageByEntityEvent)) || (!(event.getEntity() instanceof LivingEntity))) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;

            // Check for both arrow shots and left click attacks. Determine player based on which we're dealing with		
            boolean arrow = false;
            Player player;
            Entity damagingEntity = ((EntityDamageByEntityEvent) event).getDamager();
            if (damagingEntity instanceof Arrow) {
                if (!(((Projectile) damagingEntity).getShooter() instanceof Player))
                    return;

                player = (Player) ((Projectile) damagingEntity).getShooter();
                arrow = true;
            }
            else if (subEvent.getCause() == DamageCause.ENTITY_ATTACK) {
                if (!(subEvent.getDamager() instanceof Player))
                    return;

                player = (Player) subEvent.getDamager();
            }
            else
                return;

            Hero hero = plugin.getCharacterManager().getHero(player);

            if (!hero.hasEffect("EnvenomBuffEffect"))
                return;

            boolean rangedAttacksEnabled = SkillConfigManager.getUseSetting(hero, skill, "ranged-attacks-enabled", false);
            if (arrow == true && !rangedAttacksEnabled) {
                // We're dealing with a ranged attack, but ranged attacks are disabled.
                return;
            }

            long duration = SkillConfigManager.getUseSetting(hero, skill, "poison-duration", Integer.valueOf(18000), false);
            long period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, Integer.valueOf(6000), false);
            int tickDamage = SkillConfigManager.getUseSetting(hero, skill, "tick-damage", 25, false);

            EnvenomPoisonEffect epEffect = new EnvenomPoisonEffect(skill, period, duration, tickDamage, player);

            Entity target = event.getEntity();
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter((LivingEntity) target);

            if (arrow == true && rangedAttacksEnabled) {
                // We're dealing with a ranged attack, and ranged attacks are enabled.
                targetCT.addEffect(epEffect);
                checkBuff(hero);

                return;
            }
            else {
                // Not dealing with a ranged attack. Check to see if they're wielding a proper weapon.

                ItemStack item = player.getItemInHand();
                if (!SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.swords).contains(item.getType().name())) {
                    return;
                }

                targetCT.addEffect(epEffect);
                checkBuff(hero);

                return;
            }
        }

        private void checkBuff(Hero hero) {
            EnvenomBuffEffect abBuff = (EnvenomBuffEffect) hero.getEffect("EnvenomBuffEffect");
            abBuff.applicationsLeft -= 1;
            if (abBuff.applicationsLeft < 1) {
                hero.removeEffect(abBuff);
            }
        }
    }

    public class EnvenomBuffEffect extends ExpirableEffect {

        protected boolean rangedApplicationEnabled = false;
        protected int applicationsLeft = 1;

        public EnvenomBuffEffect(Skill skill, long duration, int numAttacks, boolean rangedApplicationEnabled) {
            super(skill, "EnvenomBuffEffect", duration);

            if (rangedApplicationEnabled)
                types.add(EffectType.IMBUE);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.POISON);

            this.applicationsLeft = numAttacks;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            for (final Effect effect : hero.getEffects()) {
                if (effect.equals(this)) {
                    continue;
                }

                if (effect.isType(EffectType.IMBUE)) {
                    hero.removeEffect(effect);
                }
            }

            Player player = hero.getPlayer();
            if (rangedApplicationEnabled)
                Messaging.send(player, "You poison your weapons.");
            else
                Messaging.send(player, "You poison your weapon.");
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Messaging.send(hero.getPlayer(), "Your weapon is no longer poisoned!");
        }

        public int getApplicationsLeft() {
            return applicationsLeft;
        }

        public void setApplicationsLeft(int applicationsLeft) {
            this.applicationsLeft = applicationsLeft;
        }
    }

    public class EnvenomPoisonEffect extends PeriodicDamageEffect {

        public EnvenomPoisonEffect(Skill skill, long period, long duration, int tickDamage, Player applier) {
            super(skill, "EnvenomPoisonEffect", period, duration, tickDamage, applier);

            types.add(EffectType.POISON);

            addMobEffect(19, (int) (duration / 1000) * 20, 0, true);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), applyText, Messaging.getLivingEntityName(monster));
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
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
    }
}
