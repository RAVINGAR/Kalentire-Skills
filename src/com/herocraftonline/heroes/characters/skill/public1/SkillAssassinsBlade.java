package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillAssassinsBlade extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillAssassinsBlade(Heroes plugin) {
        super(plugin, "AssassinsBlade");
        this.setDescription("You poison your blade which will deal an extra $1 damage every $2 second(s).");
        this.setUsage("/skill ablade");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill ablade", "skill assassinsblade");
        this.setTypes(SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("weapons", Util.swords);
        node.set("buff-duration", 600000); // 10 minutes in milliseconds
        node.set("poison-duration", 10000); // 10 seconds in milliseconds
        node.set(SkillSetting.PERIOD.node(), 2000); // 2 seconds in milliseconds
        node.set(SkillSetting.DAMAGE_TICK.node(), 2);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0);
        node.set("attacks", 1); // How many attacks the buff lasts for.
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% is poisoned!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has recovered from the poison!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% is poisoned!").replace("%target%", "$1");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has recovered from the poison!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final long duration = SkillConfigManager.getUseSetting(hero, this, "buff-duration", 600000, false);
        final int numAttacks = SkillConfigManager.getUseSetting(hero, this, "attacks", 1, false);
        hero.addEffect(new AssassinBladeBuff(this, hero.getPlayer(), duration, numAttacks));
        this.broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class AssassinBladeBuff extends ExpirableEffect {

        private int applicationsLeft = 1;

        public AssassinBladeBuff(Skill skill, Player applier, long duration, int numAttacks) {
            super(skill, "PoisonBlade", applier, duration);
            this.applicationsLeft = numAttacks;
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.POISON);
        }

        /**
         * @return the applicationsLeft
         */
        public int getApplicationsLeft() {
            return this.applicationsLeft;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            hero.getPlayer().sendMessage(ChatColor.GRAY + "Your blade is no longer poisoned!");
        }

        /**
         * @param applicationsLeft
         *            the applicationsLeft to set
         */
        public void setApplicationsLeft(int applicationsLeft) {
            this.applicationsLeft = applicationsLeft;
        }
    }

    public class AssassinsPoison extends PeriodicDamageEffect {

        public AssassinsPoison(Skill skill, long period, long duration, double tickDamage, Player applier) {
            super(skill, "AssassinsPoison", applier, period, duration, tickDamage);
            this.types.add(EffectType.POISON);
            this.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (20 * duration / 1000), 0), true);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            this.broadcast(monster.getEntity().getLocation(), SkillAssassinsBlade.this.applyText, CustomNameManager.getName(monster).toLowerCase());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), SkillAssassinsBlade.this.applyText, player.getDisplayName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            this.broadcast(monster.getEntity().getLocation(), SkillAssassinsBlade.this.expireText, CustomNameManager.getName(monster).toLowerCase());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), SkillAssassinsBlade.this.expireText, player.getDisplayName());
        }
    }

    public class SkillDamageListener implements Listener {

        private final Skill skill;

        public SkillDamageListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || (event.getDamage() == 0)) {
                return;
            }

            // If our target isn't a creature or player lets exit
            if (!(event.getEntity() instanceof LivingEntity) && !(event.getEntity() instanceof Player)) {
                return;
            }

            final EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            if (!(subEvent.getDamager() instanceof Player)) {
                return;
            }

            final Player player = (Player) subEvent.getDamager();
            final ItemStack item = NMSHandler.getInterface().getItemInMainHand(player.getInventory());
            final Hero hero = SkillAssassinsBlade.this.plugin.getCharacterManager().getHero(player);
            if (!SkillConfigManager.getUseSetting(hero, this.skill, "weapons", Util.swords).contains(item.getType().name())) {
                return;
            }

            if (hero.hasEffect("PoisonBlade")) {
                final long duration = SkillConfigManager.getUseSetting(hero, this.skill, "poison-duration", 10000, false);
                final long period = SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.PERIOD, 2000, false);
                double tickDamage = SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.DAMAGE_TICK, 2, false);
                tickDamage += (SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.DAMAGE_INCREASE, 0, false) * hero.getHeroLevel(this.skill));
                final AssassinsPoison apEffect = new AssassinsPoison(this.skill, period, duration, tickDamage, player);
                final Entity target = event.getEntity();
                if (event.getEntity() instanceof LivingEntity) {
                    final CharacterTemplate character = SkillAssassinsBlade.this.plugin.getCharacterManager().getCharacter((LivingEntity) target);
                    character.addEffect(apEffect);
                    this.checkBuff(hero);
                }
            }
        }

        private void checkBuff(Hero hero) {
            final AssassinBladeBuff abBuff = (AssassinBladeBuff) hero.getEffect("PoisonBlade");
            abBuff.applicationsLeft -= 1;
            if (abBuff.applicationsLeft < 1) {
                hero.removeEffect(abBuff);
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 2, false);
        damage += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0, false) * hero.getHeroLevel(this));
        final double seconds = SkillConfigManager.getUseSetting(hero, this, "poison-duration", 10000, false) / 1000.0;
        final String s = this.getDescription().replace("$1", damage + "").replace("$2", seconds + "");
        return s;
    }
}
