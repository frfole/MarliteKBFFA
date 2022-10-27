package code.frfole.kb.game;

public record GameMap(String name, String dir, Bound boundsX, Bound boundsZ) {

    public int area() {
        return boundsX.area() * boundsZ.area();
    }

    public record Bound(int min, int max) {
        public Bound {
            if (min > max) {
                int temp = min;
                min = max;
                max = temp;
            }
        }

        public int area() {
            return max - min + 1;
        }
    }
}
