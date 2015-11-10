package main;

import interfaces.Logger;

class Timer {

    private Logger logger;
    private Long start = null, stop = null;

    Timer(Logger logger) {
        this.logger = logger;
    }

    void start() {
        if(this.start != null) {
            this.logger.raise("Timer has already been started");
        }

        this.start = this.getCurrentTime();
    }

    void stop() {
        if(this.start == null) {
            this.logger.raise("Timer hasn't been started");
        } else if(this.stop != null) {
            this.logger.raise("Timer has already been stopped");
        }

        this.stop = this.getCurrentTime();
    }

    double getTime() {
        if(this.start == null) {
            this.logger.raise("Timer hasn't been started");
        } else if(this.stop == null) {
            this.logger.raise("Timer hasn't been stopped");
        }

        return (this.stop - this.start) / 1e9;
    }

    private long getCurrentTime() {
        return System.nanoTime();
    }
}
