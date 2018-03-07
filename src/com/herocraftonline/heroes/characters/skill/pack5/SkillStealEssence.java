package com.herocraftonline.heroes.characters.skill.pack5;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class SkillStealEssence extends TargettedSkill {

    public SkillStealEssence(Heroes plugin) {
        super(plugin, "StealEssence");
        setDescription("You steal beneficial effects from your target.");
        setUsage("/skill stealessence");
        setArgumentRange(0, 0);
        setIdentifiers("skill stealessence");
        setTypes(SkillType.DISPELLING, SkillType.AGGRESSIVE, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% used %skill% and stole %effect% from %target%!");
        node.set("max-steals", 1);

        return node;
    }

    @Override
    public void init() {
        super.init();

        this.setUseText(SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% used %skill% and stole %effect%from %target%!").replace("%hero%", "$1").replace("%skill%", "$2").replace("%effect%", "$3").replace("%target%", "$4"));
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player))
        	return SkillResult.INVALID_TARGET;

        ArrayList<Effect> possibleEffects = new ArrayList<Effect>();
        Hero tHero = plugin.getCharacterManager().getHero((Player) target);
        for (Effect e : tHero.getEffects()) {
            if (e.isType(EffectType.BENEFICIAL) && e.isType(EffectType.DISPELLABLE)) {
                possibleEffects.add(e);
            }
        }

        if (possibleEffects.isEmpty()) {
            player.sendMessage("That target has no effects to steal!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        String stolenNames = "";
        int numEffects = SkillConfigManager.getUseSetting(hero, this, "max-steals", 1, false);
        for (int i = 0; i < numEffects && possibleEffects.size() > 0; i++) {
            Effect stolenEffect = possibleEffects.get(Util.nextInt(possibleEffects.size()));
            tHero.removeEffect(stolenEffect);

            if (stolenEffect instanceof ExpirableEffect)
                ((ExpirableEffect) stolenEffect).setApplier(player);

            hero.addEffect(stolenEffect);
            possibleEffects.remove(stolenEffect);
            stolenNames += stolenEffect.getName() + " ";
        }

        broadcast(player.getLocation(), getUseText(), player.getName(), getName(), stolenNames, tHero.getPlayer().getName());

        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_BAT_LOOP.value(), 0.8F, 2.0F);

        return SkillResult.NORMAL;
    }
}
