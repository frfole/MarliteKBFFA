package code.frfole.kb.game;

import net.minestom.server.coordinate.Vec;

import java.util.Arrays;

// TODO: use records when they are supported by GSON
public final class GameMap {
    private final String name;
    private final String dir;
    private final int[] boundsX;
    private final int[] boundsZ;

    @SuppressWarnings("unused")
    public GameMap(String name, String dir, int[] boundsX, int[] boundsZ) {
        this.name = name;
        this.dir = dir;
        if (boundsX.length != 2 || boundsZ.length != 2) {
            throw new IllegalArgumentException("Bounds must be of length 2");
        }
        this.boundsX = boundsX[0] > boundsX[1] ? new int[]{boundsX[1], boundsX[0]} : boundsX;
        this.boundsZ = boundsZ[0] > boundsZ[1] ? new int[]{boundsZ[1], boundsZ[0]} : boundsZ;
    }

    public String name() {
        return name;
    }

    public String dir() {
        return dir;
    }

    @Override
    public String toString() {
        return "GameMap[" +
                "name=" + name + ", " +
                "dir=" + dir + ", " +
                "boundsX=" + Arrays.toString(boundsX) + ", " +
                "boundsZ=" + Arrays.toString(boundsZ) + ']';
    }

    public Vec boundsMin() {
        return new Vec(boundsX[0], boundsZ[0]);
    }

    public Vec boundsMax() {
        return new Vec(boundsX[1], boundsZ[1]);
    }
}
