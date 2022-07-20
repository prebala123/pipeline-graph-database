package org.neo4j.graphql.examples.graphqlspringboot;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.Neo4jException;

import java.io.File;
import java.io.FileNotFoundException;
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
        String path = "C:/Users/rebal/Documents/Pipeline/pipeline.json";
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

    public void addPipeline(JSONObject jo, String type) {
        HashMap<String, HashSet<String>> hm = separateFields(jo);
        Iterator<String> it = jo.keys();
        ArrayList<String> lst = new ArrayList<>();
        type = type.replace(".", "_");
        String cypher = "CREATE (node:" + type + " {";

        while (it.hasNext()) {
            String k = it.next();
            ArrayList<String> replacements = new ArrayList<>(Arrays.asList(".", "/", "-"));
            String k2 = new String(k.toCharArray());

            for (String i: replacements) {
                k2 = k2.replace(i, "_");
            }

            if (hm.get("Other").contains(k)) {
                lst.add(k2+": "+jo.get(k));
            }
            else if (hm.get("String").contains(k)){
                if (type.equals("avatarUrls")) {
                    //fix later
                    continue;
                }
                lst.add(k2 + ": \"" + ((String) jo.get(k)).replace("\"", "\\\"") + "\"");
            }
            else if (hm.get("Object").contains(k)){
                addPipeline(jo.getJSONObject(k), k);
            }
            else {
                JSONArray arr = jo.getJSONArray(k);
                for (int i = 0; i < arr.length(); i++) {
                    if (arr.get(i) instanceof JSONObject) {
                        addPipeline(arr.getJSONObject(i), k);
                    }
                }
            }
        }
        String fields = String.join(", ", lst);
        cypher += fields + "}) RETURN node";

        String finalCypher = cypher;

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

    public static void main(String[] args){
        // Aura queries use an encrypted connection using the "neo4j+s" protocol
         String uri = "neo4j://localhost";
         String user = "neo4j";
         String password = "movies";

        try (PopulateDatabase app = new PopulateDatabase(uri, user, password, Config.defaultConfig())) {
            app.deleteAll();
            JSONObject jo = readJSON();
            app.addPipeline(jo, "Pipeline");
        } catch (Exception e) {
            System.out.println("fail");
        }
    }
}
