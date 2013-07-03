package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillDespair extends ActiveSkill {
    private String applyText;
    private String expireText;

    public VisualEffect fplayer = new VisualEffect(); //fireworks
    public SkillDespair(Heroes plugin) {
        super(plugin, "Despair");
        setDescription("Blinds all enemies within $1 blocks for $2 seconds and deals $3 dark damage.");
        setUsage("/skill despair");
        setArgumentRange(0, 0);
        setIdentifiers("skill despair");
        setTypes(SkillType.DARK, SkillType.SILENCABLE, SkillType.HARMFUL);
    }

    @Override
    public String getDescription(Hero hero) {
        long duration = (long) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false) + (SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0.0, false) * hero.getSkillLevel(this))) / 1000;
        int damage = (int) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 0, false) + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(this)));
        int radius = (int) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false) + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE, 0.0, false) * hero.getSkillLevel(this)));
        String description = getDescription().replace("$1", radius + "").replace("$2", duration + "").replace("$3", damage + "");
        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.RADIUS.node(), 10);
        node.set(SkillSetting.RADIUS_INCREASE.node(), 0);
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set("duration-increase", 0);
        node.set(SkillSetting.DAMAGE.node(), 0);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0);
        node.set("exp-per-blinded-player", 0);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% has blinded %target% with %skill%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% has recovered their sight!");
        return node;
    }
    
    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% has blinded %target% with %skill%!").replace("%hero%", "$1").replace("%target%", "$2").replace("%skill%", "$3");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% has recovered their sight!").replace("%hero%", "$1").replace("%target%", "$2").replace("%skill%", "$3");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 10, false);
        radius += SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE, 0.0, false) * hero.getSkillLevel(this);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false);
        duration += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE, 0.0, false) * hero.getSkillLevel(this);
        Player player = hero.getPlayer();
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 0, false);
        damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(this);
        //int exp = SkillConfigManager.getUseSetting(hero, this, "exp-per-blinded-player", 0, false);
        DespairEffect dEffect = new DespairEffect(this, duration, player);
        //int hit = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity)) {
                continue;
            }
            CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) entity);
            character.addEffect(dEffect);
            if (damage > 0) {
                addSpellTarget(entity, hero);
                damageEntity(character.getEntity(), player, damage, DamageCause.MAGIC);
            }
            //hit++;
        }
        /*if (hit == 0) {
            Messaging.send(player, "No valid targets within range!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        if (exp > 0) {
            if (hero.hasParty()) {
                hero.getParty().gainExp(exp * hit, ExperienceType.SKILL, player.getLocation());
            } else {
                hero.gainExp(exp * hit, ExperienceType.SKILL, hero.getViewingLocation(1.0));
            }
        }
        */
        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.PORTAL , 0.5F, 1.0F);
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), 
            		player.getLocation()
            		.add(0,2.5,0), 
            		FireworkEffect.builder()
            		.flicker(false).trail(false)
            		.with(FireworkEffect.Type.BURST)
            		.withColor(Color.NAVY).withFade(Color.BLACK)
            		.build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class DespairEffect extends ExpirableEffect {

        private final Player player;
        public DespairEffect(Skill skill, long duration, Player player) {
            super(skill, "Despair", duration);
            this.player = player;
            this.addMobEffect(15, (int) ((duration / 1000) * 20), 3, false);
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.DARK);
            this.types.add(EffectType.BLIND);
        }
        
        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            broadcast(hero.getPlayer().getLocation(), applyText, player.getDisplayName(), hero.getPlayer().getDisplayName(), "Despair");
        }
        
        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            broadcast(hero.getPlayer().getLocation(), expireText, player.getDisplayName(), hero.getPlayer().getDisplayName(), "Despair");
        }
    }
}