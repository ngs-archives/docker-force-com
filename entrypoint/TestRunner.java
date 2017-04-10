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
        request.setCheckOnly(true);
        RunTestsRequest runTestsRequest = new RunTestsRequest();
        runTestsRequest.setAllTests(true);
        request.setRunTestsRequest(runTestsRequest);

        CompileAndTestResult result = connector.compileAndTest(request);

        Integer maxNameLength = 0;
        Integer maxProblemLength = 0;
        List<Object[]> rows = new ArrayList<Object[]>();

        for (CompileClassResult cls : result.getClasses()) {
            if (cls.isSuccess()) {
                continue;
            }
            String name = cls.getName();
            String problem = cls.getProblem();
            Integer line = cls.getLine();
            Integer len = name == null ? 0 : name.length();
            if (maxNameLength < len) {
                maxNameLength = len;
            }
            len = problem == null ? 0 : problem.length();
            if (maxProblemLength < len) {
                maxProblemLength = len;
            }
            rows.add(new Object[] { name, line, problem });
        }
        if (rows.size() > 0) {
            System.out.println(ANSI_RED + "[ Compile Error ]"  + ANSI_RESET);
            printTable(rows,
                new Integer[] { maxNameLength, 5, maxProblemLength },
                new String[] { "Name", "Line", "Problem" });
        }
        if (!result.isSuccess()) {
            System.exit(-1);
        }
    }


    private static void printTable(List<Object[]> rows, Integer[] columnWidth, String [] columnNames) {
        List<String> topBarList = new ArrayList<String>();
        List<String> sepBarList = new ArrayList<String>();
        List<String> btmBarList = new ArrayList<String>();
        List<String> rowFmtList = new ArrayList<String>();
        for (Integer i = 0; i < columnWidth.length; i++) {
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
