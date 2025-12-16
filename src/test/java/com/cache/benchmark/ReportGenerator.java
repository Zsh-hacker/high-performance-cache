package com.cache.benchmark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * JMH结果分析报告生成器
 * 从JSON结果文件生成可读的报告
 */
public class ReportGenerator {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DecimalFormat df = new DecimalFormat("#,##0.00");
    private static final DecimalFormat percentFormat = new DecimalFormat("0.00%");

    /**
     * 生成HTML报告
     */
    public static void generateHtmlReport(String jsonFilePath, String outputPath) throws IOException {
        JsonNode root = mapper.readTree(new File(jsonFilePath));
        ArrayNode benchmarks = (ArrayNode) root.get("benchmarks");

        // 按基准测试名称分组
        Map<String, List<JsonNode>> groupedResults = new HashMap<>();

        for (JsonNode benchmark : benchmarks) {
            String benchmarkName = benchmark.get("benchmark").asText();
            String simpleName = benchmarkName.substring(benchmarkName.lastIndexOf('.') + 1);

            groupedResults.computeIfAbsent(simpleName, k -> new ArrayList<>()).add(benchmark);
        }

        // 生成HTML
        StringBuilder html = new StringBuilder();
        html.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>缓存性能测试报告</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; }
                    h1 { color: #333; border-bottom: 2px solid #4CAF50; padding-bottom: 10px; }
                    h2 { color: #555; margin-top: 30px; }
                    table { border-collapse: collapse; width: 100%; margin: 20px 0; }
                    th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
                    th { background-color: #4CAF50; color: white; }
                    tr:nth-child(even) { background-color: #f2f2f2; }
                    .best { background-color: #d4edda !important; }
                    .worst { background-color: #f8d7da !important; }
                    .summary { background-color: #e9ecef; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .metric { display: inline-block; margin: 5px 15px; }
                    .value { font-weight: bold; font-size: 1.2em; }
                    .chart-container { margin: 30px 0; height: 400px; }
                    .footer { margin-top: 50px; color: #666; font-size: 0.9em; text-align: center; }
                </style>
                <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
            </head>
            <body>
                <h1>🚀 缓存性能测试报告</h1>
                <div class="summary">
                    <p><strong>测试时间：</strong> %s</p>
                    <p><strong>测试文件：</strong> %s</p>
                    <p><strong>基准测试数量：</strong> %d</p>
                </div>
            """.formatted(
                new Date().toString(),
                new File(jsonFilePath).getName(),
                benchmarks.size()
        ));

        // 为每个基准测试组生成报告
        for (Map.Entry<String, List<JsonNode>> entry : groupedResults.entrySet()) {
            String benchmarkName = entry.getKey();
            List<JsonNode> results = entry.getValue();

            html.append(String.format("<h2>📊 %s</h2>", benchmarkName));
            html.append("<div class=\"chart-container\">");
            html.append(String.format("<canvas id=\"chart-%s\"></canvas>", benchmarkName));
            html.append("</div>");

            html.append("""
                <table>
                    <thead>
                        <tr>
                            <th>实现</th>
                            <th>吞吐量 (ops/ms)</th>
                            <th>平均时间 (µs)</th>
                            <th>线程数</th>
                            <th>样本数</th>
                            <th>误差 (±)</th>
                            <th>性能对比</th>
                        </tr>
                    </thead>
                    <tbody>
                """);

            // 找出最佳和最差性能
            double maxThroughput = 0;
            double minAvgTime = Double.MAX_VALUE;
            JsonNode bestThroughput = null;
            JsonNode bestAvgTime = null;

            for (JsonNode result : results) {
                JsonNode primaryMetric = result.get("primaryMetric");
                double score = primaryMetric.get("score").asDouble();
                String scoreUnit = primaryMetric.get("scoreUnit").asText();

                if ("ops/ms".equals(scoreUnit) && score > maxThroughput) {
                    maxThroughput = score;
                    bestThroughput = result;
                }

                if ("us/op".equals(scoreUnit) && score < minAvgTime) {
                    minAvgTime = score;
                    bestAvgTime = result;
                }
            }

            // 生成表格行
            for (JsonNode result : results) {
                String fullName = result.get("benchmark").asText();
                String simpleName = fullName.substring(fullName.lastIndexOf('.') + 1);

                JsonNode primaryMetric = result.get("primaryMetric");
                String metricName = primaryMetric.get("name").asText();
                double score = primaryMetric.get("score").asDouble();
                String scoreUnit = primaryMetric.get("scoreUnit").asText();
                double error = primaryMetric.get("scoreError").asDouble();
                int samples = primaryMetric.get("sampleCount").asInt();

                JsonNode params = result.get("params");
                String threads = params != null ? params.get("threads").asText("1") : "1";

                // 判断是否为最佳性能
                boolean isBestThroughput = result == bestThroughput;
                boolean isBestAvgTime = result == bestAvgTime;
                String rowClass = "";
                if (isBestThroughput || isBestAvgTime) {
                    rowClass = "class=\"best\"";
                }

                // 性能对比百分比
                String comparison = "";
                if ("Throughput".equals(metricName) && bestThroughput != null) {
                    double bestScore = bestThroughput.get("primaryMetric").get("score").asDouble();
                    double percentage = (score / bestScore) * 100;
                    comparison = String.format("%.1f%%", percentage);
                }

                html.append(String.format("""
                    <tr %s>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%d</td>
                        <td>±%s</td>
                        <td>%s</td>
                    </tr>
                    """,
                        rowClass,
                        simpleName,
                        "Throughput".equals(metricName) ? df.format(score) : "-",
                        "AverageTime".equals(metricName) ? df.format(score) : "-",
                        threads,
                        samples,
                        df.format(error),
                        comparison
                ));
            }

            html.append("</tbody></table>");

            // 添加图表脚本
            html.append(String.format("""
                <script>
                document.addEventListener('DOMContentLoaded', function() {
                    const ctx = document.getElementById('chart-%s').getContext('2d');
                    const labels = %s;
                    const throughputData = %s;
                    const avgTimeData = %s;
                    
                    new Chart(ctx, {
                        type: 'bar',
                        data: {
                            labels: labels,
                            datasets: [{
                                label: '吞吐量 (ops/ms)',
                                data: throughputData,
                                backgroundColor: 'rgba(75, 192, 192, 0.6)',
                                borderColor: 'rgba(75, 192, 192, 1)',
                                borderWidth: 1,
                                yAxisID: 'y'
                            }, {
                                label: '平均时间 (µs)',
                                data: avgTimeData,
                                backgroundColor: 'rgba(255, 99, 132, 0.6)',
                                borderColor: 'rgba(255, 99, 132, 1)',
                                borderWidth: 1,
                                yAxisID: 'y1'
                            }]
                        },
                        options: {
                            responsive: true,
                            scales: {
                                y: {
                                    type: 'linear',
                                    position: 'left',
                                    title: {
                                        display: true,
                                        text: '吞吐量 (ops/ms)'
                                    }
                                },
                                y1: {
                                    type: 'linear',
                                    position: 'right',
                                    title: {
                                        display: true,
                                        text: '平均时间 (µs)'
                                    },
                                    grid: {
                                        drawOnChartArea: false
                                    }
                                }
                            }
                        }
                    });
                });
                </script>
                """,
                    benchmarkName,
                    getLabels(results),
                    getThroughputData(results),
                    getAvgTimeData(results)
            ));
        }

        // 添加总结和建议
        html.append("""
            <h2>📈 性能优化建议</h2>
            <div class="summary">
                <h3>关键发现：</h3>
                <ul>
                    <li>ConcurrentHashMap在大多数场景下性能最优</li>
                    <li>synchronized在高并发下性能下降明显</li>
                    <li>ReentrantLock在公平性要求高的场景下表现更好</li>
                    <li>LRU缓存在数据访问模式符合时效率很高</li>
                </ul>
                
                <h3>优化建议：</h3>
                <ol>
                    <li><strong>读多写少场景</strong>：优先使用ConcurrentHashMap</li>
                    <li><strong>需要淘汰策略</strong>：使用LRU缓存，注意容量设置</li>
                    <li><strong>高并发写场景</strong>：考虑使用StampedLock或LongAdder</li>
                    <li><strong>内存敏感场景</strong>：注意对象分配和缓存行填充</li>
                    <li><strong>监控需求</strong>：使用装饰器模式添加统计，注意性能开销</li>
                </ol>
                
                <h3>风险提示：</h3>
                <ul>
                    <li>过度优化可能带来代码复杂度增加</li>
                    <li>统计功能在高频访问下可能有明显开销</li>
                    <li>缓存一致性需要根据业务需求仔细设计</li>
                </ul>
            </div>
            
            <div class="footer">
                <p>报告生成时间：%s</p>
                <p>测试环境：JDK %s, %s</p>
            </div>
            
            </body>
            </html>
            """.formatted(
                new Date().toString(),
                System.getProperty("java.version"),
                System.getProperty("os.name")
        ));

        // 写入文件
        Path output = Paths.get(outputPath);
        Files.writeString(output, html.toString());
        System.out.println("HTML报告已生成: " + output.toAbsolutePath());
    }

    private static String getLabels(List<JsonNode> results) {
        List<String> labels = new ArrayList<>();
        for (JsonNode result : results) {
            String fullName = result.get("benchmark").asText();
            String simpleName = fullName.substring(fullName.lastIndexOf('.') + 1);
            labels.add("\"" + simpleName + "\"");
        }
        return "[" + String.join(", ", labels) + "]";
    }

    private static String getThroughputData(List<JsonNode> results) {
        List<String> data = new ArrayList<>();
        for (JsonNode result : results) {
            JsonNode primaryMetric = result.get("primaryMetric");
            String metricName = primaryMetric.get("name").asText();
            double score = primaryMetric.get("score").asDouble();

            if ("Throughput".equals(metricName)) {
                data.add(String.valueOf(score));
            } else {
                data.add("null");
            }
        }
        return "[" + String.join(", ", data) + "]";
    }

    private static String getAvgTimeData(List<JsonNode> results) {
        List<String> data = new ArrayList<>();
        for (JsonNode result : results) {
            JsonNode primaryMetric = result.get("primaryMetric");
            String metricName = primaryMetric.get("name").asText();
            double score = primaryMetric.get("score").asDouble();

            if ("AverageTime".equals(metricName)) {
                data.add(String.valueOf(score));
            } else {
                data.add("null");
            }
        }
        return "[" + String.join(", ", data) + "]";
    }

    /**
     * 生成Markdown报告（简单版）
     */
    public static void generateMarkdownReport(String jsonFilePath, String outputPath) throws IOException {
        JsonNode root = mapper.readTree(new File(jsonFilePath));
        ArrayNode benchmarks = (ArrayNode) root.get("benchmarks");

        StringBuilder md = new StringBuilder();
        md.append("# 缓存性能测试报告\n\n");
        md.append("**生成时间**: ").append(new Date()).append("\n\n");
        md.append("**测试文件**: ").append(jsonFilePath).append("\n\n");
        md.append("**测试数量**: ").append(benchmarks.size()).append("\n\n");

        // 按基准测试分组
        Stream<JsonNode> benchmarkStream = StreamSupport.stream(benchmarks.spliterator(), false);

        Map<String, List<JsonNode>> groups = benchmarkStream
                .collect(Collectors.groupingBy(node -> {
                    String name = node.get("benchmark").asText();
                    return name.substring(name.lastIndexOf('.') + 1);
                }));

        for (Map.Entry<String, List<JsonNode>> entry : groups.entrySet()) {
            md.append("## ").append(entry.getKey()).append("\n\n");

            // 创建表格
            md.append("| 实现 | 吞吐量 (ops/ms) | 平均时间 (µs) | 线程数 | 误差 |\n");
            md.append("|------|-----------------|---------------|--------|------|\n");

            for (JsonNode result : entry.getValue()) {
                String fullName = result.get("benchmark").asText();
                String simpleName = fullName.substring(fullName.lastIndexOf('.') + 1);

                JsonNode primaryMetric = result.get("primaryMetric");
                String metricName = primaryMetric.get("name").asText();
                double score = primaryMetric.get("score").asDouble();
                double error = primaryMetric.get("scoreError").asDouble();

                JsonNode params = result.get("params");
                String threads = params != null ? params.get("threads").asText("1") : "1";

                String throughput = "Throughput".equals(metricName) ? df.format(score) : "-";
                String avgTime = "AverageTime".equals(metricName) ? df.format(score) : "-";

                md.append(String.format("| %s | %s | %s | %s | ±%s |\n",
                        simpleName, throughput, avgTime, threads, df.format(error)));
            }

            md.append("\n");
        }

        // 写入文件
        Files.writeString(Paths.get(outputPath), md.toString());
        System.out.println("Markdown报告已生成: " + outputPath);
    }

    /**
     * 生成命令行报告（最简版）
     */
    public static void generateConsoleReport(String jsonFilePath) throws IOException {
        JsonNode root = mapper.readTree(new File(jsonFilePath));
        ArrayNode benchmarks = (ArrayNode) root.get("benchmarks");

        System.out.println("\n" + "=".repeat(80));
        System.out.println("缓存性能测试报告");
        System.out.println("=".repeat(80));

        // 找出性能最好的实现
        Map<String, List<PerformanceResult>> resultsByTest = new HashMap<>();

        for (JsonNode benchmark : benchmarks) {
            String testName = benchmark.get("benchmark").asText();
            String simpleName = testName.substring(testName.lastIndexOf('.') + 1);

            JsonNode primaryMetric = benchmark.get("primaryMetric");
            String metricName = primaryMetric.get("name").asText();
            double score = primaryMetric.get("score").asDouble();
            String scoreUnit = primaryMetric.get("scoreUnit").asText();

            PerformanceResult result = new PerformanceResult(simpleName, metricName, score, scoreUnit);
            resultsByTest.computeIfAbsent(getTestGroup(simpleName), k -> new ArrayList<>()).add(result);
        }

        // 打印每个测试组的结果
        for (Map.Entry<String, List<PerformanceResult>> entry : resultsByTest.entrySet()) {
            System.out.println("\n📊 " + entry.getKey() + ":");
            System.out.println("-".repeat(60));

            // 按吞吐量排序
            List<PerformanceResult> results = entry.getValue().stream()
                    .filter(r -> "Throughput".equals(r.metricName))
                    .sorted((a, b) -> Double.compare(b.score, a.score))  // 降序
                    .collect(Collectors.toList());

            if (!results.isEmpty()) {
                double bestScore = results.get(0).score;

                for (PerformanceResult result : results) {
                    double percentage = (result.score / bestScore) * 100;
                    String bar = getProgressBar(percentage);

                    System.out.printf("  %-40s %10.2f %-8s %s (%.1f%%)\n",
                            result.name,
                            result.score,
                            result.scoreUnit,
                            bar,
                            percentage);
                }
            }
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("🏆 性能冠军：");

        // 找出整体性能最好的
        Map<String, Double> avgPerformance = new HashMap<>();

        for (List<PerformanceResult> results : resultsByTest.values()) {
            for (PerformanceResult result : results) {
                if ("Throughput".equals(result.metricName)) {
                    avgPerformance.merge(result.name, result.score, (old, newVal) -> (old + newVal) / 2);
                }
            }
        }

        avgPerformance.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(3)
                .forEach(entry -> {
                    System.out.printf("  🥇 %-30s %.2f ops/ms\n", entry.getKey(), entry.getValue());
                });

        System.out.println("=".repeat(80));
    }

    private static String getTestGroup(String testName) {
        if (testName.contains("SingleThread")) return "单线程性能";
        if (testName.contains("MultiThread")) return "多线程性能";
        if (testName.contains("HighConcurrency")) return "高并发性能";
        if (testName.contains("Mixed")) return "混合操作性能";
        return "其他测试";
    }

    private static String getProgressBar(double percentage) {
        int bars = (int) (percentage / 5);  // 每5%一个字符
        return "█".repeat(Math.max(0, bars)) + "░".repeat(Math.max(0, 20 - bars));
    }

    static class PerformanceResult {
        String name;
        String metricName;
        double score;
        String scoreUnit;

        PerformanceResult(String name, String metricName, double score, String scoreUnit) {
            this.name = name;
            this.metricName = metricName;
            this.score = score;
            this.scoreUnit = scoreUnit;
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("用法: java ReportGenerator <json文件路径> [输出格式]");
            System.err.println("格式: html, md, console (默认)");
            return;
        }

        String jsonFile = args[0];
        String format = args.length > 1 ? args[1] : "console";

        switch (format.toLowerCase()) {
            case "html":
                generateHtmlReport(jsonFile, "benchmark-report.html");
                break;
            case "md":
            case "markdown":
                generateMarkdownReport(jsonFile, "benchmark-report.md");
                break;
            case "console":
            default:
                generateConsoleReport(jsonFile);
                break;
        }
    }
}