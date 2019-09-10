import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class ChunkServer {

    public static void main(String[] args) {
        //Start ChunkServerClient connection to controller - manages heartbeats
        //controller interactions, etc.
        try {
            InetAddress ip = InetAddress.getByName("localhost");
            Socket controllerSocket = new Socket(ip, 444);
            ChunkServerClient controllerConnection = new ChunkServerClient(controllerSocket);
            controllerConnection.start();
        }
        catch(Exception e){
            System.out.println("Error connecting chunk client to controller server: "+ e);
        }

        //Host port
        final int PORT_NUMBER = 555;

        //Socket / Server
        ServerSocket listener;
        Socket connection;

        //Init server socket on PORT_NUMBER
        try{
            listener = new ServerSocket(PORT_NUMBER);
            System.out.println("Listening on port: " + PORT_NUMBER);
        }
        catch(Exception e)
        {
            System.out.println("ERROR: Unexpected chunk server shutdown");
            System.out.println("Error is: " + e);
            return;
        }

        //Bind to socket and spawn new chunk server client
        while(true) {
            try {
                connection = listener.accept();
                System.out.println("New connection: " + connection);

                //I/O Streams
                DataInputStream input = new DataInputStream(connection.getInputStream());
                DataOutputStream output = new DataOutputStream(connection.getOutputStream());

                //Determine connection type from first integer sent
                int threadType = input.readInt();
                System.out.println("New connection type is: " + ConnectionType.fromInteger(threadType) +"\n");
            }

            catch(Exception e){
                System.out.println("Error in binding / writing");
                System.out.println("Error is: " + e);
            }
        }
    }
}
