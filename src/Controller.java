import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class Controller {

    //Enum for connection types to controller
    public enum ConnectionType {
        CHUNK(0), CLIENT(10);

        private final int value;
        private ConnectionType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ConnectionType fromInteger(int x) {
            switch(x) {
                case 0:
                    return CHUNK;
                case 10:
                    return CLIENT;
            }
            return null;
        }
    }
    public static void main(String[] args) {

        //Config information
        final int PORT_NUMBER = 444;
        final String HOST = "127.0.0.1";

        //Socket / Server
        ServerSocket listener;
        Socket connection;
        ArrayList<Thread> currentConnections = new ArrayList<>();

        //Init server socket on PORT_NUMBER
        try{
            listener = new ServerSocket(PORT_NUMBER);
            System.out.println("Listening on port: " + PORT_NUMBER);
        }
        catch(Exception e)
        {
            System.out.println("ERROR: Unexpected server shutdown");
            System.out.println("Error is: " + e);
            return;
        }

        //Bind to socket and spawn client handler
        while(true) {
            try{
                connection = listener.accept();

                System.out.println("New client connection: " + connection);

                //I/O Streams
                DataInputStream input = new DataInputStream(connection.getInputStream());
                DataOutputStream output = new DataOutputStream(connection.getOutputStream());

                //New client handler
                Thread t = new ControllerClientHandler(connection, input, output);
                currentConnections.add(t);
                t.start();


            }
            catch(Exception e){
                System.out.println("Error in binding / writing");
                System.out.println("Error is: " + e);
            }
        }

    }
}
