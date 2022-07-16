import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
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
        addTrigger(jo2);
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

    public static void addTrigger(JSONObject jo2) {
        String id = (String) jo2.getJSONObject("trigger").get("executionId");
        String mutation = "mutation {\n" +
                "  createTrigger(input: {executionId: \""+id+"\"}) {\n" +
                "    executionId\n" +
                "  }\n" +
                "}";
        JSONObject jo = new JSONObject();
        jo.put("query", mutation);
        try {
            whenSendPostRequest_thenCorrect(jo);
        }
        catch (IOException ignored) {}

        String id2 = (String) jo2.get("id");
        mutation = "mutation {\n" +
                "  createPipelineTrigger(input: {s1: \""+id+"\", s2: \""+id2+"\"}) {\n" +
                "    id\n" +
                "  }\n" +
                "}";
        jo = new JSONObject();
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
            addOutputs(stages.getJSONObject(i), id);
        }
    }

    public static void addPrevStages(JSONObject jo2) {
        JSONArray stages = jo2.getJSONArray("stages");
        for (int i = 0; i < stages.length(); i++) {
            String id = (String) stages.getJSONObject(i).get("refId");
            JSONArray prevs = (JSONArray) stages.getJSONObject(i).get("requisiteStageRefIds");
            if (prevs.length() == 0) {
                String mutation = "mutation {\n" +
                        "  createStageTrigger(input: {s1: \""+jo2.get("id")+"\", s2: \""+id+"\"}) {\n" +
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
            for (int j = 0; j < prevs.length(); j++) {
                String mutation = "mutation {\n" +
                        "  createStageTimeline(input: {s1: \""+prevs.get(j)+"\", s2: \""+id+"\"}) {\n" +
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
    }

    public static void addOutputs(JSONObject jo2, String id) {
        String jobNumber = "unknown";
        try {
            jobNumber = (String) jo2.getJSONObject("outputs").get("JobNumber");
        } catch (JSONException ignored) {

        }
        System.out.println(jo2.getJSONObject("outputs").keySet());
        if (!jobNumber.equals("unknown")) {
            String mutation = "mutation {\n" +
                    "  createOutputs(input: {JobNumber: \"" + jobNumber + "\"}) {\n" +
                    "    JobNumber\n" +
                    "  }\n" +
                    "}";
            JSONObject jo = new JSONObject();
            jo.put("query", mutation);
            try {
                whenSendPostRequest_thenCorrect(jo);
            } catch (IOException ignored) {
            }
            connectOutputs(jobNumber, id);
        }
    }

    public static void connectOutputs(String jobNumber, String id) {
        String mutation = "mutation {\n" +
                "  createStageOutputs(input: {s1: \""+id+"\", s2: \""+jobNumber+"\"}) {\n" +
                "    refId\n" +
                "  }\n" +
                "}";
        JSONObject jo = new JSONObject();
        jo.put("query", mutation);
        try {
            whenSendPostRequest_thenCorrect(jo);
        }
        catch (IOException ignored) {}
    }

    public static JSONObject readJSON() {
        JSONObject jo = null;
        try {
            File myObj = new File(path);
            Scanner myReader = new Scanner(myObj);
            String allJson = "";
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                allJson += data;
            }
            jo = new JSONObject(allJson);
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
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

        RequestBody postBody = RequestBody.create(jo.toString(), JSON);
        Request request = new Request.Builder()
                .url("http://localhost:8080/graphql")
                .addHeader("Content-Type", "application/json")
                .post(postBody)
                .build();

        OkHttpClient client = new OkHttpClient();
        Call call = client.newCall(request);
        Response response = call.execute();


        JSONObject results = new JSONObject(response.body().string());
        return results;
    }
}
