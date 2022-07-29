package org.neo4j.graphql.examples.graphqlspringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GraphqlSpringBootApplication {

    static String dataPath = "C:/Users/rebal/Documents/Pipeline/pipeline.json";
    static String uri = "neo4j://localhost";
    static String user = "neo4j";
    static String password = "movies";
    static String database = "neo4j";
    static boolean encrypted = false;
    static boolean printer = true;
    static boolean graphiql = true;

    public static void main(String[] args) {
        PopulateDatabase.updateYAML();
        GenerateSchema.main(null);
        SpringApplication.run(GraphqlSpringBootApplication.class, args);
        PopulateDatabase.main(null);
    }

}
