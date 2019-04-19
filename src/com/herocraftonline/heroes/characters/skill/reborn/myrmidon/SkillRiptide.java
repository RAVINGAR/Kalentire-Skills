package com.herocraftonline.heroes.characters.skill.reborn.myrmidon;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.util.Vector;

public class SkillRiptide extends ActiveSkill {

    String applyText;

    public SkillRiptide(Heroes plugin) {
        super(plugin, "Riptide");
        setUsage("/skill riptide");
        setIdentifiers("skill riptide");
        setArgumentRange(0, 0);
        setDescription("You wield your trident and turn into a riptide, whirling through the air/water....");
        setTypes(SkillType.DAMAGING, SkillType.MOVEMENT_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);
        Bukkit.getServer().getPluginManager().registerEvents(new GlidingListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection cs = super.getDefaultConfig();
        return cs;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL
                + "%hero% used Riptide!")
                .replace("%hero%", "$2");
    }

    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        Location playerLocation = player.getLocation();

        float pitch = playerLocation.getPitch();
        float yaw = playerLocation.getYaw();

        // f = yaw; f1 = pitch

        float f2 = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);

        float f3 = -MathHelper.sin(pitch * 0.017453292F);

        float f4 = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);

        float f5 = MathHelper.c(f2 * f2 + f3 * f3 + f4 * f4);

        float f6 = 3.0F * ((1.0F + (float)3) / 4.0F);
        f2 *= f6 / f5;
        f3 *= f6 / f5;
        f4 *= f6 / f5;

        Vector vector = new Vector((double) f2, (double) f3, (double) f4);

        player.setVelocity(vector);
        player.setGliding(true);

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

//    private void PerformDash(Hero hero, Player player, double damage, double speedMult, double boost, double radius) {
//
//    }


    // Will set the players EnumAnimation type to SPEAR, which will hopefully be the animation when the player interacts with a trident...
    private void SpearAnimation(Player player) {

    }

    // Using the Elytra Glide animation and spinning the player around to re-create riptide animation
    private void RiptideAnimation(Player player) {

    }

    // Elytra glide animation while spinning the player around
    private void SpinAnimation(Player player) {

    }

    // Cool particle effect
    private void RiptideParticleEffect(Player player) {

    }

    public class GlidingListener implements Listener {
        private final Skill skill;

        GlidingListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityToggleGlide(EntityToggleGlideEvent event) {
            Player player = (Player) event.getEntity();

            if(!player.isOnGround()) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(false);
        }
    }
}
