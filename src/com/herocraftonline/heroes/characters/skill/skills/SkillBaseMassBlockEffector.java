package com.herocraftonline.heroes.characters.skill.skills;

import com.google.common.base.Predicate;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;
import org.bukkit.util.NumberConversions;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public abstract class SkillBaseMassBlockEffector extends ActiveSkill {

    public SkillBaseMassBlockEffector(Heroes plugin, String name) {
        super(plugin, name);
    }

    public interface BlockProcessor {
        void process(Hero hero, BlockState state, BlockRegion region);
    }

    protected static final class BlockRegion implements Iterable<BlockState> {

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

        public BlockState getRelative(BlockState block, int x, int y, int z) {
            int rx = block.getX() - ox + x;
            int ry = block.getY() - oy + y;
            int rz = block.getZ() - oz + z;

            if (rx < 0 || rx >= getSizeX() || ry < 0 || ry >= getSizeY() || rz < 0 || rz >= getSizeZ()) {
                return null;
            }

            return getBlock(rx, ry, rz);
        }

        public BlockState getRelative(BlockState block, BlockFace face, int distance) {
            return getRelative(block, face.getModX() * distance, face.getModY() * distance, face.getModZ() * distance);
        }

        public BlockState getRelative(BlockState block, BlockFace face) {
            return getRelative(block, face, 1);
        }

        public BlockState getCenter() {
            return getBlock(
                    NumberConversions.ceil(getSizeX() / 2d),
                    NumberConversions.ceil(getSizeY() / 2d),
                    NumberConversions.ceil(getSizeZ() / 2d));
        }

        @Override
        public Iterator<BlockState> iterator() {
            return new Iterator<BlockState>() {

                private int x = 0, y = 0, z = 0;

                @Override
                public boolean hasNext() {
                    return z < getSizeZ() || y < getSizeY() || x < getSizeX();
                }

                @Override
                public BlockState next() {
                    BlockState result;
                    try {
                        result = getBlock(x, y, z);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        throw new NoSuchElementException();
                    }

                    if (++z == getSizeZ()) {
                        z = 0;
                        if (++y == getSizeY()) {
                            y = 0;
                            x++;
                        }
                    }

                    return result;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }
            };
        }
    }
}
