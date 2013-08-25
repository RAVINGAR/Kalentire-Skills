package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillCurse extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    private String applyText;
    private String expireText;
    private String missText;

    public SkillCurse(Heroes plugin) {
        super(plugin, "Curse");
        setDescription("You curse the target for $1 seconds, giving their attacks a $2% miss chance.");
        setUsage("/skill curse");
        setArgumentRange(0, 0);
        setIdentifiers("skill curse");
        setTypes(SkillType.DARK, SkillType.SILENCABLE, SkillType.HARMFUL, SkillType.DEBUFF);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEventListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000); // in milliseconds
        node.set("miss-chance", .50); // decimal representation of miss-chance
        node.set("miss-text", "%target% misses an attack!");
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% has been cursed!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has recovered from the curse!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        missText = SkillConfigManager.getRaw(this, "miss-text", "%target% misses an attack!").replace("%target%", "$1");
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%target% has been cursed!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "%target% has recovered from the curse!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        double missChance = SkillConfigManager.getUseSetting(hero, this, "miss-chance", .50, false);
        plugin.getCharacterManager().getCharacter(target).addEffect(new CurseEffect(this, duration, missChance));
        Player player = hero.getPlayer();
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.GHAST_MOAN , 0.8F, 1.0F);
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), 
            		target.getLocation().add(0,2,0), 
            		FireworkEffect.builder()
            		.flicker(false).trail(true)
            		.with(FireworkEffect.Type.CREEPER)
            		.withColor(Color.PURPLE)
            		.withFade(Color.GREEN)
            		.build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;

    }

    public class CurseEffect extends ExpirableEffect {

        private final double missChance;

        public CurseEffect(Skill skill, long duration, double missChance) {
            super(skill, "Curse", duration);
            this.missChance = missChance;
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), applyText, Messaging.getLivingEntityName(monster).toLowerCase());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        public double getMissChance() {
            return missChance;
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), expireText, Messaging.getLivingEntityName(monster).toLowerCase());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }

    public class SkillEventListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.isCancelled() || event.getDamage() == 0) {
                return;
            }

            CharacterTemplate character = event.getDamager();
            if (character.hasEffect("Curse")) {
                CurseEffect cEffect = (CurseEffect) character.getEffect("Curse");
                if (Util.nextRand() < cEffect.missChance) {
                    event.setCancelled(true);
                    broadcast(character.getEntity().getLocation(), missText, Messaging.getLivingEntityName(character));
                }
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        double chance = SkillConfigManager.getUseSetting(hero, this, "miss-chance", .5, false);
        return getDescription().replace("$1", duration / 1000 + "").replace("$2", chance * 100 + "");
    }
}