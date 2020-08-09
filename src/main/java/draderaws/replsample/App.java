package draderaws.replsample;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.postgresql.PGProperty;
import org.postgresql.replication.PGReplicationStream;
import org.postgresql.PGConnection;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Logical Replication Sample" );

        Properties props = new Properties();
        PGProperty.ASSUME_MIN_SERVER_VERSION.set(props, "9.4");
        PGProperty.REPLICATION.set(props, "database");
        PGProperty.PREFER_QUERY_MODE.set(props, "simple");


        PGProperty.USER.set(props, "samp_repl_user");
        PGProperty.PASSWORD.set(props, "<your password>");
        String url = "jdbc:postgresql://<your RDS PostgreSQl url>:5432/postgres";
        
        try (Connection conn = DriverManager.getConnection(url, props)) {

            if (conn == null) {
                System.out.println("Failed to make connection!");
            } else {
                System.out.println("Connected to the database");
                PGConnection replConnection = conn.unwrap(PGConnection.class);

                try {
                    System.out.println("Dropping logical replication slot, just in case it was left.");
                    replConnection.getReplicationAPI()
                        .dropReplicationSlot("sample_repl_slot");                    
                } catch (SQLException e) {
                    if( e.getSQLState().equals("42704")) { 
                        // replication slot does not exist. ignore.
                    } else {
                        System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
                    }
                }

                System.out.println("Creating logical replication slot.");
                replConnection.getReplicationAPI()
                    .createReplicationSlot()
                    .logical()
                    .withSlotName("sample_repl_slot")
                    .withOutputPlugin("test_decoding")
                    .make();
                
                System.out.println("Starting logical replication stream.");
                PGReplicationStream stream = replConnection.getReplicationAPI()
                    .replicationStream()
                    .logical()
                    .withSlotName("sample_repl_slot")
                    .withSlotOption("include-xids", false)
                    .withSlotOption("skip-empty-xacts", true)
                    .withStatusInterval( 20, TimeUnit.SECONDS)
                    .start();

                
                System.out.println("Listening for replication messages");
                System.out.println("==================================");
                while (true) {
                    //non blocking receive message
                    ByteBuffer msg = stream.readPending();
                
                    if (msg == null) {
                        TimeUnit.MILLISECONDS.sleep(10L);
                        continue;
                    }
                
                    int offset = msg.arrayOffset();
                    byte[] source = msg.array();
                    int length = source.length - offset;
                    System.out.println(new String(source, offset, length));
                
                    //feedback
                    stream.setAppliedLSN(stream.getLastReceiveLSN());
                    stream.setFlushedLSN(stream.getLastReceiveLSN());
                }                  

            }

        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s\n", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
