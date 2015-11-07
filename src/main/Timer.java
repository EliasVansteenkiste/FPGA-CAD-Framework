package main;

import util.Logger;

class Timer {

    private Long start = null, stop = null;

    void start() {
        if(this.start != null) {
            Logger.raise("Timer has already been started");
        }

        this.start = this.getCurrentTime();
    }

    void stop() {
        if(this.start == null) {
            Logger.raise("Timer hasn't been started");
        } else if(this.stop != null) {
            Logger.raise("Timer has already been stopped");
        }

        this.stop = this.getCurrentTime();
    }

    double getTime() {
        if(this.start == null) {
            Logger.raise("Timer hasn't been started");
        } else if(this.stop == null) {
            Logger.raise("Timer hasn't been stopped");
        }

        return (this.stop - this.start) / 1e9;
    }

    private long getCurrentTime() {
        return System.nanoTime();
    }
}
