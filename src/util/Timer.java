package util;

public class Timer {

    private Long time = (long) 0, start;
    private boolean running = false;

    public void start() throws IllegalStateException {
        if(!this.running) {
            this.start = this.getCurrentTime();
            this.running = true;

        } else {
            throw new IllegalStateException("Timer has already been started");
        }
    }

    public void stop() throws IllegalStateException {
        if(this.running) {
            this.time += this.getCurrentTime() - this.start;
            this.running = false;

        } else {
            throw new IllegalStateException("Timer hasn't been started");
        }


    }

    public double getTime() throws IllegalStateException {
        if(!this.running) {
            return this.time / 1e9;

        } else {
            throw new IllegalStateException("Timer is still running");
        }
    }

    private long getCurrentTime() {
        return System.nanoTime();
    }
}
