import com.sforce.soap.apex.*;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TestRunner {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_REGEX = "\\u001B\\[[0-9;]*m";
    public static final String SUCCESS_ICON = ANSI_GREEN + "✔" + ANSI_RESET;
    public static final String FAILURE_ICON = ANSI_RED + "✘" + ANSI_RESET;

    public static void main(String[] args) throws ConnectionException, IOException {

        ConnectorConfig config = new ConnectorConfig();
        Map<String, String> env = System.getenv();

        String apiVersion = env.get("API_VERSION");
        String[] versionParts = apiVersion.split(Pattern.quote("."));
        apiVersion = versionParts[0] + "." + versionParts[1];
        String server = env.get("SF_SERVER");
        config.setAuthEndpoint(server + "/services/Soap/c/" + apiVersion);

        config.setUsername(env.get("SF_USERNAME"));
        config.setPassword(env.get("SF_PASSWORD"));

        EnterpriseConnection p = new EnterpriseConnection(config);

        config.setServiceEndpoint(server + "/services/Soap/s/" + apiVersion);

        SoapConnection connector = new SoapConnection(config);

        CompileAndTestRequest request = new CompileAndTestRequest();

        List<String> classSources = new ArrayList<String>();
        List<String> triggerSources = new ArrayList<String>();

        try(Stream<Path> stream = Files.find(Paths.get("/src/"), Integer.MAX_VALUE, (path, attr) -> {
          String strPath = path.toString();
          return strPath.endsWith(".cls") || strPath.endsWith(".trigger");
        })){
          stream.forEach(path -> {
            try {
                byte[] encoded = Files.readAllBytes(path);
                String src = new String(encoded, StandardCharsets.UTF_8);
                if (path.toString().endsWith(".cls")) {
                  classSources.add(src);
                } else {
                  triggerSources.add(src);
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
          });
        }
        String[] classAr = new String[classSources.size()];
        String[] triggerAr = new String[triggerSources.size()];
        classAr = classSources.toArray(classAr);
        triggerAr = triggerSources.toArray(triggerAr);
        request.setClasses(classAr);
        request.setTriggers(triggerAr);
        request.setCheckOnly(!"1".equals(env.get("DEPLOY")));
        RunTestsRequest runTestsRequest = new RunTestsRequest();
        runTestsRequest.setAllTests(true);
        request.setRunTestsRequest(runTestsRequest);

        CompileAndTestResult result = connector.compileAndTest(request);

        List<Object[]> rows = new ArrayList<Object[]>();

        for (CompileClassResult res : result.getClasses()) {
            String name = res.getName();
            if (name == null) {
              name = "";
            }
            String problem = res.getProblem();
            if (problem == null) {
              problem = "";
            }
            String status = res.isSuccess() ? SUCCESS_ICON : FAILURE_ICON;
            Integer line = res.getLine();
            rows.add(new Object[] { status, name, line == -1 ? "" : line, problem });
        }
        if (rows.size() > 0) {
            System.out.println("[ Compile Classes ]");
            printTable(rows, new String[] { " ", "Name", "Line", "Problem" });
        }

        rows = new ArrayList<Object[]>();
        for (CompileTriggerResult res : result.getTriggers()) {
            String name = res.getName();
            if (name == null) {
              name = "";
            }
            String problem = res.getProblem();
            if (problem == null) {
              problem = "";
            }
            String status = res.isSuccess() ? SUCCESS_ICON : FAILURE_ICON;
            Integer line = res.getLine();
            rows.add(new Object[] { status, name, line == -1 ? "" : line, problem });
        }
        if (rows.size() > 0) {
            System.out.println("[ Compile Triggers ]");
            printTable(rows, new String[] { " ", "Name", "Line", "Problem" });
        }

        rows = new ArrayList<Object[]>();
        List<String> stackTraces = new ArrayList<String>();
        RunTestsResult testResult = result.getRunTestsResult();
        for (RunTestSuccess res : testResult.getSuccesses()) {
            String name = getFullClassName(res.getNamespace(), res.getName());
            String methodName = res.getMethodName();
            String time = String.format("%.3f sec", res.getTime() / 1000);
            rows.add(new Object[] { SUCCESS_ICON, time, name, methodName });
        }
        for (RunTestFailure res : testResult.getFailures()) {
            String name = getFullClassName(res.getNamespace(), res.getName());
            String methodName = res.getMethodName();
            String time = String.format("%.3f sec", res.getTime() / 1000);
            rows.add(new Object[] { FAILURE_ICON, time, name, methodName });
            stackTraces.add(String.format("%s#%s:\n%s%s\n%s%s\n",
                  name, methodName, ANSI_RED,
                  res.getMessage(), "  " + res.getStackTrace().replaceAll("\n", "\n  "),
                  ANSI_RESET));
        }
        if (rows.size() > 0) {
            System.out.println("[ Test Result ]");
            printTable(rows, new String[] { " ", "Duration", "Class", "Method" });
        }
        if (stackTraces.size() > 0) {
            System.out.println("\n[ Stacktrace ]\n" + String.join("\n", stackTraces));
        }
        rows = new ArrayList<Object[]>();
        for (CodeCoverageResult res : testResult.getCodeCoverage()) {
            String name = getFullClassName(res.getNamespace(), res.getName());
            String type = res.getType();
            Integer ttl = res.getNumLocations();
            Integer cov = ttl - res.getNumLocationsNotCovered();
            String lines = String.format("%d/%d", cov, ttl);
            String par = String.format("%.2f", (double) cov / (double) ttl * 100) + "%";
            rows.add(new Object[] { type, name, lines, par });
        }
        if (rows.size() > 0) {
            System.out.println("[ Test Coverage ]");
            printTable(rows, new String[] { "Type", "Name", "Lines", "Coverage" });
        }
        if (result.isSuccess()) {
            System.out.println(String.format("%sSucceeded. %d tests ran. Total time %.3f sec%s", ANSI_GREEN, testResult.getNumTestsRun(), testResult.getTotalTime() / 1000, ANSI_RESET));
        } else {
            System.out.println(String.format("%sFailed. %d tests ran. %d failed. Total time %.3f sec%s", ANSI_RED, testResult.getNumTestsRun(), testResult.getNumFailures(), testResult.getTotalTime() / 1000, ANSI_RESET));
            System.exit(1);
        }
    }

    private static String getFullClassName(String namespace, String className) {
        if (namespace == null) {
            return className;
        }
        return String.format("%s.%s", namespace, className);
    }

    private static void printTable(List<Object[]> rows, String [] columnNames) {
        if (rows.size() == 0) {
            return;
        }
        Integer[] columnWidth = new Integer[rows.get(0).length];
        for (Integer i = 0; i < columnNames.length; i++) {
            columnWidth[i] = columnNames[i].replaceAll(ANSI_REGEX, "").length();
        }
        for (Object[] row : rows) {
            for (Integer i = 0; i < row.length; i++) {
                String str = String.format("%s", row[i]).replaceAll(ANSI_REGEX, "");
                Integer len = str.length();
                if (columnWidth[i] < len) {
                    columnWidth[i] = len;
                }
            }
        }
        List<String> topBarList = new ArrayList<String>();
        List<String> sepBarList = new ArrayList<String>();
        List<String> btmBarList = new ArrayList<String>();
        List<String> rowFmtList = new ArrayList<String>();
        for (Integer i = 0; i < rows.get(0).length; i++) {
            Integer w = columnWidth[i];
            String bar = String.join("", Collections.nCopies(w, "─"));
            topBarList.add(bar);
            sepBarList.add(bar);
            btmBarList.add(bar);
            rowFmtList.add("%" + Integer.toString(i + 1) + "$-" + w + "s");
        }
        String rowFmt = "│ " + String.join(" │ ", rowFmtList) + " │";
        List<String> out = new ArrayList<String>();
        out.add("┌─" + String.join("─┬─", topBarList) + "─┐");
        out.add(String.format(rowFmt, (Object[]) columnNames));
        out.add("├─" + String.join("─┼─", sepBarList) + "─┤");
        for (Object[] row : rows) {
          out.add(String.format(rowFmt, row));
        }
        out.add("└─" + String.join("─┴─", btmBarList) + "─┘");
        System.out.println(String.join("\n", out));
    }
}
