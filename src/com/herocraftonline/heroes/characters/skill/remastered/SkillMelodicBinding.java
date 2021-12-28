package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Note;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Song;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.List;

public class SkillMelodicBinding extends ActiveSkill {

    private String applyText;
    private String expireText;

    private Song skillSong;

    public SkillMelodicBinding(Heroes plugin) {
        super(plugin, "MelodicBinding");
        setDescription("You resonate melodic bindings, pulsing for $1 damage and slowing enemies within $2 blocks for $3 second(s). " +
                "Your melodic bindings pulse every $4 seconds for the next $5 second(s).");
        setUsage("/skill melodicbinding");
        setArgumentRange(0, 0);
        setIdentifiers("skill melodicbinding");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.MOVEMENT_SLOWING, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_SONG, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);

        skillSong = new Song(
                new Note(Sound.BLOCK_NOTE_BLOCK_HARP, 0.8F, 6.0F, 0),
                new Note(Sound.BLOCK_NOTE_BLOCK_HARP, 0.8F, 2.0F, 1),
                new Note(Sound.BLOCK_NOTE_BLOCK_HARP, 0.8F, 8.0F, 2),
                new Note(Sound.BLOCK_NOTE_BLOCK_HARP, 0.8F, 3.0F, 3)
        );
    }

    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, "melodic-buff-duration", 3000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, "melodic-buff-period", 1500, false);

        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 6.0, false);
        int slowDuration = SkillConfigManager.getUseSetting(hero, this, "melodic-slow-duration", 1500, false);

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, 17, false);

        String formattedPeriod = Util.decFormat.format(period / 1000.0);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedSlowDuration = Util.decFormat.format(slowDuration / 1000.0);
        String formattedDamage = Util.decFormat.format(damage);

        return getDescription()
                .replace("$1", formattedDamage)
                .replace("$2", Util.decFormat.format(radius))
                .replace("$3", formattedSlowDuration)
                .replace("$4", formattedPeriod)
                .replace("$5", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.USE_TEXT.node(), "");
        config.set("melodic-buff-duration", 3000);
        config.set("melodic-buff-period", 1500);
        config.set(SkillSetting.RADIUS.node(), 6.0);
        config.set(SkillSetting.DAMAGE.node(), 17.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_CHARISMA.node(), 0.0);
        config.set("melodic-slow-duration", 1500);
        config.set("slow-amplifier", 1);
        config.set("slow-amplifier-per-level", 0.0);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% releases Melodic Bindings!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer binding enemies.");
        config.set(SkillSetting.DELAY.node(), 1000);
        config.set(SkillSetting.COOLDOWN.node(), 1000);
        config.set("max-targets", 5);
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {

        Player player = hero.getPlayer();
        broadcastExecuteText(hero);

        hero.addEffect(new SoundEffect(this, "MelodicBindingSong", 100, skillSong));

        int duration = SkillConfigManager.getUseSetting(hero, this, "melodic-buff-duration", 3000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, "melodic-buff-period", 1500, false);
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 6.0, false);

        hero.addEffect(new MelodicBindingEffect(this, hero.getPlayer(), period, duration, radius));

        //FIXME Is it a particle or a sound
//        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
//        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
//        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);

        return SkillResult.NORMAL;
    }

    public class MelodicBindingEffect extends PeriodicExpirableEffect {

        private final double radius;

        public MelodicBindingEffect(SkillMelodicBinding skill, Player applier, int period, int duration, double radius) {
            super(skill, "MelodicBinding", applier, period, duration, applyText, expireText);

            types.add(EffectType.BENEFICIAL);

            this.radius = radius;
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();

            for (double r = 1; r < radius; r++)
            {
                List<Location> particleLocations = GeometryUtil.circle(player.getLocation(), 36, r);
                for (int i = 0; i < particleLocations.size(); i++)
                {
                    //player.getWorld().spigot().playEffect(particleLocations.get(i).add(0, 0.1, 0), Effect.NOTE, 0, 0, 0, 0.1F, 0, 0.0F, 1, 16);
                    player.getWorld().spawnParticle(Particle.NOTE, particleLocations.get(i), 1, 0, 0.1, 0, 0);
                }
            }

            int slowAmount = SkillConfigManager.getScaledUseSettingInt(hero, skill, "slow-amplifier", 1, false);
            int slowDuration = SkillConfigManager.getUseSetting(hero, skill, "melodic-slow-duration", 1500, false);

            double damage = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.DAMAGE, 17.0, false);

            // An example of not limiting if damage is 0. Since this isn't the case on live, it makes for a good example.
            int maxTargets = damage > 0 ? SkillConfigManager.getUseSetting(hero, skill, "max-targets", 0, false) : 0;
            int targetsHit = 0;
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                // Check to see if we've exceeded the max targets
                if (maxTargets > 0 && targetsHit >= maxTargets) {
                    break;
                }

                if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity)) {
                    continue;
                }

                CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter((LivingEntity) entity);

                if (damage > 0) {
                    addSpellTarget(entity, hero);
                    damageEntity((LivingEntity) entity, player, damage, DamageCause.MAGIC, false);
                }

                SlowEffect sEffect = new SlowEffect(skill, player, slowDuration, slowAmount, null, null);
                sEffect.types.add(EffectType.DISPELLABLE);
                targetCT.addEffect(sEffect);

                targetsHit++;
            }
        }

        @Override
        public void tickMonster(Monster monster) {}
    }
}