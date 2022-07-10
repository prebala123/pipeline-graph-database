import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.*;
import java.rmi.server.RemoteRef;
import java.util.Objects;


public class SendRequest {
    public static void main(String[] args) {
        getCastByMovie("Top Gun");
        //getMoviesByPerson("Tom Cruise");
        //getMoviesBetweenDates(2000, 2022);
    }

    static String path = "C:/Users/rebal/movies/movies-database/neo4j-graphql-java-master/JSONfiles/output.txt";

    public static void sendAnyQuery(String query) {
        JSONObject jo = new JSONObject();
        jo.put("query", query);
        //System.out.println(jo);
        try (PrintWriter out = new PrintWriter(new FileWriter(path))){
            JSONObject results = whenSendPostRequest_thenCorrect(jo);
            //System.out.println(results);
            //results.write(out);
            out.write(results.toString(4));
        }
        catch (IOException ignored) {

        }
    }

    public static void getMoviesBetweenDates(int start, int end) {
        String query = "query { \n" +
                "  movies (where : {released_gte : "+start+", released_lte : "+end+"}){\n" +
                "    title\n" +
                "  }\n" +
                "}";
        JSONObject jo = new JSONObject();
        jo.put("query", query);
        //System.out.println(jo);
        try (PrintWriter out = new PrintWriter(new FileWriter(path))){
            JSONObject results = whenSendPostRequest_thenCorrect(jo);
            //System.out.println(results);
            JSONArray data = results.getJSONObject("data").getJSONArray("movies");
            for (int i = 0; i < data.length(); i++) {
                System.out.println(data.getJSONObject(i).get("title"));
            }
            //results.write(out);
            out.write(results.toString(4));
        }
        catch (IOException ignored) {

        }
    }

    public static void getCastByMovie(String title) {
        String query = "query {\n" +
                "  movies (where:{title:\""+title+"\"}){\n" +
                "    cast {\n" +
                "      name\n" +
                "    }\n" +
                "  }\n" +
                "}";
        JSONObject jo = new JSONObject();
        jo.put("query", query);
        //System.out.println(jo);

        try (PrintWriter out = new PrintWriter(new FileWriter(path))){
            JSONObject results = whenSendPostRequest_thenCorrect(jo);
            //System.out.println(results);
            JSONArray data = results.getJSONObject("data").getJSONArray("movies")
                    .getJSONObject(0).getJSONArray("cast");
            for (int i = 0; i < data.length(); i++) {
                System.out.println(data.getJSONObject(i).get("name"));
            }
            out.write(results.toString(4));
            //results.write(out);
        }
        catch (IOException e) {
            System.out.println("fail");
        }
    }

    public static void getMoviesByPerson(String name) {
        String query = "query {\n" +
                "  people (where:{name:\""+name+"\"}){\n" +
                "    history {\n" +
                "      title\n" +
                "    }\n" +
                "  }\n" +
                "}";
        JSONObject jo = new JSONObject();
        jo.put("query", query);
        //System.out.println(jo);
        try (PrintWriter out = new PrintWriter(new FileWriter(path))){
            JSONObject results = whenSendPostRequest_thenCorrect(jo);
            //System.out.println(results);
            JSONArray data = results.getJSONObject("data").getJSONArray("people")
                    .getJSONObject(0).getJSONArray("history");
            for (int i = 0; i < data.length(); i++) {
                System.out.println(data.getJSONObject(i).get("title"));
            }
            //results.write(out);
            out.write(results.toString(4));
        }
        catch (IOException ignored) {

        }
    }

    public static JSONObject whenSendPostRequest_thenCorrect(JSONObject jo)
            throws IOException {

        final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");

        //String queryjson = "{\"query\":\"query {\n  movies {\n    title\n  }\n}\"}";
        //System.out.println(queryjson);

        //JSONObject jo = new JSONObject();
        //jo.put("query", "query {\n  movies {\n    title\n  }\n}");

        RequestBody postBody = RequestBody.create(jo.toString(), JSON);
        Request request = new Request.Builder()
                .url("http://localhost:8080/graphql")
                .addHeader("Content-Type", "application/json")
                .post(postBody)
                .build();

        OkHttpClient client = new OkHttpClient();
        Call call = client.newCall(request);
        Response response = call.execute();
        //System.out.println(response.body().string());

        JSONObject results = new JSONObject(response.body().string());
        return results;
        //System.out.println(results.getJSONObject("data").getJSONArray("movies"));
        /*JSONArray data = results.getJSONObject("data").getJSONArray("movies");
        for (int i = 0; i < data.length(); i++) {
            System.out.println(data.getJSONObject(i).get("title"));
        }*/

        //assertThat(response.code(), equalTo(200));
    }
}
