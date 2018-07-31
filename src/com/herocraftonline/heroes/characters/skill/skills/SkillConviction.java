package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;

public class SkillConviction extends ActiveSkill {

	private String applyText;
    private String expireText;

    public SkillConviction(Heroes plugin) {
        super(plugin, "Conviction");
        setDescription("You reduce all damage taken by your party by $1%!");
        setArgumentRange(0, 0);
        setUsage("/skill conviction");
        setIdentifiers("skill conviction");
        setTypes(SkillType.BUFFING, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double bonus = SkillConfigManager.getUseSetting(hero, this, "damage-modifier", 0.85, false);

        String formattedDamageModifier = Util.decFormat.format((1.0 - bonus) * 100.0);

        return getDescription().replace("$1", formattedDamageModifier);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("damage-reduction", 0.85);
        node.set(SkillSetting.RADIUS.node(), 10);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "You are filled with renewed convinction!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "Your sense of convinction begins to fade!");
        node.set(SkillSetting.DURATION.node(), 180000);

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "You are filled with renewed convinction!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "Your sense of convinction begins to fade!");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 180000, false);
        double damageModifier = SkillConfigManager.getUseSetting(hero, this, "damage-modifier", 0.85, false);

        ConvictionEffect effect = new ConvictionEffect(this, player, duration, damageModifier, applyText, expireText);
        if (!hero.hasParty()) {
        	if (hero.hasEffect("Conviction")) {
                if (((ConvictionEffect) hero.getEffect("Conviction")).getDamageModifier() < effect.getDamageModifier()) {
                    hero.getPlayer().sendMessage("You have a more powerful effect already!");
                } else {
                	hero.addEffect(effect);
                }
            }
        } else {
            int range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);
            int rangeSquared = range * range;
            Location loc = player.getLocation();
            for (Hero pHero : hero.getParty().getMembers()) {
                Player pPlayer = pHero.getPlayer();
                if (!pPlayer.getWorld().equals(player.getWorld())) {
                    continue;
                }
                if (pPlayer.getLocation().distanceSquared(loc) > rangeSquared) {
                    continue;
                }
                if (pHero.hasEffect("Conviction")) {
                    if (((ConvictionEffect) pHero.getEffect("Conviction")).getDamageModifier() < effect.getDamageModifier()) {
                        continue;
                    }
                }
                pHero.addEffect(effect);
            }
        }

        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
        player.getWorld().playSound(player.getLocation(), CompatSound.BLOCK_ANVIL_LAND.value(), 0.6F, 1.0F);
        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public static class ConvictionEffect extends ExpirableEffect {

        private double damageModifier;
        private String applyText;
        private String expireText;

        public ConvictionEffect(Skill skill, Player applier, long duration, double damageModifier, String applyText, String expireText) {
            super(skill, "Conviction", applier, duration);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);

            this.damageModifier = damageModifier;
            this.applyText = applyText;
            this.expireText = expireText;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            player.sendMessage(applyText);
        }

        public double getDamageModifier() {
            return damageModifier;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            player.sendMessage(expireText);
        }
    }

    public class SkillHeroListener implements Listener {

        @EventHandler()
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getCause() != DamageCause.ENTITY_ATTACK) {
                return;
            }

            Entity entity = event.getEntity();
            if (entity instanceof LivingEntity) {
                CharacterTemplate character = SkillConviction.this.plugin.getCharacterManager().getCharacter((LivingEntity) entity);
                if (character.hasEffect("Conviction")) {
                    double damageModifier = ((ConvictionEffect) character.getEffect("Conviction")).damageModifier;
                    event.setDamage((event.getDamage() * damageModifier));
                }
            }
        }
        
        @EventHandler
        public void onSkillDamage(SkillDamageEvent event) {

            Entity entity = event.getEntity();
            if (entity instanceof LivingEntity) {
                CharacterTemplate character = SkillConviction.this.plugin.getCharacterManager().getCharacter((LivingEntity) entity);
                if (character.hasEffect("Conviction")) {
                    double damageModifier = ((ConvictionEffect) character.getEffect("Conviction")).damageModifier;
                    event.setDamage((event.getDamage() * damageModifier));
                }
            }
        }
    }
}
