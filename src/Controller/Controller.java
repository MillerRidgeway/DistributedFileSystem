package Controller;

import Messages.ConnectionType;

import java.net.*;
import java.io.*;
import java.util.*;

public class Controller {
    public static final ArrayList<Socket> currentChunkConnections = new ArrayList<>();
    public static final Map<String, String> files = new TreeMap<>();
    public static final Map<String, String> servers = new HashMap<>();
    public static final Map<Socket, Integer> serverPorts = new HashMap<>();
    public static final Map<String, Long> spaceAtServer = new TreeMap<>();
    public static String replicationScheme = "";

    static String getChunkServer() throws UnknownHostException {
        ArrayList<Socket> validConnections = new ArrayList<>();

        synchronized (currentChunkConnections) {
            for (Socket s : currentChunkConnections) {
                String key = s.getInetAddress().getHostAddress();
                String keyWPort = key + "_" + serverPorts.get(s);
                if (spaceAtServer.get(keyWPort) > 1000000000) {
                    validConnections.add(s);
                }
            }
        }

        String server = "";
        Socket chunkServer = null;
        if (validConnections.size() == 0) {
            Long mostSpace = 0L;
            for (Map.Entry<String, Long> e : spaceAtServer.entrySet()) {
                if (e.getValue() > mostSpace) {
                    mostSpace = e.getValue();
                    server = e.getKey();
                }
            }

            synchronized (currentChunkConnections) {
                for (Socket s : currentChunkConnections) {
                    if (s.getInetAddress().getHostAddress().equalsIgnoreCase(server))
                        chunkServer = s;
                }
            }
        } else {
            Random r = new Random();
            int randIndex = r.nextInt(validConnections.size());

            chunkServer = validConnections.get(randIndex);
        }
        return chunkServer.getInetAddress().getHostAddress() + "_" +
                serverPorts.get(chunkServer);
    }

    static void addFile(String filename, String addr) {
        synchronized (files) {
            if (files.get(filename) == null) {
                files.put(filename, addr);
            } else {
                String addrList = files.get(filename);
                files.put(filename, addrList + "," + addr);
            }
        }
        synchronized (servers) {
            if (servers.get(addr) == null) {
                servers.put(addr, filename);
            } else {
                String fileList = servers.get(addr);
                servers.put(addr, fileList + "," + filename);
            }
        }
    }

    public static void main(String[] args) {
        //Host port
        final int PORT_NUMBER = Integer.parseInt(args[0]);
        replicationScheme = args[1];

        System.out.println("Started controller service on port " + PORT_NUMBER +
                " using encoding scheme " + replicationScheme);

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

                        synchronized (currentChunkConnections) {
                            ControllerChunkHandler chunkThread = new ControllerChunkHandler(connection, input, output);
                            currentChunkConnections.add(chunkThread.connection);
                            chunkThread.start();
                        }

                        //Keepalive monitor
                        Monitor m = new Monitor(connection.getInetAddress().getHostAddress(), serverPort);
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
