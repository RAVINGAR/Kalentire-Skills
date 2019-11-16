package com.herocraftonline.heroes.characters.skill.reborn.other;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.command.BasicCommand;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkillSpawnParticleTool extends ActiveSkill {

    private final static Particle[] particleTypes = Particle.values();
    private final static int PARTICLE_TYPES_PER_PAGE = 10;

    public SkillSpawnParticleTool(Heroes plugin) {
        super(plugin, "SpawnParticle");
        this.setDescription("Tests out spawning particle, with x,y,z relative to player. Use /hero spawnparticle help [page#] to view particle types.");
        this.setUsage("/hero spawnparticle §9<particleType> <x> <y> <z> <particleCount> <xOffset> <yOffset> <zOffset> [extraData] [force extended distance render]");
        this.setArgumentRange(0, 10);
        this.setIdentifiers("skill spawnparticle");
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        return SkillResult.NORMAL;
    }

    @Override
    public boolean execute(CommandSender sender, String identifier, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = ((Player) sender);

        if (args.length > 0 && args[0].toLowerCase().equals("help")){
            // Handle particle types help message
            int index = 0;
            if (args.length > 1){
                int page = 0; // zero-based
                try {
                    page = Integer.parseInt(args[1]) - 1;
                } catch (NumberFormatException e){
                    player.sendMessage(ChatColor.RED + "Invalid help page number.");
                    return false;
                }

                if (page < 0){
                    player.sendMessage(ChatColor.RED + "Invalid help page number.");
                    return false;
                }

                if (page * PARTICLE_TYPES_PER_PAGE < particleTypes.length){
                    index = page * PARTICLE_TYPES_PER_PAGE;
                } else {
                    player.sendMessage(ChatColor.RED + "Invalid help page number.");
                    return false;
                }
            }

            player.sendMessage(ChatColor.GRAY + "Player types: " + getParticleTypeMessage(index));
            player.sendMessage(ChatColor.GRAY + "Use /skill spawnparticle help [page#] to view more particle types.");
        } else if (args.length > 7 ) {
            int particleType = 0;
            try {
                particleType = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored){
            }

            double x = 0;
            try {
                x = Double.parseDouble(args[1]);
            } catch (NumberFormatException ignored) {
                // do nothing
            }

            double y = 0;
            try {
                y = Double.parseDouble(args[2]);
            } catch (NumberFormatException ignored) {
                // do nothing
            }

            double z = 0;
            try {
                z = Double.parseDouble(args[3]);
            } catch (NumberFormatException ignored) {
                // do nothing
            }

            int particleCount = 1;
            try {
                particleCount = Integer.parseInt(args[4]);
            } catch (NumberFormatException ignored){
            }

            // Offsets
            double xOffset = 0;
            try {
                xOffset = Double.parseDouble(args[5]);
            } catch (NumberFormatException ignored) {
            }

            double yOffset = 0;
            try {
                yOffset = Double.parseDouble(args[6]);
            } catch (NumberFormatException ignored) {
            }

            double zOffset = 0;
            try {
                zOffset = Double.parseDouble(args[7]);
            } catch (NumberFormatException ignored) {
            }

            double extraData = 0;
            if (args.length > 8) {
                // extra data
                try {
                    extraData = Double.parseDouble(args[8]);
                } catch (NumberFormatException ignored) {
                }
            }

            boolean forceRender = false;
            if (args.length == 10) {
                forceRender = Boolean.parseBoolean(args[9]);
            }

            World world = player.getWorld();

            Particle particle;
            if (0 <= particleType && particleType < particleTypes.length){
                particle = particleTypes[particleType];
            } else {
                particle = particleTypes[0];
                player.sendMessage(ChatColor.RED + "Invalid particle type " + particleType + ". Using default: " + particleTypes[0].toString());
            }
            // Old method (which didn't use enum values)
//            switch (particleType){
//                case 0:
//                    particle = Particle.EXPLOSION_NORMAL;
//                    break;
//                case 1:
//                    particle = Particle.VILLAGER_HAPPY;
//                    break;
//                case 2:
//                    particle = Particle.CLOUD;
//                    break;
//                case 3:
//                    particle = Particle.REDSTONE;
//                    break;
//                case 4:
//                    particle = Particle.HEART;
//                    break;
//                case 5:
//                    particle = Particle.SMOKE_NORMAL;
//                    break;
//                case 6:
//                    particle = Particle.BLOCK_DUST;
//                    break;
//                case 7:
//                    particle = Particle.BLOCK_CRACK;
//                    break;
//                default:
//                    particle = Particle.EXPLOSION_NORMAL;
//                    player.sendMessage("Using default. Particle types: 0: EXPLOSION_NORMAL; 1: VILLAGER_HAPPY; 2: Particle.CLOUD; 3: REDSTONE; 4: Particle.HEART; 5: SMOKE_NORMAL; 6: BLOCK_DUST; 7: BLOCK_CRACK; default: EXPLOSION_NORMAL;");
//            }

//            switch (particle){
//                case REDSTONE:
//                    world.spawnParticle(particle, player.getLocation().add(x,y,z),particleCount,
//                            xOffset,yOffset,zOffset,extraData, new Particle.DustOptions(Color.BLUE,2), forceRender);
//                    break;
//                case BLOCK_DUST:
//                    world.spawnParticle(particle, player.getLocation().add(x,y+1,z),particleCount,
//                            xOffset,yOffset,zOffset,extraData, world.getBlockAt(player.getLocation().add(x,y,z)));
//                    break;
//                case BLOCK_CRACK:
//                    world.spawnParticle(particle, player.getLocation().add(x,y+1,z),particleCount,
//                            xOffset,yOffset,zOffset,extraData, world.getBlockAt(player.getLocation().add(x,y,z)));
//                    break;
//                default:
            try {
                world.spawnParticle(particle, player.getLocation().add(x, y, z), particleCount,
                        xOffset, yOffset, zOffset, extraData);
            } catch (Exception e){
                player.sendMessage(ChatColor.RED + "Particle type " + particleTypes[particleType].toString() + " is not correctly handled. See log for more details.");
                e.printStackTrace();
            }
//            }
        } else {
            player.sendMessage(ChatColor.GRAY + "Need at least 8 arguments for SpawnParticle.");
            player.sendMessage(ChatColor.GRAY + "Usage: " + getUsage());
            player.sendMessage(ChatColor.GRAY + "Player types: " + getParticleTypeMessage(0));
            player.sendMessage(ChatColor.GRAY + "Use /skill spawnparticle help [page#] to view more particle types.");
//            player.sendMessage("Particle types: 0: EXPLOSION_NORMAL; 1: VILLAGER_HAPPY; 2: Particle.CLOUD; 3: REDSTONE; 4: Particle.HEART; 5: SMOKE_NORMAL; 6: BLOCK_DUST; default: EXPLOSION_NORMAL;");
        }

//        world.spawnParticle(Particle.VILLAGER_HAPPY, // effect
//                target.getLocation().add(0, 0.5, 0), // location
//                25, // particle count
//                1,1,1, // offsets x,y,z respectively
//                1.0f, // extra data - normally speed
//                0); // data

        return true;
    }

    private String getParticleTypeMessage(int startingParticleIndex){
        // Assumes index has been checked for validity
        StringBuilder message = new StringBuilder(startingParticleIndex + ": " + particleTypes[startingParticleIndex]);
        for (int i = startingParticleIndex + 1; i < particleTypes.length && i < startingParticleIndex + PARTICLE_TYPES_PER_PAGE; i++){
            message.append("; ").append(i).append(": ").append(particleTypes[i]);
        }

        return message.toString();
    }


}
