package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillStealEssence extends TargettedSkill {

    public SkillStealEssence(Heroes plugin) {
        super(plugin, "StealEssence");
        setDescription("You steal beneficial effects from your target.");
        setUsage("/skill stealessence");
        setArgumentRange(0, 1);
        setIdentifiers("skill stealessence", "skill sessence");
        setTypes(SkillType.DEBUFF, SkillType.KNOWLEDGE, SkillType.BUFF, SkillType.HARMFUL, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.APPLY_TEXT.node(), "%hero% used %skill% and stole %effect% from %target%!");
        node.set(Setting.AMOUNT.node(), 3);
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.setUseText(SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%hero% used %skill% and stole %effect%from %target%!").replace("%hero%", "$1").replace("%skill%", "$2").replace("%effect%", "$3").replace("%target%", "$4"));
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player))
        	return SkillResult.INVALID_TARGET;

        ArrayList<Effect> possibleEffects = new ArrayList<Effect>();
        Hero tHero = plugin.getHeroManager().getHero((Player) target);
        for (Effect e : tHero.getEffects()) {
            if (e.isType(EffectType.BENEFICIAL) && e.isType(EffectType.DISPELLABLE)) {
                possibleEffects.add(e);
            }
        }

        if (possibleEffects.isEmpty()) {
            Messaging.send(player, "That target has no effects to steal!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        String stolenNames = "";
        int numEffects = SkillConfigManager.getUseSetting(hero, this, Setting.AMOUNT.node(), 3, false);
        for (int i = 0; i < numEffects && possibleEffects.size() > 0; i++) {
            Effect stolenEffect = possibleEffects.get(Util.rand.nextInt(possibleEffects.size()));
            tHero.removeEffect(stolenEffect);
            hero.addEffect(stolenEffect);
            possibleEffects.remove(stolenEffect);
            stolenNames += stolenEffect.getName() + " ";
        }

        broadcast(player.getLocation(), getUseText(), player.getDisplayName(), getName(), stolenNames, tHero.getPlayer().getDisplayName());
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

}
