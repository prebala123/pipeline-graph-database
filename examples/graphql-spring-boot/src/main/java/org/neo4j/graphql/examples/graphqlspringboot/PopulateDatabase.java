package org.neo4j.graphql.examples.graphqlspringboot;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.Neo4jException;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
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

        try (Session session = driver.session(SessionConfig.forDatabase("neo4j"))) {
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

    public static void updateYAML() {
        Yaml yaml = new Yaml();
        String yamlString = "org :\n" +
                "  neo4j :\n" +
                "    driver :\n" +
                "      uri : " + GraphqlSpringBootApplication.uri + "\n" +
                "      authentication :\n" +
                "        username : " + GraphqlSpringBootApplication.user + "\n" +
                "        password : " + GraphqlSpringBootApplication.password + "\n" +
                "      config :\n" +
                "        encrypted : " + GraphqlSpringBootApplication.encrypted + "\n" +
                "database : " + GraphqlSpringBootApplication.database + "\n" +
                "spring :\n" +
                "  graphql :\n" +
                "    schema :\n" +
                "      printer :\n" +
                "        enabled : " + GraphqlSpringBootApplication.printer + "\n" +
                "    graphiql :\n" +
                "      enabled : " + GraphqlSpringBootApplication.graphiql + "\n";
        Map<String, Object> data = yaml.load(yamlString);
        try {
            File file = new File("examples/graphql-spring-boot/src/main/resources/application.yaml");
            PrintWriter writer = new PrintWriter(file);
            yaml.dump(data, writer);
        } catch (IOException e) {
            System.out.println("fail");
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

    public void addPipeline(JSONObject jo, String type, String id) {
        HashMap<String, HashSet<String>> hm = separateFields(jo);
        Iterator<String> it = jo.keys();
        Iterator<String> it2 = jo.keys();
        ArrayList<String> lst = new ArrayList<>();
        ArrayList<String> replacements = new ArrayList<>(Arrays.asList(".", "/", "-"));
        type = type.replace(".", "_");

        String cypher = "";
        if (id != null) {
            cypher += "MATCH (prev) WHERE ID(prev) = " + id + " ";
        }
        cypher += "CREATE (node:" + type + " {";

        while (it.hasNext()) {
            String k = it.next();

            String k2 = new String(k.toCharArray());

            for (String i: replacements) {
                k2 = k2.replace(i, "_");
            }
            if (Character.isDigit(k2.charAt(0)))
                k2 = "_" + k2;

            if (hm.get("Other").contains(k)) {
                lst.add(k2+": "+jo.get(k));
            }
            else if (hm.get("String").contains(k)){
                lst.add(k2 + ": \"" + ((String) jo.get(k)).replace("\"", "\\\"") + "\"");
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
        String fields = String.join(", ", lst);
        cypher += fields + "})";
        if (id != null) {
            cypher += " CREATE (prev) - [:NEXT] -> (node)";
        }
        cypher += " RETURN node";

        String finalCypher = cypher;
        String nodeId = null;

        try (Session session = driver.session(SessionConfig.forDatabase("neo4j"))) {
            Record record = session.writeTransaction(tx -> {
                Result result = tx.run(finalCypher);
                return result.single();
            });
            nodeId = record.get("node").toString();
            nodeId = nodeId.substring(nodeId.indexOf("<") + 1, nodeId.indexOf(">"));
            //System.out.println(type + ": " + nodeId.substring(nodeId.indexOf("<") + 1, nodeId.indexOf(">")));
        } catch (Neo4jException ex) {
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
                addPipeline(jo.getJSONObject(k), k, nodeId);
            }
            else if (hm.get("Array").contains(k)) {
                JSONArray arr = jo.getJSONArray(k);
                for (int i = 0; i < arr.length(); i++) {
                    if (arr.get(i) instanceof JSONObject) {
                        addPipeline(arr.getJSONObject(i), k, nodeId);
                    }
                }
            }
        }
    }

    public void addPrevStages() {
        JSONObject jo2 = readJSON();
        JSONArray stages = jo2.getJSONArray("stages");
        for (int i = 0; i < stages.length(); i++) {
            String id = (String) stages.getJSONObject(i).get("refId");
            JSONArray prevs = (JSONArray) stages.getJSONObject(i).get("requisiteStageRefIds");
            if (prevs.length() == 0) {
                String finalCypher = "MATCH (s1:trigger { executionId: \""+jo2.get("id")+"\"}) " +
                        "MATCH (s2:stages { refId: \""+id+"\"}) CREATE (s1) - [:NEXT] -> (s2) RETURN s2";

                try (Session session = driver.session(SessionConfig.forDatabase("neo4j"))) {
                    Record record = session.writeTransaction(tx -> {
                        Result result = tx.run(finalCypher);
                        return result.single();
                    });
                } catch (Neo4jException ex) {
                    LOGGER.log(Level.SEVERE, finalCypher + " raised an exception", ex);
                    throw ex;
                }
            }
            for (int j = 0; j < prevs.length(); j++) {
                String finalCypher = "MATCH (s1:stages { refId: \""+prevs.get(j)+"\"}) " +
                        "MATCH (s2:stages { refId: \""+id+"\"}) MATCH (o1:outputs) <- [:NEXT] - (s1)  " +
                        "MATCH (c1:context) <- [:NEXT] - (s2) CREATE (s1) - [:NEXT] -> (s2) " +
                        "CREATE (o1) - [:NEXT] -> (c1) RETURN s1";

                try (Session session = driver.session(SessionConfig.forDatabase("neo4j"))) {
                    Record record = session.writeTransaction(tx -> {
                        Result result = tx.run(finalCypher);
                        return result.single();
                    });
                } catch (Neo4jException ex) {
                    LOGGER.log(Level.SEVERE, finalCypher + " raised an exception", ex);
                    throw ex;
                }
            }
        }
    }

    public static void main(String[] args){
        // Aura queries use an encrypted connection using the "neo4j+s" protocol
         String uri = GraphqlSpringBootApplication.uri;
         String user = GraphqlSpringBootApplication.user;
         String password = GraphqlSpringBootApplication.password;

        try (PopulateDatabase app = new PopulateDatabase(uri, user, password, Config.defaultConfig())) {
            app.deleteAll();
            JSONObject jo = readJSON();
            app.addPipeline(jo, "Pipeline", null);
            app.addPrevStages();
        } catch (Exception e) {
            System.out.println("fail");
        }
    }
}
