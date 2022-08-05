package org.neo4j.graphql.examples.graphqlspringboot;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;


public class SendRequest {

    public static void main(String[] args) {
        String query = readFile(args[0]);
        JSONObject jo = sendQuery(query);
        System.out.println(jo.toString(4));
    }


    public static JSONObject sendQuery(String query) {
        JSONObject jo = new JSONObject();
        jo.put("query", query);
        return sendPostRequest(jo);
    }

    public static String readFile(String path) {
        String query = null;
        try {
            File myObj = new File(path);
            Scanner myReader = new Scanner(myObj);
            query = Files.readString(Path.of(path));
            myReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return query;
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
