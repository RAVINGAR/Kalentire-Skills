package com.herocraftonline.heroes.characters.skill.skills;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillFirewave extends ActiveSkill {

    public SkillFirewave(Heroes plugin) {
        super(plugin, "Firewave");
        setDescription("You throw a wave of fire in all directions!");
        setUsage("/skill firewave");
        setArgumentRange(0, 0);
        setTypes(SkillType.FIRE, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
        setIdentifiers("skill firewave");
    }

    @SuppressWarnings("deprecation")
    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        PlayerInventory inv = player.getInventory();

        Map<Integer, ? extends ItemStack> arrowSlots = inv.all(Material.ARROW);

        int numArrows = 0;
        for (Map.Entry<Integer, ? extends ItemStack> entry : arroSlots.entrySet()) {
            numArrows += entry.getValue().getAmount();
        }

        if (numArrows == 0) {
            Messaging.send(player, "You have no balls.");
            return new SkillResult(ResultType.MISSING_REAGENT, false);
        }

        numArrows = numArrows > 24 ? 24 : numArrows;

        int removedArrows = 0;
        for (Map.Entry<Integer, ? extends ItemStack> entry : arrowSlots.entrySet()) {
            int amount = entry.getValue().getAmount();
            int remove = amount;
            if (removedArrows + remove > numArrows) {
                remove = numArrows - removedArrows;
            }
            removedArrows += remove;
            if (remove == amount) {
                inv.clear(entry.getKey());
            } else {
                inv.getItem(entry.getKey()).setAmount(amount - remove);
            }

            if (removedArrows >= numArrows) {
                break;
            }
        }
        player.updateInventory();

        double diff = 2 * Math.PI / numArrows;
        for (double a = 0; a < 2 * Math.PI; a += diff) {
            Vector vel = new Vector(Math.cos(a), 0, Math.sin(a));
            player.shootArrow().setVelocity(vel);
        }
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
