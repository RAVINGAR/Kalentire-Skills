package com.herocraftonline.heroes.characters.skill.public1;

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
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillCurse extends TargettedSkill {

    private String applyText;
    private String expireText;
    private String missText;

    public SkillCurse(Heroes plugin) {
        super(plugin, "Curse");
        this.setDescription("You curse the target for $1 seconds, giving their attacks a $2% miss chance.");
        this.setUsage("/skill curse <target>");
        this.setArgumentRange(0, 1);
        this.setIdentifiers("skill curse");
        this.setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.DEBUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEventListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
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
        this.missText = SkillConfigManager.getRaw(this, "miss-text", "%target% misses an attack!").replace("%target%", "$1");
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%target% has been cursed!").replace("%target%", "$1");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "%target% has recovered from the curse!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        final double missChance = SkillConfigManager.getUseSetting(hero, this, "miss-chance", .50, false);
        this.plugin.getCharacterManager().getCharacter(target).addEffect(new CurseEffect(this, hero.getPlayer(), duration, missChance));
        return SkillResult.NORMAL;

    }

    public class CurseEffect extends ExpirableEffect {

        private final double missChance;

        public CurseEffect(Skill skill, Player applier, long duration, double missChance) {
            super(skill, "Curse", applier, duration);
            this.missChance = missChance;
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            this.broadcast(monster.getEntity().getLocation(), SkillCurse.this.applyText, CustomNameManager.getName(monster).toLowerCase());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), SkillCurse.this.applyText, player.getDisplayName());
        }

        public double getMissChance() {
            return this.missChance;
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            this.broadcast(monster.getEntity().getLocation(), SkillCurse.this.expireText, CustomNameManager.getName(monster).toLowerCase());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), SkillCurse.this.expireText, player.getDisplayName());
        }
    }

    public class SkillEventListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.isCancelled() || (event.getDamage() == 0)) {
                return;
            }

            final CharacterTemplate character = event.getDamager();
            if (character.hasEffect("Curse")) {
                final CurseEffect cEffect = (CurseEffect) character.getEffect("Curse");
                if (Util.nextRand() < cEffect.missChance) {
                    event.setCancelled(true);
                    SkillCurse.this.broadcast(character.getEntity().getLocation(), SkillCurse.this.missText, CustomNameManager.getName(character));
                }
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        final double chance = SkillConfigManager.getUseSetting(hero, this, "miss-chance", .5, false);
        return this.getDescription().replace("$1", (duration / 1000) + "").replace("$2", (chance * 100) + "");
    }
}
