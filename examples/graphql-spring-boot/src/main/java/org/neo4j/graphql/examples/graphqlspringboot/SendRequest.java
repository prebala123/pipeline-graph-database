package org.neo4j.graphql.examples.graphqlspringboot;

import okhttp3.*;
import org.json.JSONObject;
import java.io.*;


public class SendRequest {

    public static void main(String[] args) {
        String query = "query {\n" +
                "  stages {\n" +
                "    name\n" +
                "    refId\n" +
                "  }\n" +
                "}";
        JSONObject jo = sendQuery(query);
        System.out.println(jo.toString(4));
    }


    public static JSONObject sendQuery(String query) {
        JSONObject jo = new JSONObject();
        jo.put("query", query);
        return sendPostRequest(jo);
    }


    public static JSONObject sendPostRequest(JSONObject jo) {
        try {
            final MediaType JSON
                    = MediaType.parse("application/json; charset=utf-8");

            RequestBody postBody = RequestBody.create(jo.toString(), JSON);
            Request request = new Request.Builder()
                    .url("http://localhost:8080/graphql")
                    .addHeader("Content-Type", "application/json")
                    .post(postBody)
                    .build();

            OkHttpClient client = new OkHttpClient();
            Call call = client.newCall(request);
            Response response = call.execute();


            return new JSONObject(response.body().string());
        } catch (IOException e) {
            return null;
        }
    }
}
