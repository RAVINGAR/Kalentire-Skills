package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;

import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public abstract class SkillBaseMassBlockEffector extends ActiveSkill {

    public SkillBaseMassBlockEffector(Heroes plugin, String name) {
        super(plugin, name);
    }

    protected static final class BlockRegion {

        public static BlockRegion bounds(Block center, int boundsX, int boundsY, int boundsZ) {
            return new BlockRegion(center.getRelative(-boundsX, -boundsY, -boundsZ), boundsX * 2 + 1, boundsY * 2 + 1, boundsZ * 2 + 1);
        }

        public static BlockRegion bounds(Block center, int bounds) {
            return bounds(center, bounds, bounds, bounds);
        }

        private final int ox, oy, oz;       // Origin vector
        private final BlockState[][][] region;   // Blocks

        public BlockRegion(Block origin, int sizeX, int sizeY, int sizeZ) {
            checkNotNull(origin, "origin");
            checkArgument(sizeX > 0, "sizeX is non positive");
            checkArgument(sizeY > 0, "sizeY is non positive");
            checkArgument(sizeZ > 0, "sizeZ is non positive");

            World world = origin.getWorld();

            if (origin.getY() + sizeY > world.getMaxHeight()) {
                sizeY = world.getMaxHeight() - origin.getY();
            }

            // I highly doubt I need constraints on size x and z but you never know o.o

            this.ox = origin.getX();
            this.oy = origin.getY();
            this.oz = origin.getZ();

            region = new BlockState[sizeX][sizeY][sizeZ];

            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeX; y++) {
                    for (int z = 0; z < sizeX; z++) {
                        region[x][y][z] = world.getBlockAt(ox + x, oy + y, oz + z).getState();
                    }
                }
            }
        }

        public int getOriginX() {
            return ox;
        }

        public int getOriginY() {
            return oy;
        }

        public int getOriginZ() {
            return oz;
        }

        public BlockVector getOrigin() {
            return new BlockVector(getOriginX(), getOriginY(), getOriginZ());
        }

        public int getSizeX() {
            return region.length;
        }

        public int getSizeY() {
            return region[0].length;
        }

        public int getSizeZ() {
            return region[0][0].length;
        }

        public BlockState getBlock(int ix, int iy, int iz) throws ArrayIndexOutOfBoundsException {
            return region[ix][iy][iz];
        }

        public BlockState getRelative(Block block, int x, int y, int z) {
            int rx = block.getX() - ox + x;
            int ry = block.getY() - oy + y;
            int rz = block.getZ() - oz + z;

            if (rx < 0 || rx >= getSizeX() || ry < 0 || ry >= getSizeY() || rz < 0 || rz >= getSizeZ()) {
                return null;
            }

            return getBlock(rx, ry, rz);
        }

        public BlockState getRelative(Block block, BlockFace face, int distance) {
            return getRelative(block, face.getModX() * distance, face.getModY() * distance, face.getModZ() * distance);
        }

        public BlockState getRelative(Block block, BlockFace face) {
            return getRelative(block, face, 1);
        }
    }
}
