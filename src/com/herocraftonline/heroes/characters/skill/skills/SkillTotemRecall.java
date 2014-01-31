package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.skills.totem.SkillBaseTotem;
import com.herocraftonline.heroes.characters.skill.skills.totem.Totem;
import com.herocraftonline.heroes.characters.skill.skills.totem.TotemEffect;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class SkillTotemRecall extends ActiveSkill {

    public SkillTotemRecall(Heroes plugin) {
        super(plugin, "TotemRecall");
        setArgumentRange(0,0);
        setUsage("/skill totemrecall");
        setIdentifiers("skill totemrecall");
        setDescription("You call your active totem back, regaining mana proportional to how much of the totem's lifespan was used, minus $1 percent.");
    }

    @Override
    public String getDescription(Hero hero) {
        int manaPenaltyPercent = SkillConfigManager.getUseSetting(hero, this, "mana-penalty-percent", 10, false);
        return getDescription()
                .replace("$1", manaPenaltyPercent + "");
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
        if(!hero.hasEffect("TotemEffect") || !(hero.getEffect("TotemEffect") instanceof TotemEffect)) {
            hero.getPlayer().sendMessage(ChatColor.RED + "You don't have an active totem!");
            return SkillResult.FAIL;
        }
        TotemEffect tE = (TotemEffect) hero.getEffect("TotemEffect");
        Totem totem = tE.getTotem();
        SkillBaseTotem totemSkill = tE.getTotemSkill();
        double manaPenaltyPercent = SkillConfigManager.getUseSetting(hero, this, "mana-penalty-percent", 10, false) / 100D;
        double mana = SkillConfigManager.getUseSetting(hero, totemSkill, SkillSetting.MANA, 0, true);
        mana -= SkillConfigManager.getUseSetting(hero, totemSkill, SkillSetting.MANA_REDUCE_PER_LEVEL, 0.0D, false) * hero.getSkillLevel(totemSkill);
        double manaPenalized = mana - (mana * manaPenaltyPercent);
        long totemLife = System.currentTimeMillis() - tE.getApplyTime();
        double manaGrant = (manaPenalized * totemLife) / tE.getDuration();
        hero.setMana(hero.getMana() + (int) manaGrant);
        tE.expire();
        broadcastExecuteText(hero);
        
        // Unlike the others, not worth calling clone on the location every single time we add 0.1. Just going to add relative amounts.
        Location totemLoc = totem.getLocation().clone();
        /* This is the new Particle API system for Spigot - the first few int = id, data, offsetX/Y/Z, speed, count, radius)
         * offset controls how spread out the particles are
         * id and data only work for two particles: ITEM_BREAK and TILE_BREAK
         * */
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.5, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.1, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.1, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.2, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.1, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.1, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.1, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.1, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.1, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.1, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.1, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.1, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.1, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.1, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.1, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.1, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.1, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        totemLoc.getWorld().spigot().playEffect(totemLoc.add(0, 0.1, 0), Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 25, 16);
        
        return SkillResult.NORMAL;
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.USE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% called back their totem!");
        node.set("mana-penalty-percent", 10);
        return node;
    }

}
