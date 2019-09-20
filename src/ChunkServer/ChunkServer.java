package ChunkServer;

import Messages.ConnectionType;

import java.net.*;
import java.io.*;

public class ChunkServer {
    public static int serverPort;
    public static String storageDir;
    public static void main(String[] args) {
        //Start ChunkServer.ChunkServerClient connection to controller - manages heartbeats
        //controller interactions, etc.

        storageDir = args[0];
        System.out.println("Storage dir is: " + storageDir);
        try {
            InetAddress ip = InetAddress.getByName("localhost");
            Socket controllerSocket = new Socket(ip, 444);
            DataInputStream dis = new DataInputStream(controllerSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(controllerSocket.getOutputStream());


            ChunkServerClient controllerConnection = new ChunkServerClient(controllerSocket, dis, out);
            controllerConnection.start();
        } catch (Exception e) {
            System.out.println("Error connecting chunk client to controller server: " + e);
        }


        ServerSocket listener;
        Socket connection;

        //Init server socket on PORT_NUMBER
        try {
            listener = new ServerSocket(0);
            System.out.println("Listening on port: " + listener.getLocalPort());
            serverPort = listener.getLocalPort();
        } catch (Exception e) {
            System.out.println("ERROR: Unexpected chunk server shutdown");
            System.out.println("Error is: " + e);
            return;
        }

        //Bind to socket and spawn new chunk server receiver or pusher
        while (true) {
            try {
                connection = listener.accept();
                System.out.println("New connection: " + connection);

                //I/O Streams
                DataInputStream input = new DataInputStream(connection.getInputStream());
                DataOutputStream output = new DataOutputStream(connection.getOutputStream());

                //Determine connection type from first integer sent
                int threadType = input.readInt();
                System.out.println("New connection type is: " + ConnectionType.fromInteger(threadType) + "\n");

                switch (ConnectionType.fromInteger(threadType)) {
                    case CLIENT_SEND:
                        ChunkServerRecv recv = new ChunkServerRecv(connection, input, output);
                        recv.start();
                        break;
                    case CLIENT_PULL:
                        ChunkServerPush push = new ChunkServerPush(connection, input, output);
                        push.start();
                        break;
                }
            } catch (Exception e) {

            }
        }
    }
}