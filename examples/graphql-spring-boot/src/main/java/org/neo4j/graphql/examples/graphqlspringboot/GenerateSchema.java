package org.neo4j.graphql.examples.graphqlspringboot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GenerateSchema {

    static HashMap<String, HashSet<String>> otherFields = new HashMap<>();

    public static void main(String[] args) {
        //add relationships and fields first
        addFields();
        makeSchema();
    }

    public static void addFields() {
        //add our own fields not defined by JSON
        addOtherData("stages", "nextStage: stages @relation(name: \"NEXT\", direction: OUT)");
        addOtherData("trigger", "nextStage: [stages] @relation(name: \"NEXT\", direction: OUT)");
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
        return new String[]{"NEXT", "OUT"};
    }

    public static void makeSchema() {
        Object obj = readJSON2();
        JSONObject jo = new JSONObject();
        HashMap<String, HashSet<String>> fieldTypes;
        HashMap<String, HashSet<String>> schema = new HashMap<>();
        if (obj instanceof JSONObject) {
            jo = (JSONObject) obj;
            fieldTypes = separateAllFields(jo);
            readPipeline(jo, "Pipeline", schema, fieldTypes);
        } else {
            JSONArray arr = (JSONArray) obj;
            jo.put("Pipeline", arr);
            fieldTypes = separateAllFields(jo);
            readPipeline(jo, "AllPipelines", schema, fieldTypes);
        }
        String finalSchema = "";
        String queries = "type Query {\n    ";
        for (HashMap.Entry<String, HashSet<String>> entry2 : schema.entrySet()) {
            String key2 = entry2.getKey();
            if (key2.equals("expressionEvaluationSummary") || key2.charAt(0) == '{' || key2.charAt(0) == '#')
                continue;
            HashSet<String> value2 = entry2.getValue();
            finalSchema += "type " + cleanType(key2) + " {\n    ";
            queries += cleanType(key2) + ": [" + cleanType(key2) + "]\n    ";
            if (otherFields.containsKey(key2)) {
                for (String s : otherFields.get(key2))
                    finalSchema += s + "\n    ";
            }
            if (value2.size() == 0) {
                finalSchema += "id: String\n    ";
            }
            for (String s : value2) {
                String dataType = getKeyByValue(fieldTypes, s);
                if (dataType.equals("Object")) {
                    String[] info = getRelationInfo(key2, s);
                    dataType = cleanFields(s) + " @relation(name: \"" + info[0] + "\", direction: " + info[1] + ")";
                }
                if (dataType.equals("Array")) {
                    String[] info = getRelationInfo(key2, s);
                    dataType = "[" + cleanFields(s) + "] @relation(name: \"" + info[0] + "\", direction: " + info[1] + ")";
                }
                if (dataType.equals("StringArr") || dataType.equals("Other"))
                    dataType = "[String]";
                if (s.equals("expressionEvaluationSummary"))
                    dataType = "String";
                finalSchema += cleanFields(s) + ": " + dataType + "\n    ";
            }
            finalSchema += "\n}\n\n";
        }
        queries += "\n}";
        finalSchema += queries;
        toFile(finalSchema);
    }

    public static String getKeyByValue(HashMap<String, HashSet<String>> map, String value) {
        HashSet<String> keys = new HashSet<>();
        for (HashMap.Entry<String, HashSet<String>> entry : map.entrySet()) {
            if (entry.getValue().contains(value)) {
                keys.add(entry.getKey());
            }
        }
        if (keys.contains("String"))
            return "String";
        else
            return keys.iterator().next();
    }

    public static String cleanType(String s) {
        s = s.replace(".", "_");
        return s;
    }

    public static String cleanFields(String s) {
        ArrayList<String> replacements = new ArrayList<>(Arrays.asList(".", "/", "-"));
        for (String i : replacements) {
            s = s.replace(i, "_");
        }
        if (Character.isDigit(s.charAt(0)))
            s = "_" + s;
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
                if (arr.length() == 0)
                    kind = "StringArr";
                for (int j = 0; j < arr.length(); j++) {
                    if (arr.get(j) instanceof JSONObject) {
                        kind = "Array";
                        separateAllFieldsInner(arr.getJSONObject(j), hm);
                    } else {
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
        File file = new File("examples/graphql-spring-boot/src/main/resources/neo4j.graphql");
        try (PrintWriter out = new PrintWriter(file)) {
            out.write(schema);
        } catch (IOException e) {
            System.out.println("fail");
        }
    }

    public static Object readJSON2() {
        String path = GraphqlSpringBootApplication.dataPath;
        Object jo = null;
        try {
            File myObj = new File(path);
            Scanner myReader = new Scanner(myObj);
            String allJson = Files.readString(Path.of(GraphqlSpringBootApplication.dataPath));
            if (allJson.charAt(0) == '[')
                jo = new JSONArray(allJson);
            else
                jo = new JSONObject(allJson);
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        } catch (IOException e) {
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
