package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Particle;
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
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;

public class SkillCurse extends TargettedSkill {

    private String applyText;
    private String expireText;
    private String missText;

    public SkillCurse(Heroes plugin) {
        super(plugin, "Curse");
        setDescription("You curse the target for $1 seconds, giving their attacks a $2% miss chance.");
        setUsage("/skill curse");
        setArgumentRange(0, 0);
        setIdentifiers("skill curse");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.DEBUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEventListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
        node.set(SkillSetting.DURATION.node(), 7000);
        node.set("miss-chance", 0.50);
        node.set("miss-text", ChatComponents.GENERIC_SKILL + "%target% misses an attack!");
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been cursed!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has recovered from the curse!");
        node.set(SkillSetting.REAGENT.node(), 318);
        node.set(SkillSetting.REAGENT_COST.node(), 1);

        return node;
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
    public void init() {
        super.init();

        missText = SkillConfigManager.getRaw(this, "miss-text", ChatComponents.GENERIC_SKILL + "%target% misses an attack!");
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been cursed!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has recovered from the curse!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        double missChance = SkillConfigManager.getUseSetting(hero, this, "miss-chance", .50, false);
        plugin.getCharacterManager().getCharacter(target).addEffect(new CurseEffect(this, player, duration, missChance));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8F, 1.0F);
        
        //target.getWorld().spigot().playEffect(target.getLocation(), Effect.WITCH_MAGIC, 0, 0, 0.5F, 1.0F, 0.5F, 0.5F, 35, 16);
        target.getWorld().spawnParticle(Particle.SPELL_WITCH, target.getLocation(), 35, 0.5, 1, 0.5, 0.5);

        return SkillResult.NORMAL;
    }

    public class CurseEffect extends ExpirableEffect {

        private final double missChance;

        public CurseEffect(Skill skill, Player applier, long duration, double missChance) {
            super(skill, "Curse", applier, duration, applyText, expireText); //TODO Implicit broadcast() call - may need changes?

            types.add(EffectType.HARMFUL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);

            this.missChance = missChance;
        }

        public double getMissChance() {
            return missChance;
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
                if (cEffect != null && Util.nextRand() < cEffect.getMissChance()) {
                    event.setCancelled(true);
                    //character.getEntity().getWorld().spigot().playEffect(character.getEntity().getLocation(), Effect.WITCH_MAGIC, 0, 0, 0.5F, 1.0F, 0.5F, 0.5F, 35, 16);
                    character.getEntity().getWorld().spawnParticle(Particle.SPELL_WITCH, character.getEntity().getLocation(), 35, 0.5, 1, 0.5, 0.5);
                    broadcast(character.getEntity().getLocation(), missText.replace("%target%", CustomNameManager.getName(character)));
                }
            }
        }
    }
}
