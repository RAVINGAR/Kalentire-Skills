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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SkillRaiseDemon extends ActiveSkill {
    private String expireText;

    public SkillRaiseDemon(Heroes paramHeroes)
    {
        super(paramHeroes, "RaiseDemon");
        setDescription("Summons an undead creature to fight by your side");
        setUsage("/skill raisedemon");
        setArgumentRange(0, 0);
        setIdentifiers("skill raisedemon");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SUMMONING, SkillType.SILENCEABLE);
        //listener = new MinionListener(this); //SEE SkillSummonAssist
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
        node.set("mythic-mob-type", "NecroDemon");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "The creature returns to it's hellish domain.");
        node.set("max-summons", 3);
        node.set(SkillSetting.RADIUS.node(), 7);
        node.set(SkillSetting.RADIUS_INCREASE_PER_WISDOM.node(), 0.005);

        return node;
    }

    public void init()
    {
        super.init();
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "The creature returns to it's hellish domain.");
    }

    public SkillResult use(Hero paramHero, String[] paramArrayOfString)
    {
        Player localPlayer = paramHero.getPlayer();
        if (paramHero.getSummons().size() < SkillConfigManager.getUseSetting(paramHero, this, "max-summons", 3, false))
        {
            int i = SkillConfigManager.getUseSetting(paramHero, this, SkillSetting.MAX_DISTANCE, 5, false);
            Location localLocation = localPlayer.getTargetBlock(null, i).getLocation().add(0,1,0);
            try {
                localLocation.getWorld().spawnParticle(Particle.WARPED_SPORE, localLocation.add(0, 0.5, 0), 40, 1, 1, 1, 0.5);
                localLocation.getWorld().spawnParticle(Particle.CLOUD, localLocation.add(0, 0, 0), 10, 1, 1, 1, 0.5);
                LivingEntity summon = (LivingEntity) MythicBukkit.inst().getAPIHelper().spawnMythicMob(SkillConfigManager.getUseSetting(paramHero, this, "mythic-mob-type", "NecroDemon"), localLocation);
                ActiveMob mob = MythicBukkit.inst().getMobManager().getActiveMob(summon.getUniqueId()).get();
                mob.setParent(new GenericCaster(new BukkitPlayer(localPlayer)));
                mob.setLevel(paramHero.getHeroLevel(this));
                CharacterTemplate localCreature = plugin.getCharacterManager().getCharacter(summon);
                long l = SkillConfigManager.getUseSetting(paramHero, this, SkillSetting.DURATION, 60000, false);
                localCreature.addEffect(new SummonEffect(this, l, paramHero, this.expireText));
                summon.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD, 1));
                broadcastExecuteText(paramHero);
                localPlayer.sendMessage(ChatComponents.GENERIC_SKILL + "A hellish creature rises from the ground");
            }
            catch(InvalidMobTypeException e) {
                return SkillResult.FAIL;
            }
            return SkillResult.NORMAL;
        }
        localPlayer.sendMessage("You can't control any more skeletons!");
        return SkillResult.FAIL;
    }
}
