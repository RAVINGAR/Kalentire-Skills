package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;

public class SkillMagicWard extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillMagicWard(Heroes plugin) {
        super(plugin, "MagicWard");
        setDescription("Create a Magical Ward on yourself for the next $1 seconds. While active, you reduce incoming magic damage by $2%. You must be holding a Shield for this effect to work.");
        setArgumentRange(0, 0);
        setUsage("/skill magicward");
        setIdentifiers("skill magicward", "skill rayshield");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.DEBUFFING, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
        double damageReduction = SkillConfigManager.getUseSetting(hero, this, "damage-reduction", 0.2, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDamageReduction = Util.decFormat.format(damageReduction * 100);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedDamageReduction);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("damage-reduction", 0.2);
        node.set(SkillSetting.DURATION.node(), 4000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is being protected by a Magic Ward!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s Magic Ward has faded");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is being protected by a Magic Ward!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s Magic Ward has faded").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        switch (NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType()) {
            case IRON_DOOR:
            //FIXME Flattening
//            case WOOD_DOOR:
//            case TRAP_DOOR:
            case SHIELD:
                broadcastExecuteText(hero);

                int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
                double damageReduction = SkillConfigManager.getUseSetting(hero, this, "damage-reduction", 0.2, false);

                hero.addEffect(new MagicWardEffect(this, player, duration, damageReduction));

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.8F, 1.0F);

                return SkillResult.NORMAL;
        }

        switch (NMSHandler.getInterface().getItemInOffHand(player.getInventory()).getType()) {
            case IRON_DOOR:
            //FIXME Flattening
//            case WOOD_DOOR:
//            case TRAP_DOOR:
            case SHIELD:
                broadcastExecuteText(hero);

                int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
                double damageReduction = SkillConfigManager.getUseSetting(hero, this, "damage-reduction", 0.2, false);

                hero.addEffect(new MagicWardEffect(this, player, duration, damageReduction));

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.8F, 1.0F);

                return SkillResult.NORMAL;
        }

        player.sendMessage("You must have a shield equipped to use this skill");
        return SkillResult.FAIL;
    }

    public class SkillHeroListener implements Listener {

        @EventHandler
        public void onSkillDamage(SkillDamageEvent event) {

            if (!(event.getEntity() instanceof Player))
                return;

            Skill skill = event.getSkill();
            if (skill.isType(SkillType.ABILITY_PROPERTY_PHYSICAL))
                return;

            Hero defenderHero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            Player defenderPlayer = defenderHero.getPlayer();
            if (!defenderHero.hasEffect("MagicWard"))
                return;

            switch (NMSHandler.getInterface().getItemInMainHand(defenderPlayer.getInventory()).getType()) {
                case IRON_DOOR:
                //FIXME Flattening
//                case WOOD_DOOR:
//                case TRAP_DOOR:
                case SHIELD:
                    double damageReduction = 1.0 - ((MagicWardEffect) defenderHero.getEffect("MagicWard")).damageReduction;
                    event.setDamage((event.getDamage() * damageReduction));
                    return;
            }

            switch (NMSHandler.getInterface().getItemInOffHand(defenderPlayer.getInventory()).getType()) {
                case IRON_DOOR:
                //FIXME Flattening
//                case WOOD_DOOR:
//                case TRAP_DOOR:
                case SHIELD:
                    double damageReduction = 1.0 - ((MagicWardEffect) defenderHero.getEffect("MagicWard")).damageReduction;
                    event.setDamage((event.getDamage() * damageReduction));
            }
        }
    }

    public class MagicWardEffect extends ExpirableEffect {

        private final double damageReduction;

        public MagicWardEffect(Skill skill, Player applier, long duration, double damageReduction) {
            super(skill, "MagicWard", applier, duration, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);
            types.add(EffectType.BENEFICIAL);

            this.damageReduction = damageReduction;
        }

        public double getDamageReduction() {
            return damageReduction;
        }
    }
}
