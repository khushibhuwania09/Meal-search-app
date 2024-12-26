//Name: Khushi Bhuwania
//Andrew id: kbhuwani
package ds.project4app;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDBUtility {

    // MongoDB connection string (replace with your own)
    private static final String CONNECTION_STRING = "mongodb+srv://kbhuwani:nSlZ0tEUAr77BzV0@cluster0.sjcf7.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";

    // MongoClient instance (connection to MongoDB)
    private static final MongoClient mongoClient;

    // MongoDatabase instance (database in MongoDB)
    private static final MongoDatabase database;

    // MongoCollection instance (collection in the database)
    private static final MongoCollection<Document> collection;

    // Static initializer block to initialize the MongoDB connection
    static {
        // Create a ConnectionString object from the connection string
        ConnectionString connectionString = new ConnectionString(CONNECTION_STRING);

        // Set up MongoClientSettings with the connection string and server API version
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .serverApi(ServerApi.builder().version(ServerApiVersion.V1).build())
                .build();

        // Create a MongoClient instance with the settings
        mongoClient = MongoClients.create(settings);

        // Get the database named "StockApp"
        database = mongoClient.getDatabase("mealdb");

        // Get the collection named "StockAppCollection" from the database
        collection = database.getCollection("logs");
    }

    // Method to log a document to the database
    public static void logToDatabase(Document logEntry) {
        collection.insertOne(logEntry);
    }

    // Method to get the MongoCollection instance
    public static MongoCollection<Document> getCollection() {
        return collection;
    }

    // Method to close the MongoClient connection
    public static void close() {
        mongoClient.close();
    }
}
