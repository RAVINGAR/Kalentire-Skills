package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;

import pgDev.bukkit.DisguiseCraft.DisguiseCraft;
import pgDev.bukkit.DisguiseCraft.api.DisguiseCraftAPI;
import pgDev.bukkit.DisguiseCraft.disguise.Disguise;
import pgDev.bukkit.DisguiseCraft.disguise.DisguiseType;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillBecomeDeath extends ActiveSkill {

    DisguiseCraftAPI dcAPI;
    private String applyText;
    private String expireText;

    public SkillBecomeDeath(Heroes plugin) {
        super(plugin, "BecomeDeath");
        setDescription("For $1 seconds you look undead. While shrouded in death, you no longer need to breath air.");
        setUsage("/skill becomedeath");
        setArgumentRange(0, 0);
        setIdentifiers("skill becomedeath");
        setTypes(SkillType.SILENCABLE, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_DARK, SkillType.FORM_ALTERING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);

        if (Bukkit.getServer().getPluginManager().getPlugin("DisguiseCraft") != null) {
            dcAPI = DisguiseCraft.getAPI();
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 120000, false);

        return getDescription().replace("$1", duration / 1000 + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.USE_TEXT.node(), "");
        node.set(SkillSetting.DURATION.node(), 120000);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% shrouds themself in death!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% no longer appears as an undead!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% shrouds themself in death!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% no longer appears as an undead!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 120000, false);
        hero.addEffect(new BecomeDeathEffect(this, player, duration));

        player.getWorld().playSound(player.getLocation(), Sound.CAT_MEOW, 1.0F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityTarget(EntityTargetEvent event) {
            if (!(event.getEntity() instanceof LivingEntity) || !(event.getTarget() instanceof Player)) {
                return;
            }

            if (!(Util.isUndead(plugin, (LivingEntity) event.getEntity()))) {
                return;
            }

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getTarget());
            if (hero.hasEffect("BecomeDeath")) {
                BecomeDeathEffect bdEffect = (BecomeDeathEffect) hero.getEffect("BecomeDeath");
                if (!bdEffect.hasAttackedMobs())
                    event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || event.getDamage() == 0 || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            if (!Util.isUndead(plugin, (LivingEntity) event.getEntity()) || !(event instanceof EntityDamageByEntityEvent)) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            if (subEvent.getDamager() instanceof Player) {
                Hero hero = plugin.getCharacterManager().getHero((Player) subEvent.getDamager());
                if (hero.hasEffect("BecomeDeath")) {
                    BecomeDeathEffect bdEffect = (BecomeDeathEffect) hero.getEffect("BecomeDeath");
                    bdEffect.setAttackedMobs(true);
                }
            }
            else if (subEvent.getDamager() instanceof Projectile) {
                if (((Projectile) subEvent.getDamager()).getShooter() instanceof Player) {
                    Hero hero = plugin.getCharacterManager().getHero((Player) ((Projectile) subEvent.getDamager()).getShooter());
                    if (hero.hasEffect("BecomeDeath")) {
                        BecomeDeathEffect bdEffect = (BecomeDeathEffect) hero.getEffect("BecomeDeath");
                        bdEffect.setAttackedMobs(true);
                    }
                }
            }
        }
    }

    public class BecomeDeathEffect extends ExpirableEffect {
        private boolean attackedMobs = false;

        public BecomeDeathEffect(Skill skill, Player applier, long duration) {
            super(skill, "BecomeDeath", applier, duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DARK);
            types.add(EffectType.MAGIC);
            types.add(EffectType.WATER_BREATHING);

            addMobEffect(13, (int) (duration / 1000) * 20, 0, false);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            if (dcAPI != null) {
                if (dcAPI.isDisguised(player)) {
                    Disguise disguise = dcAPI.getDisguise(player).clone();
                    disguise.setType(DisguiseType.Zombie);
                    dcAPI.changePlayerDisguise(player, disguise);
                }
                else {
                    Disguise disguise = new Disguise(dcAPI.newEntityID(), DisguiseType.Zombie);
                    dcAPI.disguisePlayer(player, disguise);
                }
            }

            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();

            if (dcAPI != null) {
                if (dcAPI.isDisguised(player))
                    dcAPI.undisguisePlayer(player);
            }

            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

        public boolean hasAttackedMobs() {
            return attackedMobs;
        }

        public void setAttackedMobs(boolean attackedMobs) {
            this.attackedMobs = attackedMobs;
        }
    }
}
