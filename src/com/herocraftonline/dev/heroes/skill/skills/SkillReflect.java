package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityListener;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.ExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillReflect extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillReflect(Heroes plugin) {
        super(plugin, "Reflect");
        setDescription("Reflects all the damage done to you back to your target");
        setUsage("/skill reflect");
        setArgumentRange(0, 0);
        setIdentifiers("skill reflect");
        setTypes(SkillType.FORCE, SkillType.SILENCABLE, SkillType.BUFF);

        registerEvent(Type.ENTITY_DAMAGE, new SkillEntityListener(this), Priority.Normal);
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
        applyText = SkillConfigManager.getRaw(null, Setting.APPLY_TEXT, "%hero% put up a reflective shield!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(null, Setting.EXPIRE_TEXT, "%hero% lost his reflective shield!").replace("%hero%", "$1");
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

    public class SkillEntityListener extends EntityListener {

        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @Override
        public void onEntityDamage(EntityDamageEvent event) {
            Heroes.debug.startTask("HeroesSkillListener");
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent)) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }

            EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;
            Entity defender = edbe.getEntity();
            Entity attacker = edbe.getDamager();
            if (attacker instanceof LivingEntity && defender instanceof Player) {
                Player defPlayer = (Player) defender;
                Hero hero = plugin.getHeroManager().getHero(defPlayer);
                if (hero.hasEffect("Reflect")) {
                    if (attacker instanceof Player) {
                        Player attPlayer = (Player) attacker;
                        if (plugin.getHeroManager().getHero(attPlayer).hasEffect(getName())) {
                            event.setCancelled(true);
                            Heroes.debug.stopTask("HeroesSkillListener");
                            return;
                        }
                    }
                    LivingEntity attEntity = (LivingEntity) attacker;
                    int damage = (int) (event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "reflected-amount", 0.5, false));
                    plugin.getDamageManager().addSpellTarget(attacker, hero, skill);
                    attEntity.damage(damage, defender);
                }
            }
            Heroes.debug.stopTask("HeroesSkillListener");
        }
    }
}
