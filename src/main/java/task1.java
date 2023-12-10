import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

class StatsCollector {
    private final ConcurrentHashMap<String, List<Double>> metricsData;
    private final ExecutorService executorService;

    public StatsCollector(int numberOfThreads) {
        this.metricsData = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(numberOfThreads);
    }

    public void push(String metricName, double[] data) {
        metricsData.compute(metricName, (key, existingData) -> {
            if (existingData == null) {
                existingData = new ArrayList<>();
            }
            for (double value : data) {
                existingData.add(value);
            }
            return existingData;
        });
    }

    public List<MetricStats> stats() throws InterruptedException, ExecutionException {
        List<Future<MetricStats>> futures = new ArrayList<>();

        for (String metricName : metricsData.keySet()) {
            Future<MetricStats> future = executorService.submit(() -> calculateStats(metricName));
            futures.add(future);
        }

        List<MetricStats> result = new ArrayList<>();

        for (Future<MetricStats> future : futures) {
            result.add(future.get());
        }

        return result;
    }

    private MetricStats calculateStats(String metricName) {
        List<Double> data = metricsData.get(metricName);

        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (double value : data) {
            sum += value;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        double average = sum / data.size();

        return new MetricStats(metricName, sum, average, min, max);
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        StatsCollector collector = new StatsCollector(5);

        collector.push("metric_name", new double[]{0.1, 0.05, 1.4, 5.1, 0.3});

        List<MetricStats> stats = collector.stats();

        for (MetricStats metric : stats) {
            System.out.println(metric);
        }

        collector.shutdown();
    }
}

class MetricStats {
    private final String metricName;
    private final double sum;
    private final double average;
    private final double min;
    private final double max;

    public MetricStats(String metricName, double sum, double average, double min, double max) {
        this.metricName = metricName;
        this.sum = sum;
        this.average = average;
        this.min = min;
        this.max = max;
    }

    @Override
    public String toString() {
        return "MetricStats{" +
                "metricName='" + metricName + '\'' +
                ", sum=" + sum +
                ", average=" + average +
                ", min=" + min +
                ", max=" + max +
                '}';
    }
}
