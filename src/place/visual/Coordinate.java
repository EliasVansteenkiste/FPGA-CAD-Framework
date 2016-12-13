package place.visual;

class Coordinate {
    private double x, y;

    Coordinate(double x, double y) {
        this.x = x;
        this.y = y;
    }

    double getX() {
        return this.x;
    }
    double getY() {
        return this.y;
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f)", this.x, this.y);
    }
}
