package org.neo4j.graphql.examples.graphqlspringboot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

public class GenerateSchema {

    static HashMap<String, String[]> info = new HashMap<>();
    static HashMap<String, HashSet<String>> otherFields = new HashMap<>();

    static String schemaFile = "C:/Users/rebal/pipeline/pipelinedb/examples/graphql-spring-boot/src/main/java/org/neo4j/graphql/examples/graphqlspringboot/PipelineSchema.txt";
    //static String schemaFile = "C:/Users/rebal/pipeline/pipelinedb/examples/graphql-spring-boot/src/main/java/resources/neo4j.graphql";


    public static void main(String[] args) {
        //add relationships and fields first
        addRelationships();
        addFields();
        makeSchema();
    }

    public static void addRelationships() {
        //add all relationships here

        /*addRelationInfo("stages", "outputs", "OUTPUTS", "OUT");
        addRelationInfo("stages", "context", "CONTEXTUALIZES", "OUT");
        addRelationInfo("stages", "tasks", "REQUIRED_TASKS", "OUT");
        addRelationInfo("stages", "status", "STATUS", "OUT");*/
    }

    public static void addFields() {
        //add our own fields not defined by JSON
        addOtherData("stages", "nextStage: stages @relation(name: \"NEXT\", direction: OUT)");
    }

    public static void addRelationInfo(String node, String field, String relationship, String direction) {
        info.put(node + "->" + field, new String[]{relationship, direction});
    }

    public static void addOtherData(String node, String field) {
        if (otherFields.containsKey(node))
            otherFields.get(node).add(field);
        else {
            HashSet<String> hs = new HashSet<>();
            hs.add(field);
            otherFields.put(node, hs);
        }
    }

    public static String[] getRelationInfo(String node, String field) {
        if (info.containsKey(node + "->" + field))
            return info.get(node + "->" + field);
        else
            return new String[]{"NEXT", "OUT"};
    }

    public static void makeSchema() {
        JSONObject jo = readJSON();
        HashMap<String, HashSet<String>> fieldTypes = separateAllFields(jo);
        HashMap<String, HashSet<String>> schema = new HashMap<>();
        readPipeline(jo, "Pipeline", schema, fieldTypes);
        String finalSchema = "";
        finalSchema += prevSchema;
        String queries = "type Query {\n    ";
        for (HashMap.Entry<String, HashSet<String>> entry2 : schema.entrySet()) {
            String key2 = entry2.getKey();
            HashSet<String> value2 = entry2.getValue();
            finalSchema += "type " + cleanType(key2) + " {\n    ";
            queries += cleanType(key2) + ": [" + cleanType(key2) + "]\n    ";
            if (otherFields.containsKey(key2)){
                for (String s: otherFields.get(key2))
                    finalSchema += s + "\n    ";
            }
            if (value2.size() == 0) {
                finalSchema += "id: String\n    ";
            }
            for (String s : value2) {
                String dataType = getKeyByValue(fieldTypes, s);
                if (dataType.equals("Object")) {
                    String[] info = getRelationInfo(key2, s);
                    dataType = cleanFields(s) + " @relation(name: \""+info[0]+"\", direction: "+info[1]+")";
                }
                if (dataType.equals("Array")) {
                    String[] info = getRelationInfo(key2, s);
                    dataType = "[" + cleanFields(s) + "] @relation(name: \""+info[0]+"\", direction: "+info[1]+")";
                }
                if (dataType.equals("StringArr") || dataType.equals("Other"))
                    dataType = "[String]";
                finalSchema += cleanFields(s) + ": " + dataType + "\n    ";
            }
            finalSchema += "\n}\n\n";
        }
        queries += "\n}";
        finalSchema += queries;
        toFile(finalSchema);
    }

    public static String getKeyByValue(HashMap<String, HashSet<String>> map, String value) {
        HashSet<String> keys = new HashSet<>();
        for (HashMap.Entry<String, HashSet<String>> entry : map.entrySet()) {
            if (entry.getValue().contains(value)) {
                keys.add(entry.getKey());
            }
        }
        if (keys.contains("String"))
            return "String";
        else
            return keys.iterator().next();
    }

