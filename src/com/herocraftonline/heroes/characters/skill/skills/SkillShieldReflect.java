package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillShieldReflect extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillShieldReflect(Heroes plugin) {
        super(plugin, "ShieldReflect");
        setDescription("Your shield reflects damage taken for a short time");
        setUsage("/skill shieldreflect");
        setArgumentRange(0, 0);
        setIdentifiers("skill shieldreflect", "skill shieldref", "skill shieldr", "skill sreflect", "skill sref");
        setTypes(SkillType.FORCE, SkillType.SILENCABLE, SkillType.BUFF, SkillType.PHYSICAL);
        Bukkit.getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), Integer.valueOf(3000));
        node.set("reflected-amount", Integer.valueOf(2));
        node.set(Setting.APPLY_TEXT.node(), "%hero% holds up their shield and is now reflecting incoming attacks!");
        node.set(Setting.EXPIRE_TEXT.node(), "%hero% is no longer reflecting attacks!");
        return node;
    }

    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%hero% holds up their shield and is now reflecting incoming attacks!").replace("%hero%", "$1");
        this.expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%hero% is no longer reflecting attacks!").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 3000, false);
        Player player = hero.getPlayer();

        switch (player.getItemInHand().getType()) {
        case IRON_DOOR:
        case WOOD_DOOR:
        case TRAP_DOOR:
            broadcastExecuteText(hero);
            hero.addEffect(new ShieldReflectEffect(this, duration));
            return SkillResult.NORMAL;
        default:
            Messaging.send(player, "You must have a shield equiped to use this skill");
            return SkillResult.FAIL;
        }
    }

    public class SkillEntityListener implements Listener {
        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler()
        public void onEntityDamage(EntityDamageEvent event) {
            if ((event.isCancelled()) || (!(event instanceof EntityDamageByEntityEvent))) {
                return;
            }

            EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent)event;
            Entity defender = edbe.getEntity();
            Entity attacker = edbe.getDamager();
            if (((attacker instanceof LivingEntity)) && ((defender instanceof Player))) {
                Player defPlayer = (Player)defender;
                Hero hero = SkillShieldReflect.this.plugin.getCharacterManager().getHero(defPlayer);
                if (hero.hasEffect("ShieldReflect")) {
                    if ((attacker instanceof Player)) {
                        Player attPlayer = (Player) attacker;
                        if (SkillShieldReflect.this.plugin.getCharacterManager().getHero(attPlayer).hasEffect(SkillShieldReflect.this.getName())) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                    int damage = event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "reflected-amount", 2, false);
                    SkillShieldReflect.this.plugin.getDamageManager().addSpellTarget(attacker, hero, this.skill);
                    Skill.damageEntity((LivingEntity) attacker, (LivingEntity) defender, damage, DamageCause.MAGIC);
                }
            }
        }
    }

    public class ShieldReflectEffect extends ExpirableEffect {
        public ShieldReflectEffect(Skill skill, long duration) {
            super(skill, "ShieldReflect", duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), SkillShieldReflect.this.applyText, new Object[] { player.getDisplayName() });
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), SkillShieldReflect.this.expireText, new Object[] { player.getDisplayName() });
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}