package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillMortalWound extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillMortalWound(Heroes plugin) {
        super(plugin, "MortalWound");
        setDescription("You strike your target reducing healing by $1%, and causing them to bleed for $2 damage over $3 seconds.");
        setUsage("/skill mortalwound <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill mortalwound", "skill mwound");
        setTypes(SkillType.PHYSICAL, SkillType.DAMAGING, SkillType.DEBUFF, SkillType.HARMFUL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("weapons", Util.swords);
        node.set(Setting.DURATION.node(), 12000);
        node.set(Setting.PERIOD.node(), 3000);
        node.set("heal-multiplier", .5);
        node.set("tick-damage", 1);
        node.set(Setting.MAX_DISTANCE.node(), 2);
        node.set(Setting.APPLY_TEXT.node(), "%target% has been mortally wounded by %hero%!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% has recovered from their mortal wound!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%target% has been mortally wounded by %hero%!").replace("%target%", "$1").replace("$2", "%hero%");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%target% has recovered from their mortal wound!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        HeroClass heroClass = hero.getHeroClass();

        Material item = player.getItemInHand().getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords).contains(item.name())) {
            Messaging.send(player, "You can't Mortal Strike with that weapon!");
        }

        int damage = heroClass.getItemDamage(item) == null ? 0 : heroClass.getItemDamage(item);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 12000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 3000, true);
        int tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        double healMultiplier = SkillConfigManager.getUseSetting(hero, this, "heal-multiplier", 0.5, true);
        MortalWound mEffect = new MortalWound(this, period, duration, tickDamage, player, healMultiplier);
        if (target instanceof Player) {
            plugin.getHeroManager().getHero((Player) target).addEffect(mEffect);
        } else
            plugin.getEffectManager().addEntityEffect(target, mEffect);

        return SkillResult.NORMAL;
    }

    public class MortalWound extends PeriodicDamageEffect {

        private final double healMultiplier;

        public MortalWound(Skill skill, long period, long duration, int tickDamage, Player applier, double healMultiplier) {
            super(skill, "MortalWound", period, duration, tickDamage, applier);
            this.healMultiplier = healMultiplier;
            this.types.add(EffectType.BLEED);
        }

        @Override
        public void apply(LivingEntity lEntity) {
            super.apply(lEntity);
        }

        @Override
        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName(), applier.getDisplayName());
        }

        @Override
        public void remove(LivingEntity lEntity) {
            super.remove(lEntity);
            broadcast(lEntity.getLocation(), expireText, Messaging.getLivingEntityName(lEntity).toLowerCase(), applier.getDisplayName());
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);

            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }

    public class SkillEntityListener implements Listener {

        @EventHandler(priority = EventPriority.LOWEST)
        public void onEntityRegainHealth(EntityRegainHealthEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
                return;
            }

            Hero hero = plugin.getHeroManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("MortalWound")) {
                MortalWound mEffect = (MortalWound) hero.getEffect("MortalWound");
                event.setAmount((int) (event.getAmount() * mEffect.healMultiplier));
            }
        }
        
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onHeroRegainHealth(HeroRegainHealthEvent event) {
            if (event.isCancelled()) {
                return;
            }

            if (event.getHero().hasEffect("MortalWound")) {
                MortalWound mEffect = (MortalWound) event.getHero().getEffect("MortalWound");
                event.setAmount((int) (event.getAmount() * mEffect.healMultiplier));
            }
        }
    }
    
    @Override
    public String getDescription(Hero hero) {
        double heal = 1 - SkillConfigManager.getUseSetting(hero, this, "heal-multiplier", .5, true);
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
        double period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 2000, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        return getDescription().replace("$1", heal * 100 + "").replace("$2", damage * duration / period + "").replace("$3", duration / 1000 + "");
    }
}