    public static String cleanType(String s) {
        s = s.replace(".", "_");
        return s;
    }

    public static String cleanFields(String s) {
        ArrayList<String> replacements = new ArrayList<>(Arrays.asList(".", "/", "-"));
        for (String i: replacements) {
            s = s.replace(i, "_");
        }
        if (Character.isDigit(s.charAt(0)))
            s = "_" + s;
        return s;
    }

    public static HashMap<String, HashSet<String>> separateAllFields(JSONObject jo) {
        HashMap<String, HashSet<String>> hm = new HashMap<>();
        hm.put("Object", new HashSet<>());
        hm.put("Array", new HashSet<>());
        hm.put("String", new HashSet<>());
        hm.put("Boolean", new HashSet<>());
        hm.put("Float", new HashSet<>());
        hm.put("Other", new HashSet<>());
        hm.put("StringArr", new HashSet<>());
        separateAllFieldsInner(jo, hm);
        return hm;
    }

    public static void separateAllFieldsInner(JSONObject jo, HashMap<String, HashSet<String>> hm) {
        String kind = "Other";
        if (jo.names() == null)
            return;
        for (int i = 0; i < jo.names().length(); i++) {
            Object ob = jo.get(jo.names().getString(i));
            if (ob instanceof JSONObject) {
                kind = "Object";
                separateAllFieldsInner(jo.getJSONObject(jo.names().getString(i)), hm);
            } else if (ob instanceof JSONArray) {
                JSONArray arr = jo.getJSONArray(jo.names().getString(i));
                if (arr.length()==0)
                    kind = "StringArr";
                for (int j = 0; j < arr.length(); j++) {
                    if (arr.get(j) instanceof JSONObject) {
                        kind = "Array";
                        separateAllFieldsInner(arr.getJSONObject(j), hm);
                    }
                    else {
                        kind = "StringArr";
                    }
                }
            } else if (ob instanceof String) {
                kind = "String";
            } else if (ob instanceof Boolean) {
                kind = "Boolean";
            } else if ((ob instanceof Long) || (ob instanceof Integer) || (ob instanceof Double)) {
                kind = "Float";
            } else {
                kind = "Other";
            }
            hm.get(kind).add(jo.names().getString(i));
        }
    }

    public static void toFile(String schema) {
        try (PrintWriter out = new PrintWriter(new FileWriter(schemaFile))){
            out.write(schema);
        }
        catch (IOException e) {
            System.out.println("fail");
        }
    }

