package com.herocraftonline.heroes.characters.skill.general;

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
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SkillMagicWard extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillMagicWard(final Heroes plugin) {
        super(plugin, "MagicWard");
        setDescription("Create a Magical Ward on yourself for the next $1 second(s). While active, you reduce incoming magic damage by $2%. You must be holding a Shield for this effect to work.");
        setArgumentRange(0, 0);
        setUsage("/skill magicward");
        setIdentifiers("skill magicward", "skill rayshield");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.DEBUFFING, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
        final double damageReduction = SkillConfigManager.getUseSetting(hero, this, "damage-reduction", 0.2, false);

        final String formattedDuration = Util.decFormat.format(duration / 1000.0);
        final String formattedDamageReduction = Util.decFormat.format(damageReduction * 100);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedDamageReduction);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set("damage-reduction", 0.2);
        node.set(SkillSetting.DURATION.node(), 4000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is being protected by a Magic Ward!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s Magic Ward has faded");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is being protected by a Magic Ward!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s Magic Ward has faded").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        switch (NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType()) {
            case IRON_DOOR:
                //FIXME Flattening
//            case WOOD_DOOR:
//            case TRAP_DOOR:
            case SHIELD:
                broadcastExecuteText(hero);

                final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
                final double damageReduction = SkillConfigManager.getUseSetting(hero, this, "damage-reduction", 0.2, false);

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

                final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
                final double damageReduction = SkillConfigManager.getUseSetting(hero, this, "damage-reduction", 0.2, false);

                hero.addEffect(new MagicWardEffect(this, player, duration, damageReduction));

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.8F, 1.0F);

                return SkillResult.NORMAL;
        }

        player.sendMessage("You must have a shield equipped to use this skill");
        return SkillResult.FAIL;
    }

    public class SkillHeroListener implements Listener {

        @EventHandler
        public void onSkillDamage(final SkillDamageEvent event) {

            if (!(event.getEntity() instanceof Player)) {
                return;
            }

            final Skill skill = event.getSkill();
            if (skill.isType(SkillType.ABILITY_PROPERTY_PHYSICAL)) {
                return;
            }

            final Hero defenderHero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            final Player defenderPlayer = defenderHero.getPlayer();
            if (!defenderHero.hasEffect("MagicWard")) {
                return;
            }

            switch (NMSHandler.getInterface().getItemInMainHand(defenderPlayer.getInventory()).getType()) {
                case IRON_DOOR:
                    //FIXME Flattening
//                case WOOD_DOOR:
//                case TRAP_DOOR:
                case SHIELD:
                    final double damageReduction = 1.0 - ((MagicWardEffect) defenderHero.getEffect("MagicWard")).damageReduction;
                    event.setDamage((event.getDamage() * damageReduction));
                    return;
            }

            switch (NMSHandler.getInterface().getItemInOffHand(defenderPlayer.getInventory()).getType()) {
                case IRON_DOOR:
                    //FIXME Flattening
//                case WOOD_DOOR:
//                case TRAP_DOOR:
                case SHIELD:
                    final double damageReduction = 1.0 - ((MagicWardEffect) defenderHero.getEffect("MagicWard")).damageReduction;
                    event.setDamage((event.getDamage() * damageReduction));
            }
        }
    }

    public class MagicWardEffect extends ExpirableEffect {

        private final double damageReduction;

        public MagicWardEffect(final Skill skill, final Player applier, final long duration, final double damageReduction) {
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
