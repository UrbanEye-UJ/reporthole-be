package za.co.urbaneye.reporthole.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * General-purpose async executor backed by virtual threads (Java 21+).
     * Used for fire-and-forget tasks such as sending emails. Unbounded — no
     * backpressure needed for low-volume operations.
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Bounded executor dedicated to ONNX inference background tasks.
     *
     * <p>ONNX inference is CPU-bound, so the thread count is kept low to avoid
     * saturating CPU cores. The bounded queue provides backpressure: when full,
     * {@code AbortPolicy} causes Spring to throw {@code RejectedExecutionException},
     * which the service layer maps to HTTP 503.</p>
     *
     * <p>Queue capacity is configurable via {@code inference.queue-capacity}
     * (default 100) so it can be tuned without recompiling.</p>
     */
    @Bean(name = "inferenceExecutor")
    public ThreadPoolTaskExecutor inferenceExecutor(
            @Value("${inference.queue-capacity:100}") int queueCapacity) {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(2);
        exec.setQueueCapacity(queueCapacity);
        exec.setThreadNamePrefix("inference-");
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        exec.initialize();
        return exec;
    }
}
