package com.herocraftonline.heroes.characters.skill.reborn.archer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.effects.common.ImbueEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class SkillFireArrow extends ActiveSkill {

    public VisualEffect fplayer = new VisualEffect();

    public SkillFireArrow(Heroes plugin) {
        super(plugin, "FireArrow");
        this.setDescription("Your next arrow will light the target on fire");
        this.setUsage("/skill firearrow");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill firearrow", "skill farrow");
        this.setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("mana-per-shot", 15); // mana per shot
        node.set("fire-ticks", 100);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        if (hero.hasEffect("FireArrowBuff")) {
            hero.removeEffect(hero.getEffect("FireArrowBuff"));
            return SkillResult.SKIP_POST_USAGE;
        }
        hero.addEffect(new FireArrowBuff(this));
        this.broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class FireArrowBuff extends ImbueEffect {

        public FireArrowBuff(Skill skill) {
            super(skill, "FireArrowBuff");
            this.types.add(EffectType.FIRE);
            this.setDescription("fire");
        }
    }

    public class SkillEntityListener implements Listener {

        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler()
        public void onEntityShoot(EntityShootBowEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
                return;
            }
            final Hero hero = SkillFireArrow.this.plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("FireArrowBuff")) {
                event.getProjectile().setFireTicks(100);
            }
        }

        @EventHandler()
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            final Entity projectile = ((EntityDamageByEntityEvent) event).getDamager();
            if (!(projectile instanceof Arrow) || !(((Projectile) projectile).getShooter() instanceof Player)) {
                return;
            }

            final Player player = (Player) ((Projectile) projectile).getShooter();
            final Hero hero = SkillFireArrow.this.plugin.getCharacterManager().getHero(player);
            if (!hero.hasEffect("FireArrowBuff")) {
                return;
            }

            final LivingEntity entity = (LivingEntity) event.getEntity();
            if (!damageCheck(player, entity)) {
                event.setCancelled(true);
                return;
            }
            //Get the duration of the fire damage
            final int fireTicks = SkillConfigManager.getUseSetting(hero, this.skill, "fire-ticks", 100, false);
            //Light the target on fire
            entity.setFireTicks(entity.getFireTicks() + fireTicks);
            //Add our combust effect so we can track fire-tick damage
            SkillFireArrow.this.plugin.getCharacterManager().getCharacter(entity).addEffect(new CombustEffect(this.skill, player));

            final LivingEntity target = (LivingEntity) event.getEntity();
            addSpellTarget(target, hero);
            fplayer.playFirework(target.getWorld(), target.getLocation().add(0,2.0,0),
                    FireworkEffect.builder().flicker(false).trail(false)
                            .with(FireworkEffect.Type.BURST)
                            .withColor(Color.ORANGE)
                            .withFade(Color.RED)
                            .build());
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityShootBow(EntityShootBowEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)) {
                return;
            }
            final Hero hero = SkillFireArrow.this.plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("FireArrowBuff")) {
                    hero.removeEffect(hero.getEffect("FireArrowBuff"));
                }

        }
    }

    @Override
    public String getDescription(Hero hero) {
        final int mana = SkillConfigManager.getUseSetting(hero, this, "mana", 15, false);
        return this.getDescription().replace("$1", mana + "");
    }
}
