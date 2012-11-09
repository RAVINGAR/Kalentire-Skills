package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillFeatherFall extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillFeatherFall(Heroes plugin) {
        super(plugin, "FeatherFall");
        setDescription("Reduce your party's falling velocity");
        setArgumentRange(0, 0);
        setUsage("/skill featherfall");
        setIdentifiers("skill featherfall");
        setTypes(SkillType.BUFF, SkillType.SILENCABLE, SkillType.MOVEMENT);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.RADIUS.node(), 15);
        node.set(Setting.APPLY_TEXT.node(), "You feel lighter!");
        node.set(Setting.EXPIRE_TEXT.node(), "You feel gravity return to normal!");
        node.set(Setting.DURATION.node(), 15000);
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "You feel lighter!");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "You feel gravity return to normal!");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 15000, false);

        FeatherFallEffect mEffect = new FeatherFallEffect(this, duration);
        if (!hero.hasParty()) {
            if (hero.hasEffect("FeatherFall")) {
            }
            hero.addEffect(mEffect);
        } else {
            int range = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 15, false);
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
                if (pHero.hasEffect("FeatherFall")) {
                        continue;
                }
                pHero.addEffect(mEffect);
            }
        }

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class FeatherFallEffect extends ExpirableEffect {

        public FeatherFallEffect(Skill skill, long duration) {
            super(skill, "FeatherFall", duration);
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            Messaging.send(player, applyText);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            Messaging.send(player, expireText);
        }
    }

    public class SkillHeroListener implements Listener {

    	@EventHandler
		public void FeatherFallMoveEvent(PlayerMoveEvent event){
    		Player p = event.getPlayer();
    		
    		// I don't think this Cast is correct.
			if(((CharacterTemplate) p).hasEffect("FeatherFall") && event.getFrom().getY()>event.getTo().getY()){
				World world = p.getWorld();
  			  	Block block = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
  			  if (block.isEmpty()) {
				Vector vec =  new Vector(event.getPlayer().getVelocity().getX(),event.getPlayer().getVelocity().getY()-10d,event.getPlayer().getVelocity().getZ());
  				event.getPlayer().setVelocity(vec.multiply(0.03));
  				event.getPlayer().setFallDistance(0);
  			  }
			}
    	}
    }

    @Override
    public String getDescription(Hero hero) {
    	int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 15000, false);
        return getDescription().replace("$1", Util.stringDouble(duration/1000));
    }
}
