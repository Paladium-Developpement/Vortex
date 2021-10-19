package fr.paladium.palabot.database;

import fr.paladium.palabot.database.managers.GroupsManager;
import fr.paladium.palabot.database.managers.TempBanManager;
import fr.paladium.palabot.database.managers.TempMuteManager;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import lombok.Getter;

public class DatabaseMongo {

    @Getter
    private MongoClient mongoClient;

    @Getter
    private MongoDatabase database;
    @Getter
    final Datastore datastore;

    public final TempMuteManager tempmutes;
    public final TempBanManager tempbans;
    public final GroupsManager groups;

    public DatabaseMongo(String host, int port, String user, String pass, String database)
    {
        MongoCredential credential = MongoCredential.createCredential(user, database, pass.toCharArray());
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(
                        String.format("mongodb://%s:%s/%s", host, port, database)))
                .build();
        this.mongoClient = MongoClients.create(settings);
        datastore = Morphia.createDatastore(this.mongoClient, database);
        datastore.getMapper().mapPackage("fr.paladium.palabot.database.dao");
        datastore.ensureIndexes();
        this.database = mongoClient.getDatabase(database);
        tempmutes = new TempMuteManager(this);
        tempbans = new TempBanManager(this);
        groups = new GroupsManager(this);
    }
}
