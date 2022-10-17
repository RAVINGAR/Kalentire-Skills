package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SummonEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.lumine.mythic.api.mobs.GenericCaster;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.adapters.BukkitPlayer;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillSeeingEyes extends ActiveSkill {
    private String expireText;

    public SkillSeeingEyes(Heroes paramHeroes)
    {
        super(paramHeroes, "SeeingEyes");
        setDescription("Summons a pair of spirits to track down your enemies.");
        setUsage("/skill seeingeyes");
        setArgumentRange(0, 0);
        setIdentifiers("skill seeingeyes");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.SUMMONING, SkillType.SILENCEABLE);
        //listener = new MinionListener(this); //See SkillSummonAssist
    }

    @Override
    public String getDescription(Hero arg0) {
        return super.getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.DURATION.node(), 60000);
        node.set("mythic-mob-type", "VoidSpirit");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "The creature returns to it's hellish domain.");
        node.set("max-summons", 4);

        return node;
    }

    public void init()
    {
        super.init();
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "The spirits return to the wailing sea..");
    }

    public SkillResult use(Hero paramHero, String[] paramArrayOfString)
    {
        Player localPlayer = paramHero.getPlayer();
        if (paramHero.getSummons().size() < SkillConfigManager.getUseSetting(paramHero, this, "max-summons", 4, false))
        {
            Location localLocation = localPlayer.getLocation();
            try {
                summon(paramHero, localLocation.add(0.5,1,0));
                summon(paramHero, localLocation.add(0,1,0.5));
                localLocation.getWorld().playSound(localLocation, Sound.ENTITY_WITHER_SPAWN, 0.8F, 0.9F);
                broadcastExecuteText(paramHero);
                localPlayer.sendMessage(ChatComponents.GENERIC_SKILL + "Ancient spirits arise from the void.");
            }
            catch(InvalidMobTypeException e) {
                return SkillResult.FAIL;
            }
            return SkillResult.NORMAL;
        }
        localPlayer.sendMessage(ChatColor.RED + "You can't summon any more spirits!");
        return SkillResult.FAIL;
    }

    private void summon(Hero paramHero, Location localLocation) throws InvalidMobTypeException {
        localLocation.getWorld().spawnParticle(Particle.DRAGON_BREATH, localLocation.add(0, 0.5, 0), 40, 1, 1, 1, 0.5);
        localLocation.getWorld().spawnParticle(Particle.CLOUD, localLocation.add(0, 0, 0), 10, 1, 1, 1, 0.5);
        LivingEntity summon = (LivingEntity) MythicBukkit.inst().getAPIHelper().spawnMythicMob(SkillConfigManager.getUseSetting(paramHero, this, "mythic-mob-type", "VoidSpirit"), localLocation);
        ActiveMob mob = MythicBukkit.inst().getMobManager().getActiveMob(summon.getUniqueId()).get();
        mob.setParent(new GenericCaster(new BukkitPlayer(paramHero.getPlayer())));
        mob.setLevel(paramHero.getHeroLevel(this));
        CharacterTemplate localCreature = plugin.getCharacterManager().getCharacter(summon);
        long l = SkillConfigManager.getUseSetting(paramHero, this, SkillSetting.DURATION, 60000, false);
        localCreature.addEffect(new SummonEffect(this, l, paramHero, this.expireText));
    }
}
