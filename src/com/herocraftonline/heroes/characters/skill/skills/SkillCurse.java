package com.herocraftonline.heroes.characters.skill.skills;

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
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCABLE, SkillType.AGGRESSIVE, SkillType.DEBUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEventListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        double chance = SkillConfigManager.getUseSetting(hero, this, "miss-chance", 0.5, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedChance = Util.decFormat.format(chance * 100.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedChance);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
        node.set(SkillSetting.DURATION.node(), 7000);
        node.set("miss-chance", 0.50);
        node.set("miss-text", Messaging.getSkillDenoter() + "%target% misses an attack!");
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% has been cursed!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% has recovered from the curse!");
        node.set(SkillSetting.REAGENT.node(), Integer.valueOf(318));
        node.set(SkillSetting.REAGENT_COST.node(), Integer.valueOf(1));

        return node;
    }

    @Override
    public void init() {
        super.init();

        missText = SkillConfigManager.getRaw(this, "miss-text", Messaging.getSkillDenoter() + "%target% misses an attack!").replace("%target%", "$1");
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% has been cursed!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% has recovered from the curse!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        double missChance = SkillConfigManager.getUseSetting(hero, this, "miss-chance", .50, false);
        plugin.getCharacterManager().getCharacter(target).addEffect(new CurseEffect(this, player, duration, missChance));

        player.getWorld().playSound(player.getLocation(), Sound.GHAST_MOAN, 0.8F, 1.0F);

        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(),
                                 target.getLocation().add(0, 2, 0),
                                 FireworkEffect.builder()
                                               .flicker(false).trail(true)
                                               .with(FireworkEffect.Type.CREEPER)
                                               .withColor(Color.PURPLE)
                                               .withFade(Color.GREEN)
                                               .build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
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

    public class CurseEffect extends ExpirableEffect {

        private final double missChance;

        public CurseEffect(Skill skill, Player applier, long duration, double missChance) {
            super(skill, "Curse", applier, duration);

            types.add(EffectType.HARMFUL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);

            this.missChance = missChance;
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + applyText, Messaging.getLivingEntityName(monster).toLowerCase());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        public double getMissChance() {
            return missChance;
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, Messaging.getLivingEntityName(monster).toLowerCase());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }
    }
}
