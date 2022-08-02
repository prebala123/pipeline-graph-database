package org.neo4j.graphql.examples.graphqlspringboot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GraphqlSpringBootApplication {

    static String dataPath = "C:/Users/rebal/Documents/Pipeline/pipelines-array.json";

    //@Value("${org.neo4j.driver.uri}")
    static String uri;

    static String user = "neo4j";
    static String password = "movies";
    //static String uri = "neo4j+s://2b6e1197.databases.neo4j.io";
    //static String user = "neo4j";
    //static String password = "RUM-LXRjMVy_bz24CkwWnIdeqatLePibuG_S_VomV2w";
    static String database = "neo4j";
    static boolean encrypted = false;
    static boolean printer = true;
    static boolean graphiql = true;

    public static void main(String[] args) {
        System.out.println("starting");
        //System.out.println(uri);
        //PopulateDatabase.updateYAML();
        //GenerateSchema.main(null);
        SpringApplication.run(GraphqlSpringBootApplication.class, args);
        //PopulateDatabase.main(null);
        System.out.println("done");

    }
}
