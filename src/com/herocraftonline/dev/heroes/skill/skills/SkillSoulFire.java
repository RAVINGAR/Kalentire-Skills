package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
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
import com.herocraftonline.dev.heroes.effects.common.CombustEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;
import com.herocraftonline.dev.heroes.util.Util;

public class SkillSoulFire extends ActiveSkill {

    private String applyText;
    private String expireText;
    private String igniteText;

    public SkillSoulFire(Heroes plugin) {
        super(plugin, "SoulFire");
        setDescription("Gives your attacks a chance to ignite the opponent");
        setUsage("/skill soulfire");
        setArgumentRange(0, 0);
        setIdentifiers("skill soulfire");
        setTypes(SkillType.FIRE, SkillType.BUFF, SkillType.SILENCABLE);

        registerEvent(Type.ENTITY_DAMAGE, new SkillDamageListener(this), Priority.Monitor);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("weapons", Util.swords);
        node.set(Setting.APPLY_TEXT.node(), "%hero%'s weapon is sheathed in flame!");
        node.set(Setting.EXPIRE_TEXT.node(), "%hero%'s weapon is no longer aflame!");
        node.set(Setting.DURATION.node(), 600000);
        node.set("ignite-chance", 0.20);
        node.set("ignite-duration", 5000);
        node.set("ignite-text", "%hero% has lit %target% on fire with soulfire!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%hero%'s weapon is sheathed in flame!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%hero%'s weapon is no longer aflame!").replace("%hero%", "$1");
        igniteText = SkillConfigManager.getRaw(this, "ignite-text", "%hero% has lit %target% on fire with soulfire!").replace("%hero%", "$1").replace("%target%", "$2");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 600000, false);
        hero.addEffect(new SoulFireEffect(this, duration));
        return SkillResult.NORMAL;
    }

    public class SkillDamageListener extends EntityListener {

        private final Skill skill;

        public SkillDamageListener(Skill skill) {
            this.skill = skill;
        }

        @Override
        public void onEntityDamage(EntityDamageEvent event) {
            Heroes.debug.startTask("HeroesSkillListener");
            if (event.isCancelled() || !(event.getEntity() instanceof LivingEntity) || !(event instanceof EntityDamageByEntityEvent)) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            if (!(subEvent.getDamager() instanceof Player)) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }

            Player player = (Player) subEvent.getDamager();
            Hero hero = plugin.getHeroManager().getHero(player);
            if (!SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.swords).contains(player.getItemInHand().getType().name()) || !hero.hasEffect("SoulFire")) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }

            double chance = SkillConfigManager.getUseSetting(hero, skill, "ignite-chance", .2, false);
            if (Util.rand.nextDouble() >= chance) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }

            int fireTicks = SkillConfigManager.getUseSetting(hero, skill, "ignite-duration", 5000, false) / 50;
            LivingEntity target = (LivingEntity) event.getEntity();
            target.setFireTicks(fireTicks);

            if (target instanceof Player)
                plugin.getHeroManager().getHero((Player) target).addEffect(new CombustEffect(skill, player));
            else
                plugin.getEffectManager().addEntityEffect(target, new CombustEffect(skill, player));
            

            String name = null;
            if (event.getEntity() instanceof Player)
                name = ((Player) target).getName();
            else
                name = Messaging.getLivingEntityName(target);
            

            broadcast(player.getLocation(), igniteText, player.getDisplayName(), name);
            Heroes.debug.stopTask("HeroesSkillListener");
        }
    }

    public class SoulFireEffect extends ExpirableEffect {

        public SoulFireEffect(Skill skill, long duration) {
            super(skill, "SoulFire", duration);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.FIRE);
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
}
