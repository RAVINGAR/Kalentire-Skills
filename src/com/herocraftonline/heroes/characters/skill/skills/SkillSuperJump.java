package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillSuperJump extends ActiveSkill {

    public SkillSuperJump(Heroes plugin) {
        super(plugin, "SuperJump");
        setDescription("You launch into the air, and float safely to the ground.");
        setUsage("/skill superjump");
        setArgumentRange(0, 0);
        setIdentifiers("skill superjump");
        setTypes(SkillType.MOVEMENT, SkillType.PHYSICAL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000);
        node.set("jump-force", 4.0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        float jumpForce = (float) SkillConfigManager.getUseSetting(hero, this, "jump-force", 1.0, false);
        Vector v1 = new Vector(0, jumpForce, 0);
        Vector v = player.getVelocity().add(v1);
        player.setVelocity(v);
        player.setFallDistance(-8f);
        int duration = (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION.node(), 5000, false);
        hero.addEffect(new SafeFallEffect(this, duration));
        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}