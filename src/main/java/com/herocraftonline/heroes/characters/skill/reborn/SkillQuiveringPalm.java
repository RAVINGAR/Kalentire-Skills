package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillQuiveringPalm extends TargettedSkill {

    public SkillQuiveringPalm(Heroes plugin) {
        super(plugin, "QuiveringPalm");
        setDescription("Strike your target with a Quivering Palm dealing $1 damage and weakening the target, causing them to take $2% increased melee damage for $3 second(s). The strike also disorients the target, causing nausea.");
        setUsage("/skill quiveringpalm");
        setArgumentRange(0, 0);
        setIdentifiers("skill quiveringpalm");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.DEBUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new QuiveringPalmListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.0, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 8000, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        double damageMultiplier = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 1.2D, false);
        String formattedDamageMultiplier = Util.decFormat.format((damageMultiplier - 1.0) * 100.0);

        return getDescription().replace("$1", damage + "").replace("$2", formattedDamageMultiplier).replace("$3", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set(SkillSetting.DURATION.node(), 20000);
        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.0);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is weakened by a " + ChatColor.BOLD + "QuiveringPalm" + ChatColor.BOLD + "!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has recovered from the effects of the " + ChatColor.BOLD + "QuiveringPalm" + ChatColor.BOLD + "!");
        node.set("damage-multiplier", 1.075);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        // Display use Message
        broadcastExecuteText(hero, target);

        // Damage the target
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        // Play Sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6F, 2.0F);

        // Prep variables
        double damageMultiplier = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 1.2D, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 8000, false);

        String applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% is weakened by a " + ChatColor.BOLD + "QuiveringPalm" + ChatColor.BOLD + "!").replace("%target%", "$1");
        String expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% has recovered from the effects of the " + ChatColor.BOLD + "QuiveringPalm" + ChatColor.BOLD + "!").replace("%target%", "$1");

        // Add the debuff to the target
        CharacterTemplate targCT = this.plugin.getCharacterManager().getCharacter(target);
        QuiveringPalmEffect qpEffect = new QuiveringPalmEffect(this, hero.getPlayer(), duration, damageMultiplier, applyText, expireText);
        targCT.addEffect(qpEffect);

        //player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spawnParticle(Particle.CRIT_MAGIC, target.getLocation().add(0, 0.5, 0), 25, 0, 0, 0, 1);

        return SkillResult.NORMAL;
    }

    public class QuiveringPalmListener implements Listener {

        public QuiveringPalmListener() {}

        // Alter damage dealt by players when they are under the effect of quivering palm
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {

            if (!(event.getDamager() instanceof Hero) || !(event.getEntity() instanceof LivingEntity))
                return;

            // Ensure that the target is a living entity
            Entity targEnt = event.getEntity();
            if (!(targEnt instanceof LivingEntity))
                return;

            // Check to make sure that the target has the quivering palm effect
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter((LivingEntity) targEnt);
            if (!targetCT.hasEffect("QuiveringPalmed"))
                return;

            // Get the damage multiplier
            QuiveringPalmEffect qpEffect = (QuiveringPalmEffect) targetCT.getEffect("QuiveringPalmed");
            double damageMultiplier = qpEffect.getDamageModifier();

            // Alter the damage being dealt to the target
            double damage = event.getDamage() * damageMultiplier;
            event.setDamage(damage);

        }
    }

    // Effect required for implementing an internal cooldown on rune application
    public class QuiveringPalmEffect extends ExpirableEffect {

        private final double damageMultiplier;

        private final String applyText;
        private final String expireText;

        private final Player applier;

        public QuiveringPalmEffect(Skill skill, Player applier, long duration, double damageMultipler, String applyText, String expireText) {
            super(skill, "QuiveringPalmed", applier, duration);

            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.PHYSICAL);

            this.damageMultiplier = damageMultipler;
            this.applier = applier;
            this.applyText = applyText;
            this.expireText = expireText;

            addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (int) ((duration + 4000) / 1000) * 20, 3), false);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + applyText, CustomNameManager.getName(monster), applier.getName());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, CustomNameManager.getName(monster), applier.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

        public double getDamageModifier() {
            return damageMultiplier;
        }
    }
}