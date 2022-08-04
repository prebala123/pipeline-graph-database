# Kotlin GraphQL Query Drivers for Neo4J Pipeline Database
This is a project by Pranav Rebala, Rajeev Bommana, and Yash Deshmukh for OpsMx. It uses Kotlin to interact with a Neo4j Database using GraphQL protocol (with the help of GraphQL Java Libraries). 

The different functionalities of the project are outlined below:

## Augmented Schema Generator
The Schema Generator parses a file and automatically creates a GraphQL schema file in your chosen local directory.

![image](https://user-images.githubusercontent.com/108049514/182973886-d622eca0-c123-4fe4-b434-9dc32008cd7e.png)

## Database Populator
The Database Populator parses a JSON file and populates your Neo4j Database (NOTE: you need to add your Neo4j Database credentials in resouces/application.yaml)

![image](https://user-images.githubusercontent.com/108049514/182974175-00999c52-5f0f-4ba8-ba05-aca30cb93de2.png)

## Querying the Database 
Once you have downloaded the source code and configured it with your local directories and Neo4j Database credentials, you can run the project and head to http://localhost:8080/graphiql?path=/graphql to start querying.

![image](https://user-images.githubusercontent.com/108049514/182973192-a4d9d1d2-b59a-4d51-a35c-65ba1c1ae159.png)

You can both query for information, as well as run mutations to change the database. 

## Testing
Lastly, we have also included test cases within src/test. 
