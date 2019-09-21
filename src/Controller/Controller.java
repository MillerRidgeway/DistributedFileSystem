package Controller;

import Messages.ConnectionType;

import java.net.*;
import java.io.*;
import java.util.*;

public class Controller {
    public static ArrayList<Socket> currentChunkConnections = new ArrayList<>();
    public static Map<String, String> files = new TreeMap<>();
    public static Map<Socket, Integer> serverPorts = new HashMap<>();

    static String getChunkServer() throws UnknownHostException {
        synchronized (currentChunkConnections) {
            //TODO - Make method select a server at random until space becomes an issue
            //Do this based on available space within each chunk server - for now just give a
            //random chunk server
            Random r = new Random();
            int randIndex = r.nextInt(currentChunkConnections.size());

            Socket chunkServer = currentChunkConnections.get(randIndex);
            return chunkServer.getInetAddress().getHostAddress() + "_" +
                    serverPorts.get(chunkServer);
        }
    }

    static void addFile(String filename, String addr) {
        if (files.get(filename) == null) {
            files.put(filename, addr);
        } else {
            String addrList = files.get(filename);
            files.put(filename, addrList + "," + addr);
        }
    }

    public static void main(String[] args) {
        //Host port
        final int PORT_NUMBER = 444;

        //Socket / Server / Keepalive
        ServerSocket listener;
        Socket connection;
        Timer statusCheck = new Timer();

        //Init server socket on PORT_NUMBER
        try {
            listener = new ServerSocket(PORT_NUMBER);
            System.out.println("Controller server listening on port: " + PORT_NUMBER);
        } catch (Exception e) {
            System.out.println("ERROR: Unexpected server shutdown");
            System.out.println("Error is: " + e);
            return;
        }

        //Bind to socket and spawn handler based on client type
        while (true) {
            try {
                connection = listener.accept();
                System.out.println("New connection: " + connection);

                //I/O Streams
                DataInputStream input = new DataInputStream(connection.getInputStream());
                DataOutputStream output = new DataOutputStream(connection.getOutputStream());

                //Determine connection type from first integer sent
                int threadType = input.readInt();
                System.out.println("New connection type is: " + ConnectionType.fromInteger(threadType));

                //New connection thread based on connection type
                switch (ConnectionType.fromInteger(threadType)) {
                    case CLIENT:
                        System.out.println("\n");
                        Thread clientThread = new ControllerClientHandler(connection, input, output);
                        clientThread.start();
                        break;
                    case CHUNK:
                        //Create a new chunk server handler thread and store the server
                        //port reported by the chunk server client (different from the)
                        //client port
                        int serverPort = input.readInt();
                        Controller.serverPorts.put(connection, serverPort);
                        System.out.println("Newly connected chunk server port is: " + serverPort);

                        ControllerChunkHandler chunkThread = new ControllerChunkHandler(connection, input, output);
                        currentChunkConnections.add(chunkThread.connection);
                        chunkThread.start();

                        //Keepalive monitor
                        Monitor m = new Monitor(connection.getInetAddress().getHostName(), serverPort);
                        statusCheck.schedule(m, 0, 10000);

                        System.out.println("Chunk servers connected: " + currentChunkConnections.size() + "\n");
                        break;
                    default:
                        System.out.println("Unrecognized connection attempt...ignoring");
                        break;
                }
            } catch (Exception e) {
                System.out.println("Error in binding / writing");
                System.out.println("Error is: " + e);
            }
        }
    }
}
