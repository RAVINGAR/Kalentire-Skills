package com.herocraftonline.heroes.characters.skill.unusedskills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

public class SkillReflect extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillReflect(Heroes plugin) {
        super(plugin, "Reflect");
        setDescription("You reflect $1% of all damage back to your attacker for $2 seconds.");
        setUsage("/skill reflect");
        setArgumentRange(0, 0);
        setIdentifiers("skill reflect");
        setTypes(SkillType.FORCE, SkillType.SILENCEABLE, SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set("reflected-amount", 0.5);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% put up a reflective shield!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% lost his reflective shield!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% put up a reflective shield!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% lost his reflective shield!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        double reflectAmount = SkillConfigManager.getUseSetting(hero, this, "reflected-amount", 0.5, false);
        hero.addEffect(new ReflectEffect(this, hero.getPlayer(), duration, reflectAmount));

        return SkillResult.NORMAL;
    }

    public class ReflectEffect extends ExpirableEffect {

        private final double reflectAmount;

        public ReflectEffect(Skill skill, Player applier, long duration, double reflectAmount) {
            super(skill, "Reflect", applier, duration);
            this.reflectAmount = reflectAmount;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
        }

        public double getReflectAmount() {
            return reflectAmount;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

    }

    public class SkillHeroListener implements Listener {

        private final Skill skill;

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }
            CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            if (character.hasEffect("Reflect")) {
                double damage = event.getDamage() * ((ReflectEffect) character.getEffect("Reflect")).reflectAmount;
                plugin.getDamageManager().addSpellTarget(event.getDamager().getEntity(), character, skill);
                Skill.damageEntity(event.getDamager().getEntity(), character.getEntity(), damage, DamageCause.MAGIC);
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double amount = SkillConfigManager.getUseSetting(hero, this, "reflected-amount", .5, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        return getDescription().replace("$1", Util.stringDouble(amount * 100)).replace("$2", duration / 1000 + "");
    }
}
