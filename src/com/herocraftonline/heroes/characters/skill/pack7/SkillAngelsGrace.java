package com.herocraftonline.heroes.characters.skill.pack7;

import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.InvulnerabilityEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillAngelsGrace extends ActiveSkill {

    public SkillAngelsGrace(Heroes plugin) {
        super(plugin, "AngelsGrace");
        setDescription("Grants you and your party within $1 blocks invulnerability for $2s");
        setUsage("/skill AngelsGrace");
        setArgumentRange(0, 0);
        setIdentifiers("skill AngelsGrace", "skill Angels");
        setTypes(SkillType.BUFFING, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 7, false);
        double duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);

        return getDescription().replace("$1", radius + "").replace("$2", (int) (duration / 1000) + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 7);
        node.set(SkillSetting.DURATION.node(), 3000);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 12000, false);

        InvulnerabilityEffect iEffect = new InvulnerabilityEffect(this, player, duration);
        if (!hero.hasParty()) {
            hero.addEffect(iEffect);
        }
        else {
            int rangeSquared = (int) Math.pow(SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 7, false), 2);
            for (Hero pHero : hero.getParty().getMembers()) {
                Player pPlayer = pHero.getPlayer();
                if (!pPlayer.getWorld().equals(player.getWorld()) || pPlayer.getLocation().distanceSquared(player.getLocation()) > rangeSquared) {
                    continue;
                }
                pHero.addEffect(iEffect);
            }
        }

        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }
}