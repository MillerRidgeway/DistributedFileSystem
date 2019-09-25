package ChunkServer;

import Messages.ConnectionType;
import Messages.MessageParser;

import java.net.*;
import java.io.*;
import java.security.MessageDigest;

public class ChunkServer {
    static int serverPort;
    static String storageDir;

    public static void main(String[] args) {
        //Start ChunkServer.ChunkServerClient connection to controller - manages heartbeats
        //controller interactions, etc.


        System.out.println("Storage dir is: " + storageDir);
        try {
            InetAddress controllerAddr = InetAddress.getByName(args[0]);
            int controllerPort = Integer.parseInt(args[1]);
            storageDir = args[2];
            Socket controllerSocket = new Socket(controllerAddr, controllerPort);
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
                    case CHUNK_FORWARD:
                        ChunkServerRecv recv = new ChunkServerRecv(connection, input, output);
                        recv.start();
                        break;
                    case CLIENT_PULL:
                        ChunkServerPush push = new ChunkServerPush(connection, input, output);
                        push.start();
                        break;
                    case FORWARD_TO:
                        String message = input.readUTF();
                        MessageParser parsedInput = new MessageParser(message);
                        String filename = input.readUTF();
                        String addr = parsedInput.getValue().split("_")[0];
                        int port = Integer.parseInt(parsedInput.getValue().split("_")[1]);

                        ChunkServerForward pushForward = new ChunkServerForward(addr, port, filename, message);
                        pushForward.start();
                        break;
                }
            } catch (Exception e) {

            }
        }
    }
}
