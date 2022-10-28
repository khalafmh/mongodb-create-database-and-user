package pro.mahdi.mongodb.createdbanduser

import com.mongodb.*
import com.mongodb.client.MongoClient
import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain
import org.bson.BsonDocument
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger

@QuarkusMain
@Suppress("unused")
class Main(
    private val mongoClient: MongoClient,

    @ConfigProperty(name = "database-name")
    private val databaseName: String,

    @ConfigProperty(name = "username")
    private val username: String,

    @ConfigProperty(name = "password")
    private val password: String,

    ) : QuarkusApplication {
    companion object {
        private val LOG = Logger.getLogger(Main::class.java)
    }

    override fun run(vararg args: String?): Int {
        val clientSession = mongoClient.startSession(
            ClientSessionOptions.builder()
                .causallyConsistent(true)
                .defaultTransactionOptions(
                    TransactionOptions.builder()
                        .writeConcern(WriteConcern.MAJORITY)
                        .readConcern(ReadConcern.MAJORITY)
                        .readPreference(ReadPreference.primaryPreferred())
                        .build()
                )
                .build()
        )

        try {
            println("replicaset name: ${mongoClient.clusterDescription.clusterSettings.requiredReplicaSetName}")
            println("databases in cluster:")
            mongoClient.listDatabaseNames(clientSession).forEach {
                println("  $it")
            }
            println()

            println("requested database name: $databaseName")
            println("requested username: $username")

            val database = mongoClient.getDatabase(databaseName)
            val result = database.runCommand(
                clientSession,
                BsonDocument.parse(
                    """
                {
                  "createUser": "$username",
                  "pwd": "$password",
                  "roles": [
                    "readWrite",
                    "dbAdmin"
                  ]     
                }
            """.trimIndent()
                )
            )

            LOG.info("success: ${result.getDouble("ok")}")
        } catch (e: MongoCommandException) {
            LOG.error("error:")
            LOG.error("  code: ${e.errorCode}")
            LOG.error("  codeName: ${e.errorCodeName}")
            LOG.error("  message: ${e.errorMessage}")
            return 1
        }

        return 0
    }
}
