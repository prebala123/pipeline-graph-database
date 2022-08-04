package org.neo4j.graphql.examples.graphqlspringboot.datafetcher;

import com.vimalselvam.graphql.GraphqlTemplate;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpServerErrorException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.okhttp3.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;

import org.testng.Assert;



@SpringBootTest
@AutoConfigureMockMvc
public class GraphqlTest {

         private static final OkHttpClient client = new OkHttpClient();
         String graphqlUri = "http://localhost:8080/graphql";


        private Response prepareResponse(String graphqlPayload) throws IOException {
            RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), graphqlPayload);
            Request request = new Request.Builder().url(graphqlUri).post(body).build();
            return client.newCall(request).execute();



        }

    @Test
    public void testQ3GraphqlWithNoVariables() throws IOException {



            JSONObject temp = QueryBuilder("query{\n" +
                    "  \n" +
                    "stages(where:{AND:{startTime_gte: 1654613866953, endTime_lte: 1654613946777}}){\n" +
                    "  \n" +
                    "  \n" +
                    "  \n" +
                    "  outputs{\n" +
                    "    \n" +
                    "  artifacts{\n" +
                    "    \n" +
                    "    name\n" +
                    "  }\n" +
                    "    \n" +
                    "    buildInfo{\n" +
                    "    \t\tartifacts{\n" +
                    "     \t name\n" +
                    "    \t}\n" +
                    "    }\n" +
                    "    \n" +
                    "    outputs_createdArtifacts{\n" +
                    "      name\n" +
                    "      version\n" +
                    "    }\n" +
                    "    outputs_boundArtifacts{\n" +
                    "      name\n" +
                    "      type\n" +
                    "    }\n" +
                    "    requiredArtifacts{\n" +
                    "      name\n" +
                    "      type\n" +
                    "    }\n" +
                    "    optionalArtifacts{\n" +
                    "      name\n" +
                    "      type\n" +
                    "    }\n" +
                    "  }\n" +
                    "  \n" +
                    "  \n" +
                    "}\n" +
                    "  \n" +
                    "}\n");

            String comp = "{\n" +
                    "  \"data\": {\n" +
                    "    \"stages\": [\n" +
                    "      {\n" +
                    "        \"outputs\": {\n" +
                    "          \"artifacts\": [],\n" +
                    "          \"buildInfo\": {\n" +
                    "            \"artifacts\": [\n" +
                    "              {\n" +
                    "                \"name\": \"Spin-canary-issuegen-build-Deploy\"\n" +
                    "              }\n" +
                    "            ]\n" +
                    "          },\n" +
                    "          \"outputs_createdArtifacts\": [],\n" +
                    "          \"outputs_boundArtifacts\": [],\n" +
                    "          \"requiredArtifacts\": [],\n" +
                    "          \"optionalArtifacts\": []\n" +
                    "        }\n" +
                    "      }\n" +
                    "    ]\n" +
                    "  }\n" +
                    "}";

            Assert.assertEquals(whenSendPostRequest_thenCorrect(temp).getJSONObject("data").getJSONArray("stages").getJSONObject(0).getJSONObject("outputs").getJSONObject("buildInfo").getJSONArray("artifacts").getJSONObject(0).get("name").toString(), "Spin-canary-issuegen-build-Deploy", String.valueOf(false));

    }

    @Test
    public void IncorrectInputTest() throws IOException {

        JSONObject temp = QueryBuilder("query{\n" +
                "  \n" +
                "  stages{\n" +
                "    \n" +
                "    length\n" +
                "  }\n" +
                "}");

        try{

          String curr = whenSendPostRequest_thenCorrect(temp).toString();
        }
        catch (HttpServerErrorException e){}


    }

@Test
public void testQ1GraphqlWithNoVariables() throws IOException {

    JSONObject temp = QueryBuilder("query{\n" +
            "  \n" +
            "\n" +
            " \n" +
            "  stages(where:{context:{parameters:{imageIds:\"quay.io/opsmxpublic/canary-issuegen:issue-canary-gen-1265\"}}}){\n" +
            "    \n" +
            "    context{\n" +
            "      \n" +
            "      parameters{\n" +
            "        \n" +
            "        connectors{\n" +
            "          \n" +
            "         values{\n" +
            "          jira_ticket_no\n" +
            "        }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "  \n" +
            "  \n" +
            "\n" +
            "\n" +
            "  \n" +
            "  \n" +
            "  \n" +
            "  \n" +
            "}");

    String comp = "{\n" +
            "  \"data\": {\n" +
            "    \"stages\": [\n" +
            "      {\n" +
            "        \"context\": {\n" +
            "          \"parameters\": {\n" +
            "            \"connectors\": [\n" +
            "              {\n" +
            "                \"values\": [\n" +
            "                  {\n" +
            "                    \"jira_ticket_no\": null\n" +
            "                  }\n" +
            "                ]\n" +
            "              },\n" +
            "              {\n" +
            "                \"values\": [\n" +
            "                  {\n" +
            "                    \"jira_ticket_no\": null\n" +
            "                  }\n" +
            "                ]\n" +
            "              },\n" +
            "              {\n" +
            "                \"values\": [\n" +
            "                  {\n" +
            "                    \"jira_ticket_no\": \"ENG-519\"\n" +
            "                  }\n" +
            "                ]\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

    JSONAssert.assertEquals(whenSendPostRequest_thenCorrect(temp), new JSONObject(comp), false);

}



    public static JSONObject whenSendPostRequest_thenCorrect(JSONObject jo) {
        try {
            final okhttp3.MediaType JSON
                    = okhttp3.MediaType.parse("application/json; charset=utf-8");

            okhttp3.RequestBody postBody = okhttp3.RequestBody.create(jo.toString(), JSON);
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("http://localhost:8080/graphql")
                    .addHeader("Content-Type", "application/json")
                    .post(postBody)
                    .build();

            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.Call call = client.newCall(request);
            okhttp3.Response response = call.execute();


            JSONObject results = new JSONObject(response.body().string());
            return results;
        } catch (IOException e) {
            return null;
        }
    }



    public static JSONObject QueryBuilder(String query) {
        JSONObject jo = new JSONObject();
        jo.put("query", query);

        return  jo;


}
}
