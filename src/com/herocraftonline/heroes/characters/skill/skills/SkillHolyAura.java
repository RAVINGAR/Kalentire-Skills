package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Util;

public class SkillHolyAura extends ActiveSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    private String applyText;
    private String expireText;

    public SkillHolyAura(Heroes plugin) {
        super(plugin, "HolyAura");
        setDescription("You begin to radiate with a Holy Aura, healing all allies within $1 blocks (other than yourself) for $2 health every $3 seconds. Your aura dissipates after $4 seconds. Any undead targets within your Holy Aura will also be dealt $5 damage.");
        setUsage("/skill holyaura");
        setArgumentRange(0, 0);
        setTypes(SkillType.LIGHT, SkillType.SILENCABLE, SkillType.HEAL, SkillType.BUFF);
        setIdentifiers("skill holyaura");
    }

    @Override
    public String getDescription(Hero hero) {

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(4), false);
        double duration = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Double.valueOf(15000), false) / 1000.0);
        double period = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, Integer.valueOf(5000), false) / 1000.0);
        double tickHeal = SkillConfigManager.getUseSetting(hero, this, "tick-heal", Double.valueOf(50), false);
        double undeadDamage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", Double.valueOf(25), false);

        return getDescription().replace("$1", radius + "").replace("$2", tickHeal + "").replace("$3", period + "").replace("$4", duration + "").replace("$5", undeadDamage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), Integer.valueOf(15000));
        node.set(SkillSetting.PERIOD.node(), Integer.valueOf(5000));
        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(4));
        node.set("tick-heal", Double.valueOf(50));
        node.set("undead-damage", Double.valueOf(30));

        node.set(SkillSetting.APPLY_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %target% begins to radiate a holy aura!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %target% is no longer holy!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %target% begins to radiate a holy aura!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %target% is no longer holy!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(15000), false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, Integer.valueOf(5000), false);
        double tickheal = SkillConfigManager.getUseSetting(hero, this, "tick-heal", Double.valueOf(50), false);
        double undeadDamage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", Double.valueOf(30), false);

        hero.addEffect(new HolyAuraEffect(this, duration, period, tickheal, undeadDamage));

        broadcastExecuteText(hero);

        try {
            fplayer.playFirework(player.getWorld(), player.getLocation().add(0, 1.5, 0), FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.BALL).withColor(Color.YELLOW).withFade(Color.SILVER).build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
    }

    public class HolyAuraEffect extends PeriodicExpirableEffect {

        double tickHeal;
        double undeadDamage;

        public HolyAuraEffect(Skill skill, long duration, long period, double tickHeal, double undeadDamage) {
            super(skill, "HolyAuraEffect", period, duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HEAL);
            types.add(EffectType.LIGHT);

            this.tickHeal = tickHeal;
            this.undeadDamage = undeadDamage;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

        @Override
        public void tickHero(Hero hero) {
            healNerby(hero);
        }

        private void healNerby(Hero hero) {

            Player player = hero.getPlayer();

            int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 4, false);
            int radiusSquared = radius * radius;

            // Check if the hero has a party
            if (hero.hasParty()) {
                Location playerLocation = player.getLocation();
                // Loop through the player's party members and heal as necessary
                for (Hero member : hero.getParty().getMembers()) {
                    if (member.equals(hero))        // Skip the player
                        continue;

                    Location memberLocation = member.getPlayer().getLocation();

                    // Ensure the party member is in the same world.
                    if (memberLocation.getWorld().equals(playerLocation.getWorld())) {

                        if (memberLocation.distanceSquared(playerLocation) <= radiusSquared) {
                            // Check to see if they are close enough to the player to receive healing

                            HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(member, tickHeal, skill, hero);
                            Bukkit.getPluginManager().callEvent(healEvent);
                            if (!healEvent.isCancelled()) {
                                member.heal(healEvent.getAmount());
                            }
                        }
                    }
                }
            }

            // Damage nearby undead
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (!(entity instanceof LivingEntity) || (entity instanceof Player)) {
                    continue;
                }

                LivingEntity lETarget = (LivingEntity) entity;
                if (!(isUndead(lETarget)))
                    continue;

                // Damage for 50% of heal value
                addSpellTarget(lETarget, hero);
                Skill.damageEntity(lETarget, player, undeadDamage, DamageCause.MAGIC);
            }
        }

        @Override
        public void tickMonster(Monster monster) {

        }
    }

    private boolean isUndead(Entity entity) {
        return entity instanceof Zombie || entity instanceof Skeleton || entity instanceof PigZombie || entity instanceof Ghast;
    }
}
