package com.herocraftonline.heroes.characters.skill.pack7;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;

public class SkillPlague extends TargettedSkill {
    private String applyText;
    private String expireText;

    public SkillPlague(Heroes plugin) {
        super(plugin, "Plague");
        setUsage("/skill plague");
        setDescription("You cast a plague, dealing $1 damage to everything and spreading to nearby enemies. $2 $3");
        setArgumentRange(0, 0);
        setIdentifiers("skill plague");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DISEASE, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
    }

    @Override
    public String getDescription(Hero hero) {

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 4, false);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 17, false);
        double tickDamageIncrease = hero.getAttributeValue(AttributeType.INTELLECT) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.17, false);
        tickDamage += tickDamageIncrease;

        String formattedDamage = Util.decFormat.format(tickDamage * ((double) duration / (double) period));
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration).replace("$3", radius + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
        node.set(SkillSetting.DURATION.node(), 15000);
        node.set(SkillSetting.PERIOD.node(), 1500);
        node.set(SkillSetting.DAMAGE_TICK.node(), (double) 10);
        node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 0.125);
        node.set(SkillSetting.RADIUS.node(), 4);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is infected with the plague!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer infected with the plague!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% is infected with the plague!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% is no longer infected with the plague!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, true);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, (double) 17, false);
        double tickDamageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 0.17, false);
        tickDamage += (tickDamageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        plugin.getCharacterManager().getCharacter(target).addEffect(new PlagueEffect(this, player, duration, period, tickDamage));

        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_BAT_HURT.value(), 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class PlagueEffect extends PeriodicDamageEffect {
        private boolean jumped = false;

        public PlagueEffect(Skill skill, Player applier, long duration, long period, double tickDamage) {
            super(skill, "Plague", applier, period, duration, tickDamage);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.DISEASE);

            addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (duration / 1000) * 20, 0), true);
        }

        // Clone Constructor
        private PlagueEffect(PlagueEffect pEffect) {
            super(pEffect.getSkill(), pEffect.getName(), pEffect.getApplier(), pEffect.getPeriod(), pEffect.getRemainingTime(), pEffect.tickDamage);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.DISEASE);
            types.add(EffectType.HARMFUL);

            this.jumped = true;
            addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (pEffect.getRemainingTime() / 1000) * 20, 0), true);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, CustomNameManager.getName(monster));
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

        @Override
        public void tickMonster(Monster monster) {
            super.tickMonster(monster);
            spreadToNearbyEntities(monster.getEntity());
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
            spreadToNearbyEntities(hero.getPlayer());
        }

        /**
         * Attempts to spread the effect to all nearby entities
         * Will not target non-pvpable targets
         * 
         * @param lEntity
         */
        private void spreadToNearbyEntities(LivingEntity lEntity) {
            if (jumped) {
                return;
            }
            Hero applyHero = plugin.getCharacterManager().getHero(getApplier());
            int radius = SkillConfigManager.getUseSetting(applyHero, skill, SkillSetting.RADIUS.node(), 4, false);
            for (Entity target : lEntity.getNearbyEntities(radius, radius, radius)) {
                if (!(target instanceof LivingEntity)) {
                    continue;
                }

                if (!damageCheck(getApplier(), (LivingEntity) target)) {
                    continue;
                }

                CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) target);
                if (character.hasEffect("Plague")) {
                    continue;
                }
                else {
                    character.addEffect(new PlagueEffect(this));
                }
            }
        }
    }
}