    public static JSONObject readJSON() {
        String path = SendRequest.path;
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

    public static void readPipeline(JSONObject jo, String type, HashMap<String, HashSet<String>> schema, HashMap<String, HashSet<String>> hm) {
        Iterator<String> it = jo.keys();
        HashSet<String> lst = new HashSet<>();
        type = type.replace(".", "_");
        if (schema.containsKey(type)) {
            lst = schema.get(type);
        }
        schema.put(type, lst);

        while (it.hasNext()) {
            String k = it.next();

            Object typ = jo.get(k);
            lst.add(k);
            if (typ instanceof JSONObject)
                readPipeline(jo.getJSONObject(k), k, schema, hm);
            if (typ instanceof JSONArray) {
                JSONArray arr = jo.getJSONArray(k);
                for (int i = 0; i < arr.length(); i++) {
                    if (arr.get(i) instanceof JSONObject) {
                        readPipeline(arr.getJSONObject(i), k, schema, hm);
                    }
                }
            }
        }
    }

    static String prevSchema = "input PipelineInput {\n" +
            "  type: String\n" +
            "  id: String\n" +
            "  application: String\n" +
            "  name: String\n" +
            "  buildTime: Float\n" +
            "  canceled: Boolean\n" +
            "  limitConcurrent: Boolean\n" +
            "  keepWaitingPipelines: Boolean\n" +
            "  startTime: Float\n" +
            "  endTime: Float\n" +
            "  status: String\n" +
            "  origin: String\n" +
            "  pipelineConfigId: String\n" +
            "  spelEvaluator: String\n" +
            "}\n" +
            "\n" +
            "input NotificationsInput {\n" +
            "  id: String\n" +
            "  address: String\n" +
            "  level: String\n" +
            "  type: String\n" +
            "  when: [String]\n" +
            "}\n" +
            "\n" +
            "input TriggerInput {\n" +
            "  type: String\n" +
            "  user: String\n" +
            "  rebake: Boolean\n" +
            "  dryRun: Boolean\n" +
            "  strategy: Boolean\n" +
            "  executionId: String\n" +
            "  eventId: String\n" +
            "  enabled: Boolean\n" +
            "  preferred: Boolean\n" +
            "  expectedArtifacts: [String ]\n" +
            "  resolvedExpectedArtifacts: [String]\n" +
            "  notifications: [String ]\n" +
            "  artifacts: [String ]\n" +
            "}\n" +
            "\n" +
            "input ContextInput{\n" +
            "  propertyFile: String\n" +
            "  consecutiveErrors: String\n" +
            "  refId: String\n" +
            "  Buildnumber: String\n" +
            "}\n" +
            "\n" +
            "input StagesInput {\n" +
            "  id: String\n" +
            "  refId: String\n" +
            "  type: String\n" +
            "  name: String\n" +
            "  startTime: Float\n" +
            "  endTime: Float\n" +
            "  status: String\n" +
            "}\n" +
            "\n" +
            "input OutputsInput {\n" +
            "  refId: String\n" +
            "  Buildnumber: String\n" +
            "  GitBranch: String\n" +
            "  GitRepo: String\n" +
            "  JobNumber: String\n" +
            "  canaryimage: String\n" +
            "  startTime: Float\n" +
            "  report: String\n" +
            "  hosturl: String\n" +
            "  key: String\n" +
            "  reason: String\n" +
            "  canaryReportURL: String\n" +
            "  overallScore: Float\n" +
            "  location: String\n" +
            "  trigger: String\n" +
            "  overallResult: String\n" +
            "  trigger_json: String\n" +
            "  executedBy: String\n" +
            "  message: String\n" +
            "  status: String\n" +
            "  comments: String\n" +
            "  navigationalURL: String\n" +
            "  cimage: String\n" +
            "  bimage: String\n" +
            "  prodimage: String\n" +
            "}\n" +
            "\n" +
            "input ArtifactInput {\n" +
            "  refId: String\n" +
            "  customKind: Boolean\n" +
            "  location: String\n" +
            "  type: String\n" +
            "  version: String\n" +
            "  reference: String\n" +
            "  name: String\n" +
            "  artifactAccount: String\n" +
            "  account: String\n" +
            "}\n" +
            "\n" +
            "input MetadataInput {\n" +
            "  account: String\n" +
            "}\n" +
            "\n" +
            "input StagesRelationshipInput {\n" +
            "  s1: String\n" +
            "  s2: String\n" +
            "}\n" +
            "\n" +
            "input ContextRelationshipInput{\n" +
            "\n" +
            "  c1: String\n" +
            "  s1: String\n" +
            "}\n" +
            "\n" +
            "type Mutation {\n" +
            "  createPipeline(input: PipelineInput): Pipeline @cypher(statement:\n" +
            "  \"\"\"MERGE (p:Pipeline {type: input.type, id: input.id, application: input.application, name: input.name,\n" +
            "  buildTime: input.buildTime, canceled: input.canceled, limitConcurrent: input.limitConcurrent,\n" +
            "  keepWaitingPipelines: input.keepWaitingPipelines, startTime: input.startTime, endTime: input.endTime, status: input.status,\n" +
            "  origin: input.origin, pipelineConfigId: input.pipelineConfigId, spelEvaluator: input.spelEvaluator}) RETURN p\"\"\")\n" +
            "  createTrigger(input: TriggerInput): trigger @cypher(statement:\n" +
            "  \"\"\"MERGE (t:trigger {eventId: input.eventId, dryRun: input.dryRun, rebake: input.rebake, type: input.type,\n" +
            "  enabled: input.enabled, executionId: input.executionId, strategy: input.strategy, user: input.user,\n" +
            "  preferred: input.preferred}) RETURN t\"\"\")\n" +
            "  createNotifications(input: NotificationsInput): notifications @cypher(statement:\n" +
            "  \"\"\"MERGE (n:notifications {address: input.address, level: input.level, type: input.type, when: input.when})\n" +
            "  MERGE (p:Pipeline {id: input.id}) MERGE (p) - [:NOTIFIES] -> (n) RETURN n\"\"\")\n" +
            "  createStages(input: StagesInput): stages @cypher(statement:\n" +
            "  \"\"\"MERGE (s:stages {id: input.id, refId: input.refId, type: input.type, name: input.name,\n" +
            "  startTime: input.startTime, endTime: input.endTime, status: input.status}) RETURN s\"\"\")\n" +
            "  createOutputs(input: OutputsInput): outputs @cypher(statement:\n" +
            "  \"\"\"CREATE (o:outputs {Buildnumber: input.Buildnumber, GitBranch: input.GitBranch, GitRepo: input.GitRepo,\n" +
            "  JobNumber: input.JobNumber, canaryimage: input.canaryimage, startTime: input.startTime, report: input.report,\n" +
            "  hosturl: input.hosturl, key: input.key, reason: input.reason, canaryReportURL: input.canaryReportURL,\n" +
            "  overallScore: input.overallScore, location: input.location, trigger: input.trigger, overallResult: input.overallResult,\n" +
            "  trigger_json: input.trigger_json, executedBy: input.executedBy, message: input.message, status: input.status,\n" +
            "  comments: input.comments, navigationalURL: input.navigationalURL, cimage: input.cimage, bimage: input.bimage,\n" +
            "  prodimage: input.prodimage}) MERGE (s1:Stages { refId: input.refId}) MERGE (s1) - [:OUTPUTS] -> (o) RETURN o\"\"\")\n" +
            "\n" +
            "\n" +
            "\n" +
            "  createContext(input: ContextInput): context@cypher(statement:\n" +
            "\n" +
            "    \"\"\"CREATE (c:Context{propertyFile: input.propertyFile, buildNumber: input.buildNumber, refId: input.refId\n" +
            "\n" +
            "        }) MERGE (s1:Stages { refId: input.refId}) MERGE (s1) - [:CONTEXTUALIZES] -> (c) RETURN c\"\"\"\n" +
            "\n" +
            "  )\n" +
            "\n" +
            "  ContextMerger(input:ContextRelationshipInput): context@cypher(statement:\n" +
            "\n" +
            "  \"MERGE (c1:Context { refId: input.c1}) MERGE (s1:Stages { refId: input.s1}) MERGE (c1) - [:NEXT] -> (s1) RETURN s1\")\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "  createdArtifacts(input: ArtifactInput): artifacts @cypher(statement:\n" +
            "  \"\"\"CREATE (a:Artifact {customKind: input.customKind, location: input.location, type: input.type,\n" +
            "  version: input.version, reference: input.reference, name: input.name, artifactAccount: input.artifactAccount})\n" +
            "  MERGE (s1:Stages { refId: input.refId}) MERGE (s1) - [:OUTPUTS] -> (o:Outputs) MERGE (o) - [:CREATED] -> (a)\n" +
            "  CREATE (m:Metadata {account: input.account}) MERGE (a) - [:METADATA] -> (m) RETURN a\"\"\")\n" +
            "  boundArtifacts(input: ArtifactInput): artifacts @cypher(statement:\n" +
            "  \"\"\"CREATE (a:Artifact {customKind: input.customKind, location: input.location, type: input.type,\n" +
            "  version: input.version, reference: input.reference, name: input.name, artifactAccount: input.artifactAccount})\n" +
            "  MERGE (s1:Stages { refId: input.refId}) MERGE (s1) - [:OUTPUTS] -> (o:Outputs) MERGE (o) - [:BOUND] -> (a)\n" +
            "  CREATE (m:Metadata {account: input.account}) MERGE (a) - [:METADATA] -> (m) RETURN a\"\"\")\n" +
            "  optionalArtifacts(input: ArtifactInput): artifacts @cypher(statement:\n" +
            "  \"\"\"CREATE (a:Artifact {customKind: input.customKind, location: input.location, type: input.type,\n" +
            "  version: input.version, reference: input.reference, name: input.name, artifactAccount: input.artifactAccount})\n" +
            "  MERGE (s1:Stages { refId: input.refId}) MERGE (s1) - [:OUTPUTS] -> (o:Outputs) MERGE (o) - [:OPTIONAL] -> (a)\n" +
            "  CREATE (m:Metadata {account: input.account}) MERGE (a) - [:METADATA] -> (m) RETURN a\"\"\")\n" +
            "  requiredArtifacts(input: ArtifactInput): artifacts @cypher(statement:\n" +
            "  \"\"\"CREATE (a:Artifact {customKind: input.customKind, location: input.location, type: input.type,\n" +
            "  version: input.version, reference: input.reference, name: input.name, artifactAccount: input.artifactAccount})\n" +
            "  MERGE (s1:Stages { refId: input.refId}) MERGE (s1) - [:OUTPUTS] -> (o:Outputs) MERGE (o) - [:REQUIRED] -> (a)\n" +
            "  CREATE (m:Metadata {account: input.account}) MERGE (a) - [:METADATA] -> (m) RETURN a\"\"\")\n" +
            "  artifacts(input: ArtifactInput): artifacts @cypher(statement:\n" +
            "  \"\"\"CREATE (a:Artifact {customKind: input.customKind, location: input.location, type: input.type,\n" +
            "  version: input.version, reference: input.reference, name: input.name, artifactAccount: input.artifactAccount})\n" +
            "  MERGE (s1:Stages { refId: input.refId}) MERGE (s1) - [:OUTPUTS] -> (o:Outputs) MERGE (o) - [:ARTIFACT] -> (a)\n" +
            "  CREATE (m:Metadata {account: input.account}) MERGE (a) - [:METADATA] -> (m) RETURN a\"\"\")\n" +
            "\n" +
            "  createStageTimeline(input: StagesRelationshipInput): stages @cypher(statement:\n" +
            "  \"MERGE (s1:stages { refId: input.s1}) MERGE (s2:Stages { refId: input.s2}) MERGE (s1) - [:NEXT] -> (s2) RETURN s2\")\n" +
            "  createStageTrigger(input: StagesRelationshipInput): stages @cypher(statement:\n" +
            "  \"MERGE (s1:trigger { executionId: input.s1}) MERGE (s2:Stages { refId: input.s2}) MERGE (s1) - [:NEXT] -> (s2) RETURN s2\")\n" +
            "  createPipelineTrigger(input: StagesRelationshipInput): Pipeline @cypher(statement:\n" +
            "  \"MERGE (s1:trigger { executionId: input.s1}) MERGE (s2:Pipeline {id: input.s2}) MERGE (s2) - [:NEXT] -> (s1) RETURN s2\")\n" +
            "\n" +
            "  attachStages(input: StagesRelationshipInput): stages @cypher(statement:\n" +
            "  \"\"\"MATCH (s1:stages { refId: input.s1}) MATCH (s2:stages { refId: input.s2}) MATCH (o1:outputs) <-- (s1)\n" +
            "  MATCH (c1:context) <-- (s2) CREATE (s1) - [:NEXT] -> (s2) CREATE (o1) - [:NEXT] -> (c1) RETURN s1\"\"\")\n" +
            "  attachTrigger(input: StagesRelationshipInput): stages @cypher(statement:\n" +
            "  \"MERGE (s1:trigger { executionId: input.s1}) MERGE (s2:stages { refId: input.s2}) MERGE (s1) - [:NEXT] -> (s2) RETURN s2\")\n" +
            "  attachPipeline(input: StagesRelationshipInput): Pipeline @cypher(statement:\n" +
            "  \"MERGE (s1:trigger { executionId: input.s1}) MERGE (s2:Pipeline {id: input.s2}) MERGE (s2) - [:NEXT] -> (s1) RETURN s2\")\n" +
            "\n" +
            "  deleteAll: Pipeline @cypher(statement: \"MATCH (a) DETACH DELETE a\")\n" +
            "}\n\n";
}
