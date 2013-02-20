package com.herocraftonline.heroes.characters.skill.skills;

/* I, Roadkill909, the author of this class, PotionListener, allow anyone to use, modify, 
 * distribute, or relicense this source file or this file in compiled form without restriction */

import static com.herocraftonline.heroes.api.SkillResult.FAIL;
import static com.herocraftonline.heroes.api.SkillResult.INVALID_TARGET;
import static com.herocraftonline.heroes.api.SkillResult.NORMAL;
import static com.herocraftonline.heroes.characters.skill.SkillConfigManager.getUseSetting;
import static com.herocraftonline.heroes.characters.skill.SkillType.HEAL;
import static com.herocraftonline.heroes.characters.skill.SkillType.ITEM;
import static com.herocraftonline.heroes.characters.skill.SkillType.LIGHT;
import static com.herocraftonline.heroes.characters.skill.SkillType.MANA;
import static com.herocraftonline.heroes.characters.skill.SkillType.SILENCABLE;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.server.EntityPotion;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftThrownPotion;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.util.Setting;

public class SkillAmpul extends ActiveSkill {
    private Map<ThrownPotion, Long> potions = new LinkedHashMap<ThrownPotion, Long>(89) {//Didn't know this handy trick, so I reused it.
        private static final long serialVersionUID = -8018803104297802046L;

        @Override
        protected boolean removeEldestEntry(Entry<ThrownPotion, Long> eldest) {
            return (size() > 50 || eldest.getValue() + 10000 <= System.currentTimeMillis());
        }
    };

    public SkillAmpul(Heroes plugin) {
        super(plugin, "Ampul");
        setDescription("You throw a health vial that heals nearby party members for a maximum of $1.  Can also be used on a single player by providing a name");
        setUsage("/skill ampul [player name]");
        setIdentifiers("skill ampul");
        setArgumentRange(0, 1);
        setTypes(HEAL,LIGHT,MANA,SILENCABLE,ITEM);
        plugin.getServer().getPluginManager().registerEvents(new PotionListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription().replaceAll("$1", getMaxHeal(hero)+" hp");
    }

    private int getMaxHeal(Hero hero){
        return getUseSetting(hero, this, Setting.AMOUNT, 150, false);
    }

    @Override
    public SkillResult use(Hero casterHero, String[] args) {
        final Player casterPlayer = casterHero.getPlayer();

        if(args.length>0){
            final Player target= Bukkit.getPlayer(args[0]);

            //Ignores not present or offline players
            if(target == null || !target.isOnline()) {
                return INVALID_TARGET;
            }

            final Location targetLocation = target.getLocation();

            //ignores out of range players
            if(casterPlayer.getLocation().getWorld()!=targetLocation.getWorld()
                    ||casterPlayer.getLocation().distance(targetLocation)>getUseSetting(casterHero, this, Setting.MAX_DISTANCE, 10,false)){
                casterHero.getPlayer().sendMessage("Target is out of range!");
                return FAIL;
            }

            targetLocation.getWorld().playEffect(targetLocation, Effect.POTION_BREAK, 8197);
            //heals the target
            broadcast(casterPlayer.getLocation(),"$1 used an ampul on $2", casterPlayer.getDisplayName(), target.getDisplayName());
            target.setHealth(target.getHealth() + getMaxHeal(casterHero));
            return NORMAL;


        } else {
            broadcastExecuteText(casterHero);
            //creates and throws a potion from the player
            net.minecraft.server.World world = ((CraftWorld) casterPlayer.getWorld()).getHandle();
            EntityPotion entityPotion = new EntityPotion(world, ((CraftLivingEntity) casterPlayer).getHandle(), 8197);
            world.addEntity(entityPotion);

            //converts it to bukkit
            final ThrownPotion thrownPotion = new CraftThrownPotion(world.getServer(),entityPotion);
            thrownPotion.setVelocity(thrownPotion.getVelocity().multiply(2)); //OPTIONAL Makes the potion fly twice as fast 

            //adds it to the Potionmanager
            potions.put(thrownPotion,System.currentTimeMillis());
            return NORMAL;
        }
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.HEALTH.node(), 250);
        node.set(Setting.MAX_DISTANCE.node(),10);
        return node;
    }

    //Used to listen for potion splash events
    public class PotionListener implements Listener{

        @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=false)
        public void onPotionSplash(PotionSplashEvent event){
            if (potions.remove(event.getPotion()) == null) {
                return;
            }
            final LivingEntity shooter = event.getPotion().getShooter();
            final Hero casterHero;
            if (shooter !=null && shooter instanceof Player) {
                casterHero = plugin.getCharacterManager().getHero((Player) shooter);
            } else {
                return;
            }

            for(LivingEntity affected : event.getAffectedEntities()){
                if(affected instanceof Player){
                    final Hero affectedHero = plugin.getCharacterManager().getHero((Player) affected);

                    //heals friendly players for the right amount
                    if(isFriendlyPlayer(casterHero, affectedHero)){
                        affected.setHealth(affected.getHealth() + (int) (getMaxHeal(casterHero) * event.getIntensity(affected)));
                    }
                }
                //entities are removed from map, 
                //just in case some devs are careless and don't ignore cancelled events
                //which may heal the player twice
                event.setIntensity(affected, 0);
            }
            event.setCancelled(true);
        }

        //Checks to see if the heroes are in the same party or the same person
        private boolean isFriendlyPlayer(Hero hero, Hero affected) {
            if(hero.getPlayer() == affected.getPlayer()) {
                return true;
            }
            return hero.getParty() != null && hero.getParty() == affected.getParty();
        }
    }
}