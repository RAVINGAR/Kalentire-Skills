package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillCombustBlood extends TargettedSkill {

    public VisualEffect fplayer = new VisualEffect();		// Firework effect
    private String applyText;
    private String expireText;

    public SkillCombustBlood(Heroes plugin) {
        super(plugin, "CombustBlood");
        setDescription("Boil the blood of your target, dealing $1 dark damage. If you have Blood Union $2 or greater, the target will bleed, taking an additional $3 damage over $4 seconds. Increases Blood Union by 1.");
        setUsage("/skill combustblood");
        setArgumentRange(0, 0);
        setIdentifiers("skill combustblood");
        setTypes(SkillType.DAMAGING, SkillType.SILENCABLE, SkillType.HARMFUL, SkillType.DARK);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 100);
        node.set("blood-union-increase", 1);
        node.set("blood-union-required-for-dot", 3);
        node.set(SkillSetting.DAMAGE_TICK.node(), 17);
        node.set(SkillSetting.PERIOD.node(), 2500);
        node.set(SkillSetting.DURATION.node(), 7500);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatColor.GRAY + "["+ChatColor.DARK_GREEN+"Skill"+ ChatColor.GRAY+ "] %target% is bleeding from the effects of their Combusted Blood!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatColor.GRAY + "["+ChatColor.DARK_GREEN+"Skill"+ ChatColor.GRAY+ "] %target% is no longer bleeding.");
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100, false);
        int bloodUnionReq = SkillConfigManager.getUseSetting(hero, this, "blood-union-required-for-dot", 3, false);
        int damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 17, false);
        double period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false) / 1000;
        double duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false) / 1000;
        int dotDamage = (int) (damageTick * (duration / period));

        return getDescription().replace("$1", damage + "").replace("$2", bloodUnionReq + "").replace("$3", dotDamage + "").replace("$4", duration + "");
    }

    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatColor.GRAY + "["+ChatColor.DARK_GREEN+"Skill"+ ChatColor.GRAY+ "] %target% is bleeding from the effects of their Combusted Blood!").replace("%target%", "$1");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatColor.GRAY + "["+ChatColor.DARK_GREEN+"Skill"+ ChatColor.GRAY+ "] %target% is no longer bleeding.").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();

        // Deal damage
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100, false);
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC);

        broadcastExecuteText(hero, target);
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0.0D, 1.5D, 0.0D), FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.STAR).withColor(Color.MAROON).withFade(Color.RED).build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Get Blood Union Level
        int bloodUnionLevel = 0;
        if (hero.hasEffect("BloodUnionEffect")) {
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");

            bloodUnionLevel = buEffect.getBloodUnionLevel();
        }

        // Add DoT if blood union is high enough.
        int bloodUnionRequirement = SkillConfigManager.getUseSetting(hero, this, "blood-union-required-for-dot", 3, false);
        if (bloodUnionLevel >= bloodUnionRequirement) {

            // Get DoT values
            int tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 17, false);
            int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);
            int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);

            // Add DoT effect
            CombustingBloodEffect cbEffect = new CombustingBloodEffect(this, period, duration, tickDamage, hero.getPlayer());
            CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
            targCT.addEffect(cbEffect);
        }

        // Increase Blood Union
        if (hero.hasEffect("BloodUnionEffect")) {
            int bloodUnionIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-increase", 1, false);
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");

            if (target instanceof Player)
                buEffect.addBloodUnion(bloodUnionIncrease, true);
            else
                buEffect.addBloodUnion(bloodUnionIncrease, false);
        }

        // Finish
        return SkillResult.NORMAL;
    }

    public class CombustingBloodEffect extends PeriodicDamageEffect {

        private final Player applier;

        public CombustingBloodEffect(Skill skill, long period, long duration, double tickDamage, Player applier) {
            super(skill, "CombustingBloodEffect", period, duration, tickDamage, applier);

            this.applier = applier;
            types.add(EffectType.BLEED);
            types.add(EffectType.HARMFUL);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), applyText, Messaging.getLivingEntityName(monster), applier.getDisplayName());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), expireText, Messaging.getLivingEntityName(monster), applier.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }
}
