package org.neo4j.graphql.examples.graphqlspringboot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class TestJSON {
    public static void main(String[] args) {
        JSONArray jo = (JSONArray) PopulateDatabase.readJSON2();
        //System.out.println(jo.length());

        File file = new File("C:/Users/rebal/Documents/Pipeline/testing.txt");
        try (PrintWriter out = new PrintWriter(file)) {
            out.write(jo.getJSONObject(17).toString(4));
        } catch (IOException e) {
            System.out.println("fail");
        }

    }

    public static void test(String t) {
        File file = new File("C:/Users/rebal/Documents/Pipeline/testing.txt");
        try (PrintWriter out = new PrintWriter(file)) {
            out.write(t);
        } catch (IOException e) {
            System.out.println("fail");
        }
    }

}
