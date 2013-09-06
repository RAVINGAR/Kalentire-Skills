package com.herocraftonline.heroes.characters.skill.unusedskills;
/*package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.effects.common.ImbueEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillFireArrow extends ActiveSkill {

    public SkillFireArrow(Heroes plugin) {
        super(plugin, "FireArrow");
        setDescription("Your arrows will light the target on fire, but they will drain $1 mana per shot!");
        setUsage("/skill firearrow");
        setArgumentRange(0, 0);
        setIdentifiers("skill firearrow", "skill farrow");
        setTypes(SkillType.FIRE, SkillType.BUFF);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("mana-per-shot", 1); // mana per shot
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
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class FireArrowBuff extends ImbueEffect {

        public FireArrowBuff(Skill skill) {
            super(skill, "FireArrowBuff");
            this.types.add(EffectType.FIRE);
            setDescription("fire");
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
            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("FireArrowBuff")) {
                event.getProjectile().setFireTicks(100);
            }
        }

        @EventHandler()
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            Entity projectile = ((EntityDamageByEntityEvent) event).getDamager();
            if (!(projectile instanceof Arrow) || !(((Projectile) projectile).getShooter() instanceof Player)) {
                return;
            }

            Player player = (Player) ((Projectile) projectile).getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.hasEffect("FireArrowBuff")) {
                return;
            }

            LivingEntity entity = (LivingEntity) event.getEntity();
            if (!damageCheck((Player) player, entity)) {
                event.setCancelled(true);
                return;
            }
            //Get the duration of the fire damage
            int fireTicks = SkillConfigManager.getUseSetting(hero, skill, "fire-ticks", 100, false);
            //Light the target on fire
            entity.setFireTicks(fireTicks);
            //Add our combust effect so we can track fire-tick damage
            plugin.getCharacterManager().getCharacter(entity).addEffect(new CombustEffect(skill, player));
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityShootBow(EntityShootBowEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)) {
                return;
            }
            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("FireArrowBuff")) {
                int mana = SkillConfigManager.getUseSetting(hero, skill, "mana-per-shot", 1, true);
                if (hero.getMana() < mana) {
                    hero.removeEffect(hero.getEffect("FireArrowBuff"));
                } else {
                    hero.setMana(hero.getMana() - mana);
                }
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int mana = SkillConfigManager.getUseSetting(hero, this, "mana-per-shot", 1, false);
        return getDescription().replace("$1", mana + "");
    }
}
*/
