package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import org.bukkit.Bukkit;
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

public class SkillToxicblade extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillToxicblade(Heroes plugin) {
        super(plugin, "Toxicblade");
        setDescription("Your blade which will deal an extra $1 toxic damage every $2 seconds.");
        setUsage("/skill toxicblade");
        setArgumentRange(0, 0);
        setIdentifiers("skill tblade", "skill toxicblade");
        setTypes(SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("weapons", Util.swords);
        node.set("buff-duration", 600000); // 10 minutes in milliseconds
        node.set("poison-duration", 10000); // 10 seconds in milliseconds
        node.set(SkillSetting.PERIOD.node(), 2000); // 2 seconds in milliseconds
        node.set(SkillSetting.DAMAGE_TICK.node(), 2);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0);
        node.set("attacks", 1); // How many attacks the buff lasts for.
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% has become toxic!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has recovered from the toxicity!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% has become toxic!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has recovered from the toxicity!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        long duration = SkillConfigManager.getUseSetting(hero, this, "buff-duration", 600000, false);
        int numAttacks = SkillConfigManager.getUseSetting(hero, this, "attacks", 1, false);
        hero.addEffect(new ToxicbladeBuff(this, hero.getPlayer(), duration, numAttacks));
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class ToxicbladeBuff extends ExpirableEffect {

        protected int applicationsLeft = 1;

        public ToxicbladeBuff(Skill skill, Player applier, long duration, int numAttacks) {
            super(skill, "Toxicblade", applier, duration);
            this.applicationsLeft = numAttacks;
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.POISON);
        }

        public int getApplicationsLeft() {
            return applicationsLeft;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            hero.getPlayer().sendMessage("Your blade is no longer toxified!");
        }

        public void setApplicationsLeft(int applicationsLeft) {
            this.applicationsLeft = applicationsLeft;
        }
    }

    public class ToxicbladePoison extends PeriodicDamageEffect {

        public ToxicbladePoison(Skill skill, long period, long duration, double tickDamage, Player applier) {
            super(skill, "ToxicbladePoison", applier, period, duration, tickDamage);
            this.types.add(EffectType.POISON);
            addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (duration / 1000) * 20, 0), true);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + applyText, CustomNameManager.getName(monster));
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, CustomNameManager.getName(monster));
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }
    }

    public class SkillDamageListener implements Listener {

        private final Skill skill;

        public SkillDamageListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (!(event.getEntity() instanceof LivingEntity) || !(event.getDamager() instanceof Hero) || event.getDamage() == 0 || event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                return;  // With this, we know a Hero damaged some other sort of LivingEntity with damage that counts as ENTITY_ATTACK (i.e. not a Bow or a Skill)
            }

            Hero hero = (Hero) event.getDamager();
            Player player = hero.getPlayer();
            if (!hero.hasEffect("Toxicblade") || !SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.swords).contains(NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType().name())) {
                return; // With this, we know this Hero has the ToxicBlade Effect and is holding a suitable weapon.
            }

            long duration = SkillConfigManager.getUseSetting(hero, skill, "poison-duration", 10000, false);
            long period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 2000, false);
            int tickDamage = SkillConfigManager.getUseSetting(hero, skill, "tick-damage", 20, false);
            tickDamage += (SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE, 0, false) * hero.getHeroLevel(skill));

            ToxicbladePoison apEffect = new ToxicbladePoison(skill, period, duration, tickDamage, player);
            CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            character.addEffect(apEffect);

            checkBuff(hero);
        }

        private void checkBuff(Hero hero) {
        	ToxicbladeBuff abBuff = (ToxicbladeBuff) hero.getEffect("Toxicblade");
            abBuff.applicationsLeft -= 1;
            if (abBuff.applicationsLeft < 1) {
                hero.removeEffect(abBuff);
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20, false);
        damage += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0, false) * hero.getHeroLevel(this));
        double seconds = SkillConfigManager.getUseSetting(hero, this, "poison-duration", 10000, false) / 1000.0;
        String s = getDescription().replace("$1", damage + "").replace("$2", seconds + "");
        return s;
    }
}
