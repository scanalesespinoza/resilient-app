# Resilient Apps When Databases Slow Down

## The Problem Nobody Talks About Until It Happens

Applications might run smoothly for months, but databases eventually have issues. Imagine one day the database starts responding slowly. Maybe it's a spike in traffic. Maybe a missing index. Maybe the disk is full. Whatever the reason, a database that used to respond in 10 milliseconds now takes 3 seconds.

What happens next is predictable and painful:

1. Requests pile up waiting for the database
2. Your application threads get exhausted
3. New requests can't be processed
4. Your entire system crashes
5. Even healthy parts of your application become unreachable

This is called **cascading failure**. A slow database doesn't just slow down your app—it kills it entirely.

```
Normal operation:

    User Request -----> Application -----> Database
                 <-----             <-----
                   Fast response (10ms)


Database slowdown without protection:

    Request 1 -----> Application -----> Database
    Request 2 ----->      |               (slow...)
    Request 3 ----->      |               (waiting...)
    Request 4 ----->      |               (still waiting...)
    Request 5 ----->   OVERLOAD           (3 seconds...)
         ...              CRASH!
```

The application might be perfectly healthy. The database is the problem. But without protection, the healthy application becomes collateral damage.

## The Key Insight: Protect the Healthy Parts

Here's the mental shift that makes resilient apps possible:

**A slow response is worse than no response.**

When your database is slow, the best thing your application can do is *stop asking it questions*. This sounds counterintuitive. Users want data! But consider the alternative: if you keep hammering a struggling database, you guarantee that:

1. The database gets even slower (more load)
2. Your application crashes (thread exhaustion)
3. Nobody gets anything

By temporarily refusing some requests, you achieve something better:

1. The database gets breathing room
2. Your application stays alive
3. Some users still get served
4. You can show helpful error messages to others

This is the foundation of the **Adaptive Rate Limiting** pattern.

## What Is Rate Limiting?

Before we dive deeper, let's make sure we understand rate limiting.

Rate limiting is simply controlling how many operations can happen in a given time period. Think of it like a nightclub bouncer. The club has a capacity. When it's full, new people have to wait outside, even if they really want to get in.

```
Without rate limiting:

    Request 1 -----> Database
    Request 2 -----> Database
    Request 3 -----> Database    (Database overwhelmed)
    Request 4 -----> Database
    Request 5 -----> Database


With rate limiting (max 3 per second):

    Request 1 -----> Database
    Request 2 -----> Database
    Request 3 -----> Database
    Request 4 -----> WAIT or REJECT
    Request 5 -----> WAIT or REJECT
```

Rate limiting protects your system from being overwhelmed. But traditional rate limiting has a fixed limit. You decide "10 requests per second" and that's it forever.

The problem? That fixed limit might be too high when your database is struggling, and too low when your database is healthy.

## Adaptive Rate Limiting: The Smart Bouncer

Adaptive rate limiting is rate limiting that adjusts itself based on how the system is performing.

Imagine our nightclub bouncer notices that people inside are having trouble moving around. The dance floor is too crowded. A smart bouncer would temporarily reduce the capacity, letting fewer people in until the crowd thins out.

That's exactly what adaptive rate limiting does:

```
Normal conditions:

    Bouncer: "Capacity is 100. Let them in!"
    
    Request -----> Check limit (100 available) -----> Database (fast)


Database slowing down:

    Bouncer: "Database is slow. Reducing capacity to 50."
    
    Request -----> Check limit (50 available) -----> Database (slow)
    Request -----> Check limit (limit reached!) -----> "Please try later"


Database recovered:

    Bouncer: "Database is fast again. Restoring capacity to 100."
    
    Request -----> Check limit (100 available) -----> Database (fast)
```

The key components are:

1. **Measure performance**: Track how long database operations take
2. **Detect degradation**: Notice when response times exceed acceptable thresholds
3. **Reduce load**: Lower the rate limit to give the database relief
4. **Recover automatically**: Restore normal limits when performance improves

## The Token Bucket: A Simple Mental Model

To implement rate limiting, we need a mechanism to control the flow of requests. The most intuitive one is called the **Token Bucket**.

Imagine a bucket that holds tokens. Every request needs one token to proceed. If the bucket is empty, the request must wait or be rejected.

The bucket refills over time. If the bucket can hold 10 tokens and refills 1 token per second, you get a rate limit of "10 requests initially, then 1 per second sustained."

