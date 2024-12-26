//Name: Khushi Bhuwania
//Andrew id: kbhuwani

package ds.project4app;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


// Define the servlet and its URL pattern
@WebServlet(name = "DashboardServlet", urlPatterns = {"/dashboard"})
public class MealDbService extends HttpServlet {

    private static final String MONGO_URL = "mongodb+srv://kbhuwani:nSlZ0tEUAr77BzV0@cluster0.sjcf7.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
    private static final String DATABASE_NAME = "mealdb";
    private static final String COLLECTION_NAME = "logs";

    // Handle the GET request
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Get the MongoDB collection
        MongoCollection<Document> collection = MongoDBUtility.getCollection();

        // Total number of logs
        long totalLogs = collection.countDocuments();
        request.setAttribute("totalLogs", Long.valueOf(totalLogs));

        // Find top device using the app
        String topDevice = "Unknown";
        Document topDeviceDoc = collection.aggregate(
                List.of(
                        Aggregates.group("$userAgent", Accumulators.sum("count", 1)),
                        Aggregates.sort(Sorts.descending("count")),
                        Aggregates.limit(1)
                )
        ).first();
        if (topDeviceDoc != null) {
            topDevice = topDeviceDoc.getString("_id");
        }
        request.setAttribute("topDevice", topDevice);

        // Find top searched term
        String topSearchedTerm = "None";
        Document topTermDoc = collection.aggregate(
                List.of(
                        Aggregates.group("$query", Accumulators.sum("count", 1)),
                        Aggregates.sort(Sorts.descending("count")),
                        Aggregates.limit(1)
                )
        ).first();
        if (topTermDoc != null) {
            topSearchedTerm = topTermDoc.getString("_id");
        }
        request.setAttribute("topSearchedTerm", topSearchedTerm);

        // Retrieve logs for the table
        StringBuilder logData = new StringBuilder("<tr><th>Query</th><th>Creation Time</th><th>Countries</th><th>User Agent</th><th>User IP</th></tr>");
        try (MongoCursor<Document> cursor = collection.find().sort(Sorts.descending("generationTime")).iterator()) {
            while (cursor.hasNext()) {
                Document log = cursor.next();

                // Extract log details
                String query = log.getString("query");
                String generationTime = log.getString("generationTime") != null ? log.getString("generationTime"): "NA";
                List<String> countries = log.getList("countries", String.class);
                String userAgent = log.getString("userAgent");
                String userIp = log.getString("clientIp");

                // Append log data to the table
                logData.append("<tr>")
                        .append("<td>").append(query != null ? query : "N/A").append("</td>")
                        .append("<td>").append(generationTime).append("</td>")
                        .append("<td>").append(countries != null ? String.join(", ", countries) : "N/A").append("</td>")
                        .append("<td>").append(userAgent != null ? userAgent : "N/A").append("</td>")
                        .append("<td>").append(userIp != null ? userIp : "N/A").append("</td>")
                        .append("</tr>");
            }
        }
        request.setAttribute("logData", logData.toString());

        // Retrieve unique user agents
        StringBuilder userAgentData = new StringBuilder("<tr><th>User Agent</th></tr>");
        try (MongoCursor<String> cursor = collection.distinct("userAgent", String.class).iterator()) {
            while (cursor.hasNext()) {
                String userAgent = cursor.next();
                userAgentData.append("<tr><td>").append(userAgent).append("</td></tr>");
            }
        }
        request.setAttribute("userAgentData", userAgentData.toString());

        // Forward the request to the JSP page
        request.getRequestDispatcher("/logs.jsp").forward(request, response);
    }

    // Handles POST requests
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringBuilder body = new StringBuilder();
        String line;

        // Read the JSON request body
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }

        // Parse the JSON body to extract the query parameter
        String query = "";
        try {
            JSONObject jsonObject = new JSONObject(body.toString());
            query = jsonObject.optString("query", ""); // Default to empty string if the key "query" is not present
        } catch (Exception e) {
            // Respond with a 400 status if JSON parsing fails
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid JSON input\"}");
            return;
        }

        // Validate that the query parameter is not empty
        if (query.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Query parameter is required\"}");
            return;
        }

        // Construct the API URL for fetching data
        String apiUrl = "https://www.themealdb.com/api/json/v1/1/search.php?s="+ query;

        // Fetch data from the external API
        String jsonResponse = fetchDataFromApi(apiUrl);

        List<String> countries = new ArrayList<>();

        // Process the API response
        if (jsonResponse != null) {
            try {
                JSONObject apiResponse = new JSONObject(jsonResponse);
                JSONArray meals = apiResponse.optJSONArray("meals");

                if (meals != null) {
                    for (int i = 0; i < meals.length(); i++) {
                        JSONObject meal = meals.getJSONObject(i);
                        String country = meal.optString("strArea", "");
                        if (!country.isEmpty() && !countries.contains(country)) {
                            countries.add(country);
                        }
                    }
                }
            } catch (Exception e) {
                // Log and handle the exception (e.g., log error details)
                e.printStackTrace();
            }

            // Log the query and countries to MongoDB
            logToMongoDB(query, countries, request);
        }

        // Send the response back to the client
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.println(jsonResponse);
        out.flush();
    }

    // Method to fetch data from an external API
    private String fetchDataFromApi(String apiUrl) {
        try {
            // Create and configure a connection to the API
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            // Check the response code and read the response if successful
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                Scanner scanner = new Scanner(url.openStream());
                StringBuilder inline = new StringBuilder();
                while (scanner.hasNext()) {
                    inline.append(scanner.nextLine());
                }
                scanner.close();// Close the scanner
                return inline.toString();
            }
        } catch (Exception e) {
            // Print any exceptions that occur
            e.printStackTrace();
        }
        return null; // Return null if there was an error or response code was not 200
    }

    private void logToMongoDB(String data, List<String> countries,HttpServletRequest request ) {
        try (MongoClient mongoClient = MongoClients.create(MONGO_URL)) {
            // Connect to the database and collection
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

            // Gather additional details
            String clientIp = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            String generationTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Create a new document
            Document logEntry = new Document("_id", new org.bson.types.ObjectId())
                    .append("query", data)
                    .append("countries", countries)
                    .append("generationTime", generationTime)
                    .append("clientIp", clientIp)
                    .append("userAgent", userAgent);

            // Insert the document into the collection
            collection.insertOne(logEntry);
        }
    }
}
