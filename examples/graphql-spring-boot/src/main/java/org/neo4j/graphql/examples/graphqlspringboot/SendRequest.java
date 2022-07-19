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

    static String path = "/Users/rajeevbommana/Desktop/pipeline.json";

    public static void addData() {
        JSONObject jo2 = readJSON();
        addPipeline(jo2);
        addTrigger(jo2);
        addStages(jo2);
        addPrevStages(jo2);
        addContext(jo2);
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
        whenSendPostRequest_thenCorrect(jo);
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
        whenSendPostRequest_thenCorrect(jo);

        String id2 = (String) jo2.get("id");
        mutation = "mutation {\n" +
                "  createPipelineTrigger(input: {s1: \""+id+"\", s2: \""+id2+"\"}) {\n" +
                "    id\n" +
                "  }\n" +
                "}";
        jo = new JSONObject();
        jo.put("query", mutation);
        whenSendPostRequest_thenCorrect(jo);
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
            whenSendPostRequest_thenCorrect(jo);
            addOutputs(stages.getJSONObject(i).getJSONObject("outputs"), id);

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
                whenSendPostRequest_thenCorrect(jo);
            }
            for (int j = 0; j < prevs.length(); j++) {
                String mutation = "mutation {\n" +
                        "  createStageTimeline(input: {s1: \""+prevs.get(j)+"\", s2: \""+id+"\"}) {\n" +
                        "    refId\n" +
                        "  }\n" +
                        "}";
                JSONObject jo = new JSONObject();
                jo.put("query", mutation);
                whenSendPostRequest_thenCorrect(jo);
            }
        }
    }

    public static void addOutputs(JSONObject jo2, String id) {
        //gets all fields of the object
        Iterator<String> it = jo2.keys();
        ArrayList<String> lst = new ArrayList<>();
        //iterates through fields
        while (it.hasNext()) {
            String k = it.next();
            //unhandled fields for now
            if (k.equals("buildInfo") || k.equals("propertyFileContents")
                    || k.equals("outputs.manifestNamesByNamespace")
                    || k.equals("manifests")
                    || k.equals("outputs.manifests") || k.equals("manifest")
                    || k.equals("resolvedExpectedArtifacts") || k.equals("jobStatus") || k.equals("completionDetails")
                    || k.equals("trigger_json")) {
                continue;
            }
            //artifact outputs
            else if (k.equals("outputs.createdArtifacts") || k.equals("outputs.boundArtifacts")
                    || k.equals("optionalArtifacts")  || k.equals("artifacts") || k.equals("requiredArtifacts")) {
                JSONArray artifacts = jo2.getJSONArray(k);
                k = k.replace("outputs.", "");
                //go to addArtifacts for code
                for (int i = 0; i < artifacts.length(); i++)
                    addArtifacts(artifacts.getJSONObject(i), id, k);
            }
            //non String fields
            else if (k.equals("startTime") || k.equals("overallScore")) {
                lst.add(k+": "+jo2.get(k)+"");
            }
            //String fields
            else {
                lst.add(k + ": \"" + jo2.get(k) + "\"");
            }
        }
        lst.add("refId: \"" + id + "\"");
        String fields = String.join(", ", lst);
        //creates graphql mutation
        String mutation = "mutation {\n" +
                "  createOutputs(input: {" + fields + "}) {\n" +
                "    __typename\n" +
                "  }\n" +
                "}";
        JSONObject jo = new JSONObject();
        jo.put("query", mutation);
        whenSendPostRequest_thenCorrect(jo);
    }

    public static void addArtifacts(JSONObject jo2, String id, String type) {
        //gets fields of the artifact
        Iterator<String> it = jo2.keys();
        ArrayList<String> lst = new ArrayList<>();
        //iterates through fields
        while (it.hasNext()) {
            String k = it.next();
            //incomplete for now
            if (k.equals("metadata")) {
                continue;
            }
            //non String type
            else if (k.equals("customKind")) {
                lst.add(k+": "+jo2.get(k)+"");
            }
            //String type
            else {
                lst.add(k + ": \"" + jo2.get(k) + "\"");
            }
        }
        lst.add("refId: \"" + id + "\"");
        String fields = String.join(", ", lst);
        //mutation changes based on type of artifact
        String mutation = "mutation {\n" +
                "  "+type+"(input: {" + fields + "}) {\n" +
                "    __typename\n" +
                "  }\n" +
                "}";
        JSONObject jo = new JSONObject();
        jo.put("query", mutation);
        whenSendPostRequest_thenCorrect(jo);
    }

    public static void addContext(JSONObject jo2) {

            JSONArray curr = jo2.getJSONArray("stages");
            ArrayList<String> holder = new ArrayList<>();
            ArrayList<String> keys = new ArrayList<>();
            //keys.add("buildNumber");
            keys.add("propertyFile");
            //keys.add("consecutiveErrors");
            String adder = "";


            for(int i = 0; i < curr.length(); i++) {
                String id = "";

                //need to decide how to update schema in order to include  or exclude certain fields
                /*
                for(int j = 0; j < keys.size(); j++) {
                    try {
                        adder = (String) curr.getJSONObject(i).getJSONObject("context").get(keys.get(j));
                    }
                    catch (JSONException e){}

                    if(!adder.isBlank()) {

                        holder.add(keys.get(j)+": "+adder+"");


                    }

                }

                 */


                try {
                    id = (String) curr.getJSONObject(i).get("refId");
                }
                catch (JSONException e){}
                holder.add("refId: \"" + id + "\"");
                String fields = String.join(", ", holder);

                String main = "refId: \"" + id + "\"";

                String mutation = "mutation {\n" +
                        "  createContext(input: {" + fields+ "}) {\n" +
                        "    refId\n" +
                        "  }\n" +
                        "}";
                JSONObject jo = new JSONObject();
                jo.put("query", mutation);
                whenSendPostRequest_thenCorrect(jo);









            }

        for(int m = curr.length()-1; m >= 0; m--){

            String currid = "";
            try {
                currid = (String)curr.getJSONObject(m).get("refId");

            }
            catch (JSONException e){}

            JSONArray endpoints = (JSONArray) curr.getJSONObject(m).get("requisiteStageRefIds");

            for(int v = 0; v < endpoints.length(); v++){

                String mutation = "mutation {\n" +
                        "  ContextMerger(input: {c1: \""+endpoints.get(v)+"\", s1: \""+currid+"\"}) {\n" +
                        "    refId\n" +
                        "  }\n" +
                        "}";
                JSONObject jo = new JSONObject();
                jo.put("query", mutation);
                whenSendPostRequest_thenCorrect(jo);

            }




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
            //out.write(results.toString(4));
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

    public static JSONObject whenSendPostRequest_thenCorrect(JSONObject jo) {
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


            JSONObject results = new JSONObject(response.body().string());
            return results;
        } catch (IOException e) {
            return null;
        }
    }
}