```
Token Bucket visualization:

    [Start]
    Bucket: [T][T][T][T][T][T][T][T][T][T]  (10 tokens)
    
    [Request arrives, takes 1 token]
    Bucket: [T][T][T][T][T][T][T][T][T][ ]  (9 tokens)
    
    [1 second passes, 1 token refills]
    Bucket: [T][T][T][T][T][T][T][T][T][T]  (10 tokens)
    
    [10 requests arrive at once]
    Bucket: [ ][ ][ ][ ][ ][ ][ ][ ][ ][ ]  (0 tokens)
    
    [11th request arrives]
    Response: "Sorry, try again later" (no tokens available)
```

The beauty of the token bucket is that it handles bursts gracefully. If nobody has made requests for a while, the bucket is full and can handle a sudden spike. But sustained high traffic will empty the bucket and enforce the rate limit.

## Pseudocode: The Core Pattern

Let's see how this all comes together. Here's the pattern in pseudocode:

```
TokenBucket:
    capacity = 10          # Maximum tokens
    tokens = 10            # Current tokens
    refill_rate = 1        # Tokens added per second
    last_refill = now()

    function try_consume():
        refill_tokens()
        if tokens >= 1:
            tokens = tokens - 1
            return true    # Request allowed
        else:
            return false   # Request denied

    function refill_tokens():
        time_passed = now() - last_refill
        tokens_to_add = time_passed * refill_rate
        tokens = min(capacity, tokens + tokens_to_add)
        last_refill = now()
```

Now, let's add the adaptive behavior:

```
AdaptiveRateLimiter:
    bucket = TokenBucket(capacity=10, refill_rate=1)
    threshold = 1000ms     # Max acceptable response time
    violation_count = 0    # Times threshold was exceeded
    check_window = 60s     # Time window for counting violations
    last_check = now()

    function record_response_time(response_time):
        # Reset counter every minute
        if now() - last_check > check_window:
            if violation_count == 0:
                restore_normal_rate()
            violation_count = 0
            last_check = now()
        
        # Count slow responses
        if response_time > threshold:
            violation_count = violation_count + 1
            
            # If too many slow responses, reduce the rate
            if violation_count >= 6:
                reduce_rate(factor=0.5)
                violation_count = 0

    function reduce_rate(factor):
        bucket.capacity = bucket.capacity * factor
        bucket.tokens = min(bucket.tokens, bucket.capacity)

    function restore_normal_rate():
        bucket.capacity = 10
```

And here's how it's used in your application:

```
function handle_request(request):
    # First, check if we have capacity
    if not rate_limiter.try_consume():
        return error("Too many requests. Please try again later.")
    
    # Execute the database operation and measure time
    start_time = now()
    result = database.query(request)
    response_time = now() - start_time
    
    # Record the response time for adaptive adjustment
    rate_limiter.record_response_time(response_time)
    
    return result
```

## Why This Works: The Feedback Loop

The pattern creates a self-regulating feedback loop:

```
Feedback loop:

    Slow Database
         |
         v
    High Response Times Detected
         |
         v
    Rate Limit Reduced
         |
         v
    Fewer Requests to Database
         |
         v
    Database Gets Relief
         |
         v
    Response Times Improve
         |
         v
    Rate Limit Restored
         |
         v
    Normal Operation
```

This feedback loop is the heart of the pattern. Without it, you have two bad options:

1. **No rate limiting**: System crashes during database slowdowns
2. **Fixed rate limiting**: You either limit too much (wasting capacity) or too little (not protecting enough)

With adaptive rate limiting, you get the best of both worlds: maximum throughput during normal operation, and automatic protection during degradation.

## The Two Layers of Protection

In the reference implementation, there are actually two rate limiting layers:

```
Two-layer protection:

    User Request
         |
         v
    [Web Service Rate Limiter]  "Can we accept this request?"
         |
         v
    [Database Rate Limiter]     "Can we query the database?"
         |
         v
    Database
```

Why two layers?

**Layer 1 (Web Service)**: Protects your application's ability to respond at all. Even if the database is struggling, your application can still return error messages, serve cached content, or handle requests that don't need the database.

**Layer 2 (Database)**: Protects the database specifically. This is where you measure response times and make adaptive adjustments.

Think of it like a hospital during a crisis:

- Layer 1 (Front desk): "We can only admit 100 patients per hour"
- Layer 2 (Operating rooms): "We can only perform 5 surgeries per hour"

