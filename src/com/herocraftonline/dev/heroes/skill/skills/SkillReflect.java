package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.HeroesEventListener;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.api.WeaponDamageEvent;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.ExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Setting;
import com.herocraftonline.dev.heroes.util.Util;

public class SkillReflect extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillReflect(Heroes plugin) {
        super(plugin, "Reflect");
        setDescription("You reflect $1% of all damage back to your attacker for $2 seconds.");
        setUsage("/skill reflect");
        setArgumentRange(0, 0);
        setIdentifiers("skill reflect");
        setTypes(SkillType.FORCE, SkillType.SILENCABLE, SkillType.BUFF);

        registerEvent(Type.CUSTOM_EVENT, new SkillHeroListener(this), Priority.Monitor);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000);
        node.set("reflected-amount", 0.5);
        node.set(Setting.APPLY_TEXT.node(), "%hero% put up a reflective shield!");
        node.set(Setting.EXPIRE_TEXT.node(), "%hero% lost his reflective shield!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%hero% put up a reflective shield!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%hero% lost his reflective shield!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        hero.addEffect(new ReflectEffect(this, duration));

        return SkillResult.NORMAL;
    }

    public class ReflectEffect extends ExpirableEffect {

        public ReflectEffect(Skill skill, long duration) {
            super(skill, "Reflect", duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

    }

    public class SkillHeroListener extends HeroesEventListener {

        private final Skill skill;

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @Override
        public void onWeaponDamage(WeaponDamageEvent event) {
            Heroes.debug.startTask("HeroesSkillListener");
            if (event.isCancelled() || !(event.getEntity() instanceof Player) || !(event.getDamager() instanceof LivingEntity)) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }
            
            LivingEntity attacker = (LivingEntity) event.getDamager();
            Player player = (Player) event.getEntity();
            Hero hero = plugin.getHeroManager().getHero(player);
            if (hero.hasEffect("Reflect")) {
                int damage = (int) (event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "reflected-amount", 0.5, false));
                plugin.getDamageManager().addSpellTarget(attacker, hero, skill);
                skill.damageEntity(attacker, player, damage, DamageCause.MAGIC);
            }
            Heroes.debug.stopTask("HeroesSkillListener");
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double amount = SkillConfigManager.getUseSetting(hero, this, "reflected-amount", .5, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        return getDescription().replace("$1", Util.stringDouble(amount * 100)).replace("$2", duration / 1000 + "");
    }
}
