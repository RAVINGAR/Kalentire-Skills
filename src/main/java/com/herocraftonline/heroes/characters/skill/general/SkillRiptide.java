package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
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

    public SkillRiptide(final Heroes plugin) {
        super(plugin, "Riptide");
        setUsage("/skill riptide");
        setIdentifiers("skill riptide");
        setArgumentRange(0, 0);
        setDescription("You wield your trident and turn into a riptide, whirling through the air/water....");
        setTypes(SkillType.DAMAGING, SkillType.MOVEMENT_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);
        Bukkit.getServer().getPluginManager().registerEvents(new GlidingListener(this), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        return super.getDefaultConfig();
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL
                        + "%hero% used Riptide!")
                .replace("%hero%", "$2").replace("$hero$", "$2");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final Location playerLocation = player.getLocation();

        final float pitch = playerLocation.getPitch();
        final float yaw = playerLocation.getYaw();

        // f = yaw; f1 = pitch

        float f2 = (float) (-Math.sin(yaw * 0.017453292F) * Math.cos(pitch * 0.017453292F));
        float f3 = (float) -Math.sin(pitch * 0.017453292F);
        float f4 = (float) (Math.cos(yaw * 0.017453292F) * Math.cos(pitch * 0.017453292F));
        final float f5 = (float) Math.sqrt(f2 * f2 + f3 * f3 + f4 * f4);

        f2 *= 3.0F / f5;
        f3 *= 3.0F / f5;
        f4 *= 3.0F / f5;

        final Vector vector = new Vector(f2, f3, f4);

        player.setVelocity(vector);
        player.setGliding(true);

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

//    private void PerformDash(Hero hero, Player player, double damage, double speedMult, double boost, double radius) {
//
//    }


    // Will set the players EnumAnimation type to SPEAR, which will hopefully be the animation when the player interacts with a trident...
    private void SpearAnimation(final Player player) {

    }

    // Using the Elytra Glide animation and spinning the player around to re-create riptide animation
    private void RiptideAnimation(final Player player) {

    }

    // Elytra glide animation while spinning the player around
    private void SpinAnimation(final Player player) {

    }

    // Cool particle effect
    private void RiptideParticleEffect(final Player player) {

    }

    public static class GlidingListener implements Listener {
        private final Skill skill;

        GlidingListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityToggleGlide(final EntityToggleGlideEvent event) {
            final Player player = (Player) event.getEntity();

            if (!player.isOnGround()) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(false);
        }
    }
}