The front desk capacity doesn't depend on the operating rooms. Patients can still be admitted for other reasons. But the operating room capacity directly affects which surgeries happen.

## Graceful Degradation vs. Total Failure

The goal of this pattern is **graceful degradation**. When something goes wrong, your system should get worse gradually, not fail completely.

```
Without protection (Total Failure):

    Database at 100% health: All users served
    Database at 50% health:  ALL USERS FAIL (cascade)
    Database at 0% health:   ALL USERS FAIL


With protection (Graceful Degradation):

    Database at 100% health: All users served
    Database at 50% health:  50% of users served, 50% get "try later"
    Database at 0% health:   0% of users served, 100% get "try later"
```

In the second scenario, your application never crashes. Users who can't be served get a clear message. The moment the database recovers, everything returns to normal automatically.

## Thread Safety: A Critical Detail

When you're adjusting rate limits, multiple threads might be trying to:

1. Check the current limit
2. Consume tokens
3. Adjust the limit

This can cause race conditions where the state becomes inconsistent. The solution is to use **locks** or **atomic operations**.

```
Thread-safe rate adjustment:

    function reduce_rate(factor):
        acquire_lock()
        try:
            bucket.capacity = bucket.capacity * factor
            bucket.tokens = min(bucket.tokens, bucket.capacity)
        finally:
            release_lock()
```

The lock ensures that only one thread can modify the rate limit at a time. Other threads wait their turn. This prevents situations where two threads both try to reduce the rate simultaneously and end up with incorrect values.

For simple counters (like counting violations), **atomic operations** are often sufficient:

```
Atomic counter:

    violation_count = AtomicInteger(0)
    
    # This is thread-safe without explicit locks
    violation_count.increment()
    current_value = violation_count.get()
```

## Health Checks: Knowing When You're Down

Rate limiting handles the "slow database" scenario. But what about when the database is completely unreachable?

**Health checks** complement rate limiting by providing visibility into system state:

```
Health check:

    function check_database_health():
        try:
            database.execute("SELECT 1")  # Simplest possible query
            return HEALTHY
        except:
            return UNHEALTHY
```

Health checks serve multiple purposes:

1. **Load balancers** use them to route traffic away from unhealthy instances
2. **Orchestrators** (like Kubernetes) use them to restart unhealthy containers
3. **Monitoring systems** use them to alert operators
4. **Your application** can use them to fail fast instead of waiting for timeouts

```
Request handling with health awareness:

    function handle_request(request):
        if not is_database_healthy():
            return error("Service temporarily unavailable")
        
        # Proceed with normal rate-limited flow
        if not rate_limiter.try_consume():
            return error("Too many requests")
        
        return database.query(request)
```

The difference is subtle but important:

- **Rate limiting**: "We're accepting limited requests because the database is slow"
- **Health check failure**: "We're not accepting any database requests because the database is unreachable"

## Periodic Reconnection: Staying Hopeful

When your database goes down, how do you know when it comes back? You need to keep checking, but not too aggressively.

```
Reconnection checker (runs every 10 seconds):

    function check_connection():
        try:
            database.execute("SELECT 1")
            log("Database is ONLINE")
        except:
            log("Database is OFFLINE")
```

This simple pattern:

1. Runs on a schedule (every 10 seconds)
2. Tests the connection with a trivial query
3. Logs the result for monitoring

When the database comes back online, your application discovers it automatically. No manual intervention required.

## Choosing Your Thresholds

The pattern requires you to choose several values:

| Threshold | Purpose | Guidance |
|-----------|---------|----------|
| Response time threshold | When to consider a response "slow" | Set to 2-3x your normal p95 response time |
| Violation count | How many slow responses before acting | 3-10, depending on how sensitive you want to be |
| Check window | Time period for counting violations | 30-60 seconds works well for most systems |
| Rate reduction factor | How much to reduce capacity | 0.5 (half) is a good starting point |
| Token capacity | Maximum concurrent operations | Based on your database connection pool and expected load |
| Refill rate | How quickly capacity recovers | Match to your normal sustained throughput |

There's no universal right answer. These values depend on your specific system, traffic patterns, and tolerance for errors.

**Start conservative** (more aggressive rate limiting) and relax the values as you gain confidence.

## Implementation Tips

### Tip 1: Measure First

Before adding rate limiting, instrument your application to measure database response times. You need baseline data to set meaningful thresholds.

