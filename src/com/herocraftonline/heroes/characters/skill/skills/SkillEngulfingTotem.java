package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.AttributeDecreaseEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.totem.SkillBaseTotem;
import com.herocraftonline.heroes.characters.skill.skills.totem.Totem;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class SkillEngulfingTotem extends SkillBaseTotem {

    public SkillEngulfingTotem(Heroes plugin) {
        super(plugin, "EngulfingTotem");
        setArgumentRange(0,0);
        setUsage("/skill engulfingtotem");
        setIdentifiers("skill engulfingtotem");
        setDescription("Places an engulfing totem at target location that reduces the agility of non-partied entites in a $1 radius by $2. Lasts for $3 seconds.");
        setTypes(SkillType.MOVEMENT_SLOWING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero h) {
        return getDescription()
                .replace("$1", getRange(h) + "")
                .replace("$2", getAgilityReduceAmount(h) + "")
                .replace("$3", getDuration(h)*0.001 + "");
    }

    @Override
    public Material[] getMaterials() {
        return new Material[] {
                Material.SOUL_SAND,
                Material.SOUL_SAND,
                Material.SOUL_SAND,
                Material.SOUL_SAND,
                Material.SOUL_SAND,
        };
    }

    @Override
    public void usePower(Hero hero, Totem totem) {
        Player heroP = hero.getPlayer();
        for(LivingEntity entity : totem.getTargets(hero)) {
            if(!damageCheck(heroP, entity)) {
                continue;
            }
            Player player = null;
            if(entity instanceof Player) {
                player = (Player) entity;
            }
            long duration = getAgilityReduceDuration(hero);
            EngulfingTotemAgilityEffect sEffect = new EngulfingTotemAgilityEffect(this, hero, duration, getAgilityReduceAmount(hero), getSlownessAmplitude(hero), null, getExpireText());
            CharacterTemplate character = plugin.getCharacterManager().getCharacter(entity);
            if(!character.hasEffect("EngulfingTotemAgilityEffect")) {
                String name;
                if(player != null) {
                    name = player.getName();
                }
                else name = Messaging.getLivingEntityName(character);
                broadcast(entity.getLocation(), getApplyText(), name, heroP.getName());
            }
            else {
                ((EngulfingTotemAgilityEffect)character.getEffect("EngulfingTotemAgilityEffect")).setExpireText(null);
            }
            character.addEffect(sEffect);
        }
    }

    @Override
    public ConfigurationSection getSpecificDefaultConfig(ConfigurationSection node) {
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "    " + "$1 is engulfed by a totem's power!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "$1 is no longer engulfed by a totem's power.");
        node.set("agility-reduce-amount", 3);
        node.set("agility-reduce-duration", 8000);
        node.set("slowness-amplitude", 2);
        return node;
    }

    // Methods to grab config info that is specific to this skill
    public int getAgilityReduceAmount(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, "agility-reduce-amount", 3, false);
    }

    public long getAgilityReduceDuration(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, "agility-reduce-duration", 8000, false);
    }

    public int getSlownessAmplitude(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, "slowness-amplitude", 2, false);
    }

    public String getApplyText() {
        return SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "$1 is engulfed by a totem's power!");
    }

    public String getExpireText() {
        return SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "$1 is no longer engulfed by a totem's power.");
    }

    private class EngulfingTotemAgilityEffect extends AttributeDecreaseEffect {

        private BukkitTask effect;

        public EngulfingTotemAgilityEffect(SkillEngulfingTotem skill, Hero applier, long duration, int decreaseValue, int slownessAmplitude, String applyText, String expireText) {
            super(skill, "EngulfingTotemAgilityEffect", applier.getPlayer(), duration, AttributeType.AGILITY, decreaseValue, applyText, expireText);
            types.add(EffectType.SLOW);

            int tickDuration = (int) ((duration / 1000) * 20);
            addMobEffect(2, tickDuration, slownessAmplitude, false);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            setEffect(hero.getPlayer());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            effect.cancel();
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            setEffect(monster.getEntity());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            effect.cancel();
        }

        private void setEffect(final LivingEntity entity) {
            
            /*final LivingEntity fEntity = entity;;
            effect = new BukkitRunnable() {

                private Location location = fEntity.getLocation();

                private double time = 0;

                @SuppressWarnings("deprecation")
                @Override
                public void run() {
                    if(!fEntity.isValid()) {
                        cancel();
                        return;
                    }
                    // Reset the timer, just in case. Don't want it going too high. Though 100 is pretty high.
                    if(time > 100.0) {
                        time = 0.0;
                    }

                    entity.getLocation(location).add(0.7 * Math.sin(time * 16), 0, 0.7 * Math.cos(time * 16));
                    /* This is the new Particle API system for Spigot - the first few int = id, data, offsetX/Y/Z, speed, count, radius)
                     * offset controls how spread out the particles are
                     * id and data only work for two particles: ITEM_BREAK and TILE_BREAK
                     * *
                    entity.getWorld().spigot().playEffect(location, Effect.TILE_BREAK, Material.SOUL_SAND.getId(), 0, 0, 0, 0, 0.1f, 25, 16);
                    fEntity.getWorld().playSound(location, Sound.DIG_GRAVEL, 0.1F, 1.0F);
                    
                    time += 0.01;
                }
            }.runTaskTimer(plugin, 0, 1);*/
        }

    }
}