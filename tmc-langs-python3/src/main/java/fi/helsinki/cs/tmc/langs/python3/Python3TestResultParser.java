package fi.helsinki.cs.tmc.langs.python3;

import fi.helsinki.cs.tmc.langs.domain.RunResult;
import fi.helsinki.cs.tmc.langs.domain.TestResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class Python3TestResultParser {
    private static Path RESULT_FILE = Paths.get(".tmc_test_results.json");

    private Path path;
    private ObjectMapper mapper;

    public Python3TestResultParser(Path path) {
        this.path = path;
        this.mapper = new ObjectMapper();
    }

    /**
     * Parses the test results from the result file.
     *
     * @return Test run results.
     */
    public RunResult parse() throws IOException {
        List<TestResult> testResults = getTestResults();

        RunResult.Status status = RunResult.Status.PASSED;
        for (TestResult result : testResults) {
            if (!result.isSuccessful()) {
                status = RunResult.Status.TESTS_FAILED;
            }
        }

        ImmutableList<TestResult> immutableResults = ImmutableList.copyOf(testResults);
        ImmutableMap<String, byte[]> logs = ImmutableMap.copyOf(new HashMap<String, byte[]>());
        return new RunResult(status, immutableResults, logs);
    }

    private List<TestResult> getTestResults() throws IOException {
        String json = String.join("", Files.readAllLines(
                path.resolve(RESULT_FILE), Charset.defaultCharset()));
        List<TestResult> results = new ArrayList<>();

        JsonNode tree = mapper.readTree(json);
        for (JsonNode node : tree) {
            results.add(toTestResult(node));
        }

        return results;
    }

    private TestResult toTestResult(JsonNode node) {
        List<String> points = new ArrayList<>();
        for (JsonNode point : node.get("points")) {
            points.add(point.asText());
        }

        List<String> backTrace = new ArrayList<>();
        for (JsonNode line : node.get("backtrace")) {
            backTrace.add(line.asText());
        }

        return new TestResult(
                node.get("name").asText(),
                node.get("passed").asBoolean(),
                ImmutableList.copyOf(points),
                node.get("message").asText(),
                ImmutableList.copyOf(backTrace));
    }
}
