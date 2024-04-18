package scanalesespinoza.ratelimitservice;

import io.github.bucket4j.Bucket;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
public class RateLimiterService {
    private static Bucket wsBucket;
    private static Bucket dbBucket;
    private final ReentrantLock lock = new ReentrantLock();
    private AtomicInteger thresholdCount = new AtomicInteger(0);
    private long lastResetTime = System.currentTimeMillis();

    public RateLimiterService() {
        wsBucket = Bucket.builder()
            .addLimit(limit -> limit.capacity(10)
            .refillGreedy(1, Duration.ofSeconds(1)))
            .build();
        dbBucket = Bucket.builder()
            .addLimit(limit -> limit.capacity(10)
            .refillGreedy(1, Duration.ofSeconds(1)))
            .build();
    }

    public boolean tryConsumeWs(long tokens) {
        return wsBucket.tryConsume(tokens);
    }
    public boolean tryConsumeDb(long tokens) {
        return dbBucket.tryConsume(tokens);
    }

    public void recordResponseTime(long responseTime) {
        long currentTime = System.currentTimeMillis();
        synchronized (this) {
            if (currentTime - lastResetTime > Duration.ofMinutes(1).toMillis()) {
                // Check if no threshold was exceeded in the past minute
                if (thresholdCount.get() == 0) {
                    resetRate();  // Reset to initial rate if no threshold exceeded
                }
                thresholdCount.set(0);
                lastResetTime = currentTime;
            }
        }

        long threshold = 1000; // response time threshold in milliseconds
        if (responseTime > threshold) {
            int count = thresholdCount.incrementAndGet();
            if (count >= 6) {
                adjustRate(0.5); // Reduce rate if the threshold is crossed 6 times in a minute
                thresholdCount.set(0); // Reset count after adjustment
            }
        }
    }
    
    public void adjustRate(double rate) {
        lock.lock();
        try {
            int baseCapacity = 10;  // Base tokens to calculate the new rate
            int newCapacity = (int) (baseCapacity * rate);
            // Create a new bucket with adjusted rate limits
            wsBucket = Bucket.builder()
            .addLimit(limit -> limit.capacity(newCapacity)
            .refillGreedy(1, Duration.ofSeconds(1)))
            .build();
            dbBucket = Bucket.builder()
            .addLimit(limit -> limit.capacity(newCapacity)
            .refillGreedy(1, Duration.ofSeconds(1)))
            .build();
        } finally {
            lock.unlock();
        }
    }

    private void resetRate() {
        lock.lock();
        try {
            wsBucket = Bucket.builder()
            .addLimit(limit -> limit.capacity(10)
            .refillGreedy(1, Duration.ofSeconds(1)))
            .build();
        dbBucket = Bucket.builder()
            .addLimit(limit -> limit.capacity(10)
            .refillGreedy(1, Duration.ofSeconds(1)))
            .build();
        } finally {
            lock.unlock();
        }
    }
}
