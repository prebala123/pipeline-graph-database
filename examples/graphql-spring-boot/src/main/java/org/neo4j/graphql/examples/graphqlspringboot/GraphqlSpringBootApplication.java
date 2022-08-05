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
        System.out.println("Starting");
        //reads configuration from yaml file
        System.out.println("Configuring details");
        dataPath = args[0];
        new YAMLConfig();
        //creates the schema from pipeline data
        System.out.println("Creating schema");
        GenerateSchema.main(null);
        //connects to graphiql
        System.out.println("Connecting to database");
        SpringApplication.run(GraphqlSpringBootApplication.class, args);
        //adds data to database
        System.out.println("Populating database");
        PopulateDatabase.main(null);
        System.out.println("Completed");

    }
}
