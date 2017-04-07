import java.util.Map;
import java.util.regex.Pattern;
import com.sforce.soap.apex.*;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

public class TestRunner {

    public static void main(String[] args) throws ConnectionException {

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

        RunTestsRequest request = new RunTestsRequest();

        request.setClasses(args);

        RunTestsResult r = connector.runTests(request);
        // TODO: compile adn test https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/sforce_api_calls_compileandtest.htm

        for(RunTestFailure t : r.getFailures()){
            System.out.println(t.getClass() + "." + t.getMethodName());
            System.out.println(t.getMessage());
            System.out.println(t.getStackTrace());
            System.out.println("*****************");
        }
    }
}
