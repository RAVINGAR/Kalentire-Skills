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
import com.herocraftonline.heroes.util.Util;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.lumine.mythic.api.mobs.GenericCaster;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.adapters.BukkitPlayer;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SkillConjureElemental extends ActiveSkill {
    private String expireText;
    private final List<String> types;

    public SkillConjureElemental(Heroes paramHeroes)
    {
        super(paramHeroes, "ConjureElemental");
        setDescription("Conjure a random nature elemental to fight by your side");
        setUsage("/skill conjureelemental");
        setArgumentRange(0, 0);
        setIdentifiers("skill conjureelemental");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.SUMMONING, SkillType.SILENCEABLE);

        types = new ArrayList<>();
        types.add("FireElemental:NETHERRACK");
        types.add("LightningElemental:END_STONE");
        types.add("IceElemental:ICE");
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

        node.set("mythic-mob-types", types);
        node.set(SkillSetting.EXPIRE_TEXT.node(), "The creature returns to whence it came.");
        node.set("max-summons", 3);
        node.set(SkillSetting.RADIUS.node(), 7);
        node.set(SkillSetting.RADIUS_INCREASE_PER_WISDOM.node(), 0.005);

        return node;
    }

    public void init()
    {
        super.init();
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "The creature returns to whence it came.");
    }

    public SkillResult use(Hero paramHero, String[] paramArrayOfString)
    {
        Player localPlayer = paramHero.getPlayer();
        if (paramHero.getSummons().size() < SkillConfigManager.getUseSetting(paramHero, this, "max-summons", 3, false))
        {
            int i = SkillConfigManager.getUseSetting(paramHero, this, SkillSetting.MAX_DISTANCE, 5, false);
            Location localLocation = localPlayer.getTargetBlock(null, i).getLocation().add(0,1,0);
            List<String> mobTypes = SkillConfigManager.getUseSetting(paramHero, this, "mythic-mob-types", types);
            int nextInt = Util.nextInt(mobTypes.size()-1);
            try {
                summon(paramHero, mobTypes.get(nextInt), localLocation);
                localLocation.getWorld().playSound(localLocation, Sound.WEATHER_RAIN, 0.8F, 0.9F);
                localLocation.getWorld().playSound(localLocation, Sound.ITEM_TRIDENT_RIPTIDE_3, 0.8F, 0.6F);
                broadcastExecuteText(paramHero);
                localPlayer.sendMessage(ChatComponents.GENERIC_SKILL + "You have conjured an ancient elemental!");
            }
            catch(InvalidMobTypeException e) {
                return SkillResult.FAIL;
            }
            return SkillResult.NORMAL;
        }
        localPlayer.sendMessage(ChatColor.RED + "You can't summon any more elementals!");
        return SkillResult.FAIL;
    }

    private void summon(Hero paramHero, String type, Location localLocation) throws InvalidMobTypeException {
        String[] split = type.split(":");
        localLocation.getWorld().playSound(localLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.9F, 0.9F);
        localLocation.getWorld().spawnParticle(Particle.BLOCK_CRACK, localLocation.add(0, 0.5, 0), 40, 1, 1, 1, 0.5, Bukkit.createBlockData(Material.matchMaterial(split[1])));
        localLocation.getWorld().spawnParticle(Particle.CLOUD, localLocation.add(0, 0, 0), 10, 1, 1, 1, 0.5);
        LivingEntity summon = (LivingEntity) MythicBukkit.inst().getAPIHelper().spawnMythicMob(split[0], localLocation);
        ActiveMob mob = MythicBukkit.inst().getMobManager().getActiveMob(summon.getUniqueId()).get();
        mob.setParent(new GenericCaster(new BukkitPlayer(paramHero.getPlayer())));
        mob.setLevel(paramHero.getHeroLevel(this));
        CharacterTemplate localCreature = plugin.getCharacterManager().getCharacter(summon);
        long l = SkillConfigManager.getUseSetting(paramHero, this, SkillSetting.DURATION, 60000, false);
        localCreature.addEffect(new SummonEffect(this, l, paramHero, this.expireText));
    }
}