### Tip 2: Log Rate Limit Events

When rate limiting kicks in, log it. When it recovers, log it. This gives you visibility into how often protection is activating.

```
function reduce_rate(factor):
    log("ALERT: Reducing rate limit due to slow database responses")
    # ... actual reduction logic
```

### Tip 3: Return Helpful Errors

Don't just return "Error". Tell users what's happening:

```
Good: "Our system is experiencing high load. Please try again in a few seconds."
Bad:  "Error 503"
```

### Tip 4: Consider Separate Limits for Different Operations

Not all database operations are equal. A simple lookup might be fine while a complex report query is too expensive. Consider different rate limits for different operation types:

```
rate_limiters = {
    "simple_read": TokenBucket(capacity=100),
    "complex_query": TokenBucket(capacity=10),
    "write": TokenBucket(capacity=50)
}
```

### Tip 5: Test Your Resilience

Deliberately slow down your database in a test environment. Verify that:

1. Rate limiting activates
2. Your application stays responsive
3. Users get appropriate error messages
4. The system recovers when the database recovers

## Common Mistakes to Avoid

### Mistake 1: Only Protecting the Database Layer

If your web server runs out of threads, it doesn't matter that your database layer has rate limiting. Protect both layers.

### Mistake 2: Setting Limits Too Low

Being too aggressive with rate limiting is almost as bad as having none. You'll reject legitimate traffic unnecessarily. Start by measuring your actual traffic patterns.

### Mistake 3: Forgetting to Restore Limits

If you reduce the rate but never restore it, your system will be permanently degraded. Always include automatic recovery logic.

### Mistake 4: Not Handling the "Limit Reached" Case

When a request is rate-limited, you need to return a proper response. Don't leave the user hanging or throw an unhandled exception.

### Mistake 5: Shared State in Distributed Systems

If you have multiple application instances, they might have separate rate limiters. This can lead to inconsistent behavior. Consider using a shared rate limiting service (like Redis) for distributed systems.

## Summary: The Pattern at a Glance

```
The Adaptive Rate Limiting Pattern:

    1. Accept request
         |
         v
    2. Check rate limit -----> If exceeded, return "Try later"
         |
         v
    3. Measure operation time
         |
         v
    4. Execute database operation
         |
         v
    5. Record response time
         |
         v
    6. If response was slow:
         |
         +-----> Count it
         |
         +-----> If too many slow responses:
                      |
                      +-----> Reduce rate limit
         |
         v
    7. If no slow responses for a while:
         |
         +-----> Restore normal rate limit
         |
         v
    8. Return result to user
```

## Conclusion

The Adaptive Rate Limiting pattern transforms how your application handles database slowdowns. Instead of crashing, your application degrades gracefully. Instead of guessing the right limits, your application adjusts automatically.

The pattern is simple in concept:

1. **Measure** how long operations take
2. **React** to degradation by reducing load
3. **Recover** automatically when things improve

This creates a self-regulating system that protects itself from cascading failures.

The next time your database has a bad day, your application doesn't have to share its fate. With adaptive rate limiting, your application can say: "I see you're struggling. Let me give you a break."

And that's resilience.

---

## Appendix: Quick Reference

### Core Components

| Component | Purpose |
|-----------|---------|
| Token Bucket | Controls request flow with refillable capacity |
| Response Time Tracker | Measures database operation duration |
| Threshold Counter | Counts violations within a time window |
| Rate Adjuster | Modifies token bucket capacity based on performance |
| Health Check | Verifies database connectivity |

### Key Pseudocode

```
# Rate limiting check
if not bucket.try_consume():
    return "Too many requests"

# Measure and record
start = now()
result = database.query()
bucket.record_response_time(now() - start)

# Adaptive adjustment
if response_time > threshold:
    violations = violations + 1
    if violations >= max_violations:
        bucket.capacity = bucket.capacity * 0.5

# Automatic recovery
if no_violations_for(60 seconds):
    bucket.capacity = original_capacity
```

### Questions to Ask Before Implementing

1. What are my current database response times (p50, p95, p99)?
2. How many concurrent database connections can I sustain?
3. What's the acceptable error rate during degradation?
4. Do I need distributed rate limiting across multiple instances?
5. What error messages will users see when rate limited?

---

*This document accompanies the [Resilient App](https://github.com/scanalesespinoza/resilient-app) reference implementation.*
