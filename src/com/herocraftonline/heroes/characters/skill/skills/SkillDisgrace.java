package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillDisgrace extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillDisgrace(Heroes plugin) {
        super(plugin, "Disgrace");
        setDescription("You disgrace your target, causing them to bleed, dealing $1 damage over $2 seconds.");
        setUsage("/skill disgrace <target>");
        setArgumentRange(0, 1);
        setTypes(SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
        setIdentifiers("skill disgrace");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 10000);
        node.set(Setting.PERIOD.node(), 2000);
        node.set(Setting.DAMAGE_TICK.node(), 1);
        node.set(Setting.DAMAGE_INCREASE.node(), 0);
        node.set(Setting.APPLY_TEXT.node(), "%target% is bleeding with disgrace!");
        node.set(Setting.EXPIRE_TEXT.node(), "");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT.node(), "%target% is bleeding with disgrace!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT.node(), "").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (target.equals(player) || hero.getSummons().contains(target)) {
            return SkillResult.INVALID_TARGET;
        }

        long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 2000, true);
        int tickDamage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE_TICK, 1, false);
        tickDamage += (SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE_INCREASE, 0, false) * hero.getSkillLevel(this));
        BleedSkillEffect bEffect = new BleedSkillEffect(this, duration, period, tickDamage, player);
        plugin.getCharacterManager().getCharacter(target).addEffect(bEffect);
        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.HURT_FLESH , 10.0F, 1.0F);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public class BleedSkillEffect extends PeriodicDamageEffect {

        public BleedSkillEffect(Skill skill, long duration, long period, int tickDamage, Player applier) {
            super(skill, "Disgrace", period, duration, tickDamage, applier);
            this.types.add(EffectType.BLEED);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
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
            broadcast(monster.getEntity().getLocation(), expireText, Messaging.getLivingEntityName(monster).toLowerCase());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 2000, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        damage += (SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE_INCREASE, 0, false) * hero.getSkillLevel(this));
        damage = damage * duration / period;
        return getDescription().replace("$1", damage + "").replace("$2", duration / 1000 + "");
    }
}
