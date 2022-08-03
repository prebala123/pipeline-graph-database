package org.neo4j.graphql.examples.graphqlspringboot;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.Neo4jException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PopulateDatabase implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(PopulateDatabase.class.getName());
    private final Driver driver;

    public PopulateDatabase(String uri, String user, String password, Config config) {
        // The driver is a long living object and should be opened during the start of your application
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password), config);
    }

    @Override
    public void close() throws Exception {
        // The driver object should be closed before the application ends.
        driver.close();
    }

    public void deleteAll() {
        String finalCypher = "MATCH (a) DETACH DELETE a";

        try (Session session = driver.session(SessionConfig.forDatabase(GraphqlSpringBootApplication.database))) {
            Record record = session.writeTransaction(tx -> {
                Result result = tx.run(finalCypher);
                //return result.single();
                return null;
            });
        } catch (Neo4jException ex) {
            LOGGER.log(Level.SEVERE, finalCypher + " raised an exception", ex);
            throw ex;
        }
    }

    public static HashMap<String, HashSet<String>> separateFields(JSONObject jo) {
        HashMap<String, HashSet<String>> hm = new HashMap<>();
        hm.put("Object", new HashSet<>());
        hm.put("Array", new HashSet<>());
        hm.put("String", new HashSet<>());
        hm.put("Other", new HashSet<>());
        String kind = "";
        if (jo.names() == null)
            return hm;
        for (int i = 0; i < jo.names().length(); i++) {
            Object ob = jo.get(jo.names().getString(i));
            if (ob instanceof JSONObject) {
                kind = "Object";
            } else if (ob instanceof JSONArray) {
                kind = "Array";
            } else if (ob instanceof String){
                kind = "String";
            } else {
                kind = "Other";
            }
            hm.get(kind).add(jo.names().getString(i));
        }
        return hm;
    }

    public static JSONObject readJSON() {
        String path = GraphqlSpringBootApplication.dataPath;
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

    public void addPipeline(JSONObject jo, String type, String id, String pipelineId) {
        HashMap<String, HashSet<String>> hm = separateFields(jo);
        Iterator<String> it = jo.keys();
        Iterator<String> it2 = jo.keys();
        ArrayList<String> lst = new ArrayList<>();
        ArrayList<String> replacements = new ArrayList<>(Arrays.asList(".", "/", "-"));
        type = type.replace(".", "_");

        if (type.equals("expressionEvaluationSummary"))
            System.out.println("here");

        String cypher = "";
        if (id != null) {
            cypher += "MATCH (prev) WHERE ID(prev) = " + id + " ";
        }
        cypher += "CREATE (node:" + type + " {";

        while (it.hasNext()) {
            String k = it.next();
            //System.out.println(k);

            String k2 = new String(k.toCharArray());

            for (String i: replacements) {
                k2 = k2.replace(i, "_");
            }
            if (Character.isDigit(k2.charAt(0)))
                k2 = "_" + k2;

            if (k.equals("expressionEvaluationSummary")) {
                lst.add(k2 + ": \"" + (jo.getJSONObject(k).toString())
                        .replace("\\", "/")
                        .replace("\"", "\\\"")
                        //.replace("/", "\\")
                        + "\"");
            }

            if (hm.get("Other").contains(k)) {
                lst.add(k2+": "+jo.get(k));
            }
            else if (hm.get("String").contains(k)){
                lst.add(k2 + ": \"" + ((String) jo.get(k))
                        .replace("\\", "/")
                        .replace("\"", "\\\"")
                        //.replace("/", "\\")
                        + "\"");
            }
            else if (hm.get("Array").contains(k)) {
                JSONArray arr = jo.getJSONArray(k);
                for (int i = 0; i < arr.length(); i++) {
                    if (!(arr.get(i) instanceof JSONObject) && !(arr.get(i) instanceof JSONObject)) {
                        lst.add(k2 + ": " + jo.get(k) + "");
                    }
                }
            }
        }
        lst.add("pipelineId: \"" + pipelineId + "\"");
        String fields = String.join(", ", lst);
        cypher += fields + "})";
        if (id != null) {
            cypher += " CREATE (prev) - [:NEXT] -> (node)";
        }
        cypher += " RETURN node";

        String finalCypher = cypher;
        String nodeId = null;

        //System.out.println(finalCypher);

        try (Session session = driver.session(SessionConfig.forDatabase(GraphqlSpringBootApplication.database))) {
            Record record = session.writeTransaction(tx -> {
                Result result = tx.run(finalCypher);
                return result.single();
            });
            nodeId = record.get("node").toString();
            nodeId = nodeId.substring(nodeId.indexOf("<") + 1, nodeId.indexOf(">"));
            //System.out.println(type + ": " + nodeId.substring(nodeId.indexOf("<") + 1, nodeId.indexOf(">")));
        } catch (Neo4jException ex) {
            System.out.println(finalCypher);
            LOGGER.log(Level.SEVERE, finalCypher + " raised an exception", ex);
            throw ex;
        }

        while (it2.hasNext()) {
            String k = it2.next();
            String k2 = new String(k.toCharArray());

            for (String i: replacements) {
                k2 = k2.replace(i, "_");
            }

            if (hm.get("Object").contains(k)){
                if (k.equals("expressionEvaluationSummary"))
                    continue;
                addPipeline(jo.getJSONObject(k), k, nodeId, pipelineId);
            }
            else if (hm.get("Array").contains(k)) {
                JSONArray arr = jo.getJSONArray(k);
                for (int i = 0; i < arr.length(); i++) {
                    if (arr.get(i) instanceof JSONObject) {
                        addPipeline(arr.getJSONObject(i), k, nodeId, pipelineId);
                    }
                }
            }
        }
    }

    public void addPrevStages(JSONObject jo2, String pipelineId) {
        JSONArray stages = jo2.getJSONArray("stages");
        for (int i = 0; i < stages.length(); i++) {
            String id = (String) stages.getJSONObject(i).get("refId");
            JSONArray prevs = (JSONArray) stages.getJSONObject(i).get("requisiteStageRefIds");
            if (prevs.length() == 0) {
                String finalCypher = "MATCH (s1:trigger { executionId: \""+jo2.get("id")+"\", pipelineId: \""+pipelineId+"\"}) " +
                        "MATCH (s2:stages { refId: \""+id+"\", pipelineId: \""+pipelineId+"\"}) CREATE (s1) - [:NEXT] -> (s2) RETURN s2";

                try (Session session = driver.session(SessionConfig.forDatabase(GraphqlSpringBootApplication.database))) {
                    Record record = session.writeTransaction(tx -> {
                        Result result = tx.run(finalCypher);
                        return result.single();
                    });
                } catch (Neo4jException ex) {
                    System.out.println(finalCypher);
                    LOGGER.log(Level.SEVERE, finalCypher + " raised an exception", ex);
                    throw ex;
                }
            }
            for (int j = 0; j < prevs.length(); j++) {
                String finalCypher = "MATCH (s1:stages { refId: \""+prevs.get(j)+"\", pipelineId: \""+pipelineId+"\"}) " +
                        "MATCH (s2:stages { refId: \""+id+"\", pipelineId: \""+pipelineId+"\"}) MATCH (o1:outputs) <- [:NEXT] - (s1)  " +
                        "MATCH (c1:context) <- [:NEXT] - (s2) CREATE (s1) - [:NEXT] -> (s2) " +
                        "CREATE (o1) - [:NEXT] -> (c1) RETURN s1";

                try (Session session = driver.session(SessionConfig.forDatabase(GraphqlSpringBootApplication.database))) {
                    Record record = session.writeTransaction(tx -> {
                        Result result = tx.run(finalCypher);
                        return result.single();
                    });
                } catch (Neo4jException ex) {
                    System.out.println(finalCypher);
                    LOGGER.log(Level.SEVERE, finalCypher + " raised an exception", ex);
                    throw ex;
                }
            }
        }
    }

    public static boolean populate(JSONObject jo) {
        String uri = GraphqlSpringBootApplication.uri;
        String user = GraphqlSpringBootApplication.user;
        String password = GraphqlSpringBootApplication.password;

        try (PopulateDatabase app = new PopulateDatabase(uri, user, password, Config.defaultConfig())) {
            //app.deleteAll();
            if (jo.has("stages")) {
                app.addPipeline(jo, "Pipeline", null, jo.getString("id"));
                app.addPrevStages(jo, jo.getString("id"));
            }
            return true;
        } catch (Exception e) {
            System.out.println("fail");
            deleteWithId(jo.getString("id"));
            return false;
        }
    }

    public static void deleteWithId(String pipelineId) {
        String uri = GraphqlSpringBootApplication.uri;
        String user = GraphqlSpringBootApplication.user;
        String password = GraphqlSpringBootApplication.password;

        try (PopulateDatabase app = new PopulateDatabase(uri, user, password, Config.defaultConfig())) {
            app.deleteWithIdHelper(pipelineId);
        } catch (Exception e) {
            System.out.println("fail");
        }
    }

    public void deleteWithIdHelper(String pipelineId) {
        String finalCypher = "MATCH (a {pipelineId: \""+pipelineId+"\"}) DETACH DELETE a";

        try (Session session = driver.session(SessionConfig.forDatabase(GraphqlSpringBootApplication.database))) {
            Record record = session.writeTransaction(tx -> {
                Result result = tx.run(finalCypher);
                //return result.single();
                return null;
            });
        } catch (Neo4jException ex) {
            LOGGER.log(Level.SEVERE, finalCypher + " raised an exception", ex);
            throw ex;
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

    public static void fullPopulate() {
        int count = 0;
        boolean populated = false;
        Object jo = readJSON2();
        if (jo instanceof JSONArray) {
            for (Object obj: (JSONArray) jo) {
                count++;
                System.out.println(count);
                populated = populate((JSONObject) obj);
                if (!populated)
                    deletedPipelines.add(count);
            }
        }
        else {
            populate((JSONObject) jo);
        }
    }

    public static HashSet<Integer> deletedPipelines = new HashSet<>();

    public static void main(String[] args){
        String uri = GraphqlSpringBootApplication.uri;
        String user = GraphqlSpringBootApplication.user;
        String password = GraphqlSpringBootApplication.password;

        try (PopulateDatabase app = new PopulateDatabase(uri, user, password, Config.defaultConfig())) {
            app.deleteAll();
        } catch (Exception e) {
            System.out.println("fail");
        }
        deletedPipelines = new HashSet<>();

        fullPopulate();
        System.out.println("finished");
        System.out.println(deletedPipelines);
    }
}
