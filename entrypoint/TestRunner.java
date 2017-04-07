import com.sforce.soap.apex.*;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TestRunner {

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
        System.out.println(result);
        assert (result.isSuccess());
    }
}
