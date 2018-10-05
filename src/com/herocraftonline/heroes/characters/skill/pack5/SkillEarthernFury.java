package com.herocraftonline.heroes.characters.skill.pack5;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.AttributeDecreaseEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;

public class SkillEarthernFury extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillEarthernFury(Heroes plugin) {
        super(plugin, "EarthernFury");
        setDescription("You deliver a chilling strike to your target with your axe, dealing $1 physical damage and slowing them for $2 second(s).");
        setUsage("/skill earthernfury");
        setArgumentRange(0, 0);
        setIdentifiers("skill earthernfury");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.ABILITY_PROPERTY_EARTH, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.MOVEMENT_SLOWING);
    }

    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", damage + "").replace("$2", formattedDuration);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set("weapons", Util.axes);
        node.set("slownessAmplitude", 2);
        node.set("dexterity-reduction", 3);
        node.set(SkillSetting.DAMAGE.node(), 60);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.0);
        node.set(SkillSetting.DURATION.node(), 2500);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% feels the earthern fury of %hero%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer slowed!");

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% feels the earthern fury of %hero%!").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% is no longer slowed!").replace("%target%", "$1");
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.axes).contains(item.name())) {
            player.sendMessage("You can't use Earthern Fury with that weapon!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero, target);

        // Prep variables
        CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);

        // Damage the target and add the slow effect.
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        /* This is the new Particle API system for Spigot - the first few int = id, data, offsetX/Y/Z, speed, count, radius)
         * offset controls how spread out the particles are
         * id and data only work for two particles: ITEM_BREAK and TILE_BREAK
         * See line ~181 for my comments on this
         * */
        //player.getWorld().spigot().playEffect(target.getLocation().add(0, 1.0, 0), org.bukkit.Effect.TILE_BREAK, Material.SOUL_SAND.getId(), 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0, 1, 0), 25, 0, 0, 0, 1, Bukkit.createBlockData(Material.SOUL_SAND));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_HIT, 1.0F, 1.0F);
        
        // Create the effect and slow the target
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int dexterityReduction = SkillConfigManager.getUseSetting(hero, this, "dexterity-reduction", 3, false);
        int slownessAmplitude = SkillConfigManager.getUseSetting(hero, this, "slownessAmplitude", 2, false);
        EarthernFuryDexterityEffect cEffect = new EarthernFuryDexterityEffect(this, hero, duration, dexterityReduction, slownessAmplitude, applyText, expireText);
        targCT.addEffect(cEffect);

        return SkillResult.NORMAL;
    }

    private class EarthernFuryDexterityEffect extends AttributeDecreaseEffect {

        private BukkitTask effect;

        public EarthernFuryDexterityEffect(SkillEarthernFury skill, Hero applier, long duration, int decreaseValue, int slownessAmplitude, String applyText, String expireText) {
            super(skill, "EarthernFuryDexterityEffect", applier.getPlayer(), duration, AttributeType.DEXTERITY, decreaseValue, applyText, expireText);
            types.add(EffectType.SLOW);
            int tickDuration = (int) ((duration / 1000) * 20);
            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, tickDuration, slownessAmplitude), false);
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

        private void setEffect(LivingEntity entity) {
            
            final LivingEntity fEntity = entity;
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

                    fEntity.getLocation(location).add(0.7 * Math.sin(time * 16), 0, 0.7 * Math.cos(time * 16));
                    /* This is the new Particle API system for Spigot - the first few int = id, data, offsetX/Y/Z, speed, count, radius)
                     * offset controls how spread out the particles are
                     * id and data only work for two particles: ITEM_BREAK and TILE_BREAK
                     * BUT THE METHOD WAS REMOVED AND REPLACED SO I FIXED IT! ... all of them -_- ... regards, Soren.
                     * */
                    //fEntity.getWorld().spigot().playEffect(location, org.bukkit.Effect.TILE_BREAK, Material.SOUL_SAND.getId(), 0, 0, 0, 0, 0.1f, 25, 16);
                    fEntity.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 25, 0, 0, 0, 0.1, Bukkit.createBlockData(Material.SOUL_SAND));
                    fEntity.getWorld().playSound(location, Sound.BLOCK_GRAVEL_HIT, 0.1F, 1.0F);
                    
                    time += 0.01;
                }
            }.runTaskTimer(plugin, 0, 1);
        }

    }
}