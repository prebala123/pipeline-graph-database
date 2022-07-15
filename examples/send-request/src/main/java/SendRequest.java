import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.*;
import java.rmi.server.RemoteRef;
import java.util.Objects;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;


public class SendRequest {
    public static void main(String[] args) {
        addData();
    }

    static String path = "C:/Users/rebal/pipeline/pipelinedb/JSONfiles/pipeline.json";

    public static void addData() {
        JSONObject jo2 = readJSON();
        addPipeline(jo2);
        addStages(jo2);
        addPrevStages(jo2);
    }

    public static void addPipeline(JSONObject jo2) {
        String id = (String) jo2.get("id");
        String mutation = "mutation {\n" +
                "  createPipeline(input: {id: \""+id+"\"}) {\n" +
                "    id\n" +
                "  }\n" +
                "}";
        JSONObject jo = new JSONObject();
        jo.put("query", mutation);
        try {
            whenSendPostRequest_thenCorrect(jo);
        }
        catch (IOException ignored) {}
    }

    public static void addStages(JSONObject jo2) {
        JSONArray stages = jo2.getJSONArray("stages");
        for (int i = 0; i < stages.length(); i++) {
            String id = (String) stages.getJSONObject(i).get("refId");
            String mutation = "mutation {\n" +
                    "  createStages(input: {refId: \"" + id + "\"}) {\n" +
                    "    refId\n" +
                    "  }\n" +
                    "}";
            JSONObject jo = new JSONObject();
            jo.put("query", mutation);
            try {
                whenSendPostRequest_thenCorrect(jo);
            } catch (IOException ignored) {
            }
        }
    }

    public static void addPrevStages(JSONObject jo2) {
        JSONArray stages = jo2.getJSONArray("stages");
        for (int i = 0; i < stages.length(); i++) {
            String id = (String) stages.getJSONObject(i).get("refId");
            System.out.println(stages.getJSONObject(i).get("requisiteStageRefIds"));
            JSONArray temp = (JSONArray) stages.getJSONObject(i).get("requisiteStageRefIds");
            /*String[] prevs = (String[]) stages.getJSONObject(i).get("requisiteStageRefIds");
            for (int j = 0; j < prevs.length; j++) {
                String mutation = "mutation {\n" +
                        "  createStageTimeline(input: {s1: \""+id+"\", s2: \""+prevs[j]+"\"}) {\n" +
                        "    refId\n" +
                        "  }\n" +
                        "}";
                System.out.println(mutation);
                JSONObject jo = new JSONObject();
                jo.put("query", mutation);
                try {
                    whenSendPostRequest_thenCorrect(jo);
                } catch (IOException ignored) {
                }
            }*/
        }
    }

    public static JSONObject readJSON() {
        JSONObject jo = null;
        try {
            File myObj = new File(path);
            Scanner myReader = new Scanner(myObj);
            String allJson = "";
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                //System.out.println(data);
                allJson += data;
            }
            //allJson += "]}";
            //System.out.println(allJson);
            jo = new JSONObject(allJson);
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        //System.out.println(jo);
        return jo;
    }

    public static void sendAnyQuery(String query) {
        JSONObject jo = new JSONObject();
        jo.put("query", query);
        //System.out.println(jo);
        try (PrintWriter out = new PrintWriter(new FileWriter(path))){
            JSONObject results = whenSendPostRequest_thenCorrect(jo);
            //System.out.println(results);
            //results.write(out);
            out.write(results.toString(4));
        }
        catch (IOException ignored) {

        }
    }

    public static JSONObject whenSendPostRequest_thenCorrect(JSONObject jo)
            throws IOException {

        final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");

        //String queryjson = "{\"query\":\"query {\n  movies {\n    title\n  }\n}\"}";
        //System.out.println(queryjson);

        //JSONObject jo = new JSONObject();
        //jo.put("query", "query {\n  movies {\n    title\n  }\n}");

        RequestBody postBody = RequestBody.create(jo.toString(), JSON);
        Request request = new Request.Builder()
                .url("http://localhost:8080/graphql")
                .addHeader("Content-Type", "application/json")
                .post(postBody)
                .build();

        OkHttpClient client = new OkHttpClient();
        Call call = client.newCall(request);
        Response response = call.execute();
        //System.out.println(response.body().string());

        JSONObject results = new JSONObject(response.body().string());
        return results;
        //System.out.println(results.getJSONObject("data").getJSONArray("movies"));
        /*JSONArray data = results.getJSONObject("data").getJSONArray("movies");
        for (int i = 0; i < data.length(); i++) {
            System.out.println(data.getJSONObject(i).get("title"));
        }*/

        //assertThat(response.code(), equalTo(200));
    }
}
