# Neo4J Pipeline Database
This is a project by Pranav Rebala, Rajeev Bommana, and Yash Deshmukh. 

Our project is designed to take in pipeline data and insert it into a graph database for easy querying. We hope to use the graph database structure to make finding failure points of pipelines much easier and help to determine what key components of the pipeline need to be changed to run successfully. Our project uses Kotlin to interact with a Neo4j Database using GraphQL protocol (with the help of GraphQL Java Libraries). Users can input GraphQL queries and receive the requested outputs efficiently.

The different functionalities of the project are outlined below:

## Augmented Schema Generator
The Schema Generator parses a pipeline data JSON file and automatically creates a GraphQL schema file in your chosen local directory. The program searches through all JSON keys and writes out all objects and attributes, including links to other objects.

![image](https://user-images.githubusercontent.com/108049514/182973886-d622eca0-c123-4fe4-b434-9dc32008cd7e.png)

## Database Populator
The Database Populator parses a pipelin JSON file and populates your Neo4j Database. The program recursively traverses the levels of the JSON and inputs all objects, including adding all of the links between objects. (NOTE: you need to add your Neo4j Database credentials in resouces/application.yaml)

![image](https://user-images.githubusercontent.com/108049514/182974175-00999c52-5f0f-4ba8-ba05-aca30cb93de2.png)

## Querying the Database 
Once you have downloaded the source code and configured it with your local directories and Neo4j Database credentials, you can run the project and head to http://localhost:8080/graphiql?path=/graphql to start querying. Use standard GraphQL syntax and the program will output the response from querying the Neo4j database.

![image](https://user-images.githubusercontent.com/108049514/182973192-a4d9d1d2-b59a-4d51-a35c-65ba1c1ae159.png)

You can both query for information, as well as run mutations to change the database. 

## Testing
Lastly, we have also included test cases within src/test. 
