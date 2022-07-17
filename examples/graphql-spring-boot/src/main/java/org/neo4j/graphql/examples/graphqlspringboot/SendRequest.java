package org.neo4j.graphql.examples.graphqlspringboot;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;


public class SendRequest {
    public static void main(String[] args) {
        addData();
    }

    static String path = "C:/Users/rebal/Documents/Pipeline/pipeline.json";

    public static void addData() {
        JSONObject jo2 = readJSON();
        addPipeline(jo2);
        addTrigger(jo2);
        addStages(jo2);
        addPrevStages(jo2);
    }

    public static void addPipeline(JSONObject jo2) {
        Iterator<String> it = jo2.keys();
        ArrayList<String> lst = new ArrayList<>();
        while (it.hasNext()) {
            String k = it.next();
            if (k.equals("systemNotifications") || k.equals("trigger") || k.equals("initialConfig") || k.equals("stages")
                    || k.equals("notifications") || k.equals("authentication")) {
                continue;
            }
            else if (k.equals("startTime") || k.equals("endTime") || k.equals("buildTime") || k.equals("canceled")
                    || k.equals("limitConcurrent") || k.equals("keepWaitingPipelines")) {
                lst.add(k+": "+jo2.get(k)+"");
            }
            else {
                lst.add(k + ": \"" + jo2.get(k) + "\"");
            }
        }
        String fields = String.join(", ", lst);
        String mutation = "mutation {\n" +
                "  createPipeline(input: {"+fields+"}) {\n" +
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
            Iterator<String> it = stages.getJSONObject(i).keys();
            ArrayList<String> lst = new ArrayList<>();
            while (it.hasNext()) {
                String k = it.next();
                if (k.equals("outputs") || k.equals("tasks") || k.equals("context") || k.equals("requisiteStageRefIds")) {
                    continue;
                }
                else if (k.equals("startTime") || k.equals("endTime")) {
                    lst.add(k+": "+stages.getJSONObject(i).get(k)+"");
                }
                else {
                    lst.add(k + ": \"" + stages.getJSONObject(i).get(k) + "\"");
                }
            }
            String fields = String.join(", ", lst);
            String mutation = "mutation {\n" +
                    "  createStages(input: {" + fields + "}) {\n" +
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
        //System.out.println(jo2.getJSONObject("outputs").keySet());
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

    public static void outputJSON() {
        JSONObject jo = readJSON();
        String path2 = "C:/Users/rebal/Documents/Pipeline/pipeline2.txt";
        try (PrintWriter out = new PrintWriter(new FileWriter(path2))){
            JSONObject results = whenSendPostRequest_thenCorrect(jo);
            //System.out.println(results);
            //results.write(out);
            out.write(jo.toString(4));
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
