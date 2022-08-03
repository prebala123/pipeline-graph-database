package org.neo4j.graphql.examples.graphqlspringboot;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class YAMLConfig {

    public YAMLConfig() {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("application.yaml");
        Map<String, Object> obj = yaml.load(inputStream);
        Map<String, Object> org = (Map<String, Object>) obj.get("org");
        Map<String, Object> neo4j = (Map<String, Object>) org.get("neo4j");
        Map<String, Object> driver = (Map<String, Object>) neo4j.get("driver");
        Map<String, Object> authentication = (Map<String, Object>) driver.get("authentication");
        String database = (String) obj.get("database");
        String dataPath = (String) obj.get("dataPath");
        String uri = (String) driver.get("uri");
        String user = (String) authentication.get("username");
        String password = (String) authentication.get("password");
        GraphqlSpringBootApplication.uri = uri;
        GraphqlSpringBootApplication.user = user;
        GraphqlSpringBootApplication.password = password;
        GraphqlSpringBootApplication.database = database;
        GraphqlSpringBootApplication.dataPath = dataPath;
    }

}

