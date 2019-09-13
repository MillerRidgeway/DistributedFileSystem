import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class Controller {
    public static ArrayList<InetAddress> currentChunkConnections = new ArrayList<>();

    public static InetAddress getChunkServer() throws UnknownHostException{
        //Do this based on available space within each chunk server - for now just give a chunk server
//        for(Thread t: currentChunkConnections){
//            if(t instanceof ControllerChunkHandler) return ((ControllerChunkHandler) t).connection.getInetAddress();
//        }
        return currentChunkConnections.get(0);
    }

    public static void main(String[] args) {
        //Host port
        final int PORT_NUMBER = 444;

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
            System.out.println("ERROR: Unexpected server shutdown");
            System.out.println("Error is: " + e);
            return;
        }

        //Bind to socket and spawn client handler
        while(true) {
            try{
                connection = listener.accept();
                System.out.println("New connection: " + connection);

                //I/O Streams
                DataInputStream input = new DataInputStream(connection.getInputStream());
                DataOutputStream output = new DataOutputStream(connection.getOutputStream());

                //Determine connection type from first integer sent
                int threadType = input.readInt();
                System.out.println("New connection type is: " + ConnectionType.fromInteger(threadType));

                //New connection thread based on connection type
                //Only two cases, client connection or new chunk server
                //Anything else is unrecognized
                switch(ConnectionType.fromInteger(threadType)){
                    case CLIENT:
                        Thread clientThread = new ControllerClientHandler(connection, input, output);
                        clientThread.start();
                        break;
                    case CHUNK:
                        ControllerChunkHandler chunkThread = new ControllerChunkHandler(connection, input, output);
                        currentChunkConnections.add(chunkThread.connection.getInetAddress());
                        chunkThread.start();
                        System.out.println("Chunk servers connected: " + currentChunkConnections.size() + "\n");
                        break;
                    default:
                        System.out.println("Unrecognized connection attempt...ignoring");
                        break;

                }
            }
            catch(Exception e){
                System.out.println("Error in binding / writing");
                System.out.println("Error is: " + e);
            }
        }
    }
}
