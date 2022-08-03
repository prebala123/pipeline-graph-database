package org.neo4j.graphql.examples.graphqlspringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GraphqlSpringBootApplication {

    static String dataPath;
    static String uri;
    static String user;
    static String password;
    static String database;

    public static void main(String[] args) {
        System.out.println("starting");
        //reads configuration from yaml file
        new YAMLConfig();
        //creates the schema from pipeline data
        GenerateSchema.main(null);
        //connects to graphiql
        SpringApplication.run(GraphqlSpringBootApplication.class, args);
        //adds data to database
        PopulateDatabase.main(null);
        System.out.println("done");

    }
}
