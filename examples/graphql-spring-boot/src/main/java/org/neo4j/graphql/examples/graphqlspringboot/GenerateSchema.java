package org.neo4j.graphql.examples.graphqlspringboot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

public class GenerateSchema {

    static HashMap<String, String[]> info = new HashMap<>();
    static HashMap<String, HashSet<String>> otherFields = new HashMap<>();

    static String schemaFile = "C:/Users/rebal/pipeline/pipelinedb/examples/graphql-spring-boot/src/main/java/org/neo4j/graphql/examples/graphqlspringboot/PipelineSchema.txt";

    public static void main(String[] args) {
        //add relationships and fields first
        addRelationships();
        addFields();
        makeSchema();
    }

    public static void addRelationships() {
        //add all relationships here
        addRelationInfo("stages", "outputs", "OUTPUTS", "OUT");
        addRelationInfo("stages", "context", "CONTEXTUALIZES", "OUT");
        addRelationInfo("stages", "tasks", "REQUIRED_TASKS", "OUT");
        addRelationInfo("stages", "status", "STATUS", "OUT");
    }

    public static void addFields() {
        //add our own fields not defined by JSON
        addOtherData("stages", "nextStage: stages @relation(name: \"NEXT_STAGE\", direction: OUT)");
    }

    public static void addRelationInfo(String node, String field, String relationship, String direction) {
        info.put(node + "->" + field, new String[]{relationship, direction});
    }

    public static void addOtherData(String node, String field) {
        if (otherFields.containsKey(node))
            otherFields.get(node).add(field);
        else {
            HashSet<String> hs = new HashSet<>();
            hs.add(field);
            otherFields.put(node, hs);
        }
    }

    public static String[] getRelationInfo(String node, String field) {
        if (info.containsKey(node + "->" + field))
            return info.get(node + "->" + field);
        else
            return new String[]{"", "IN"};
    }

    public static void makeSchema() {
        JSONObject jo = readJSON();
        HashMap<String, HashSet<String>> fieldTypes = separateAllFields(jo);
        HashMap<String, HashSet<String>> schema = new HashMap<>();
        readPipeline(jo, "Pipeline", schema, fieldTypes);
        String finalSchema = "";
        for (HashMap.Entry<String, HashSet<String>> entry2 : schema.entrySet()) {
            String key2 = entry2.getKey();
            HashSet<String> value2 = entry2.getValue();
            finalSchema += "type " + cleanType(key2) + " {\n    ";
            if (otherFields.containsKey(key2)){
                for (String s: otherFields.get(key2))
                    finalSchema += s + "\n    ";
            }
            for (String s : value2) {
                String dataType = getKeyByValue(fieldTypes, s);
                if (dataType.equals("Object")) {
                    String[] info = getRelationInfo(key2, s);
                    dataType = cleanFields(s) + " @relation(name: \""+info[0]+"\", direction: "+info[1]+")";
                }
                if (dataType.equals("Array")) {
                    String[] info = getRelationInfo(key2, s);
                    dataType = "[" + cleanFields(s) + "] @relation(name: \""+info[0]+"\", direction: "+info[1]+")";
                }
                if (dataType.equals("StringArr") || dataType.equals("Other"))
                    dataType = "[String]";
                finalSchema += cleanFields(s) + ": " + dataType + "\n    ";
            }
            finalSchema += "\n}\n\n";
        }
        toFile(finalSchema);
    }

    public static String getKeyByValue(HashMap<String, HashSet<String>> map, String value) {
        for (HashMap.Entry<String, HashSet<String>> entry : map.entrySet()) {
            if (entry.getValue().contains(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static String cleanType(String s) {
        s = s.replace(".", "_");
        return s;
    }

    public static String cleanFields(String s) {
        ArrayList<String> replacements = new ArrayList<>(Arrays.asList(".", "/", "-"));
        for (String i: replacements) {
            s = s.replace(i, "_");
        }
        return s;
    }

    public static HashMap<String, HashSet<String>> separateAllFields(JSONObject jo) {
        HashMap<String, HashSet<String>> hm = new HashMap<>();
        hm.put("Object", new HashSet<>());
        hm.put("Array", new HashSet<>());
        hm.put("String", new HashSet<>());
        hm.put("Boolean", new HashSet<>());
        hm.put("Float", new HashSet<>());
        hm.put("Other", new HashSet<>());
        hm.put("StringArr", new HashSet<>());
        separateAllFieldsInner(jo, hm);
        return hm;
    }

    public static void separateAllFieldsInner(JSONObject jo, HashMap<String, HashSet<String>> hm) {
        String kind = "Other";
        if (jo.names() == null)
            return;
        for (int i = 0; i < jo.names().length(); i++) {
            Object ob = jo.get(jo.names().getString(i));
            if (ob instanceof JSONObject) {
                kind = "Object";
                separateAllFieldsInner(jo.getJSONObject(jo.names().getString(i)), hm);
            } else if (ob instanceof JSONArray) {
                JSONArray arr = jo.getJSONArray(jo.names().getString(i));
                for (int j = 0; j < arr.length(); j++) {
                    if (arr.get(j) instanceof JSONObject) {
                        kind = "Array";
                        separateAllFieldsInner(arr.getJSONObject(j), hm);
                    }
                    else {
                        kind = "StringArr";
                    }
                }
            } else if (ob instanceof String) {
                kind = "String";
            } else if (ob instanceof Boolean) {
                kind = "Boolean";
            } else if ((ob instanceof Long) || (ob instanceof Integer) || (ob instanceof Double)) {
                kind = "Float";
            } else {
                kind = "Other";
            }
            hm.get(kind).add(jo.names().getString(i));
        }
    }

    public static void toFile(String schema) {
        try (PrintWriter out = new PrintWriter(new FileWriter(schemaFile))){
            out.write(schema);
        }
        catch (IOException ignored) {

        }
    }

    public static JSONObject readJSON() {
        String path = SendRequest.path;
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

    public static void readPipeline(JSONObject jo, String type, HashMap<String, HashSet<String>> schema, HashMap<String, HashSet<String>> hm) {
        Iterator<String> it = jo.keys();
        HashSet<String> lst = new HashSet<>();
        type = type.replace(".", "_");
        if (schema.containsKey(type)) {
            lst = schema.get(type);
        }
        schema.put(type, lst);

        while (it.hasNext()) {
            String k = it.next();

            Object typ = jo.get(k);
            lst.add(k);
            if (typ instanceof JSONObject)
                readPipeline(jo.getJSONObject(k), k, schema, hm);
            if (typ instanceof JSONArray) {
                JSONArray arr = jo.getJSONArray(k);
                for (int i = 0; i < arr.length(); i++) {
                    if (arr.get(i) instanceof JSONObject) {
                        readPipeline(arr.getJSONObject(i), k, schema, hm);
                    }
                }
            }
        }
    }
}
