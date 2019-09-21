package Controller;

import Messages.MessageParser;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class ControllerClientHandler extends Thread {
    final DataInputStream input;
    final DataOutputStream output;
    final Socket connection;

    Map<String, String> payload = new HashMap<>();

    ControllerClientHandler(Socket s, DataInputStream in, DataOutputStream out) {
        this.connection = s;
        this.input = in;
        this.output = out;
    }

    static String getForwardList(int rep, String originalDestination) throws UnknownHostException {
        synchronized (Controller.currentChunkConnections) {
            if (Controller.currentChunkConnections.size() < rep) {
                System.out.println("Need more than " + rep + " servers to forward");
                return "null";
            }
            String list = "";
            String toBeAdded = Controller.getChunkServer();
            boolean first = true;
            for (int i = 0; i < rep; i++) {
                while (originalDestination.contains(toBeAdded) || list.contains(toBeAdded)) {
                    System.out.println("Finding new forward addr");
                    toBeAdded = Controller.getChunkServer();
                }
                if (first) {
                    list += toBeAdded;
                    first = false;
                } else
                    list += "-" + toBeAdded;
            }
            return list;
        }
    }

    @Override
    public void run() {
        String received;
        String toreturn;

        try {
            while (true) {
                // Ask user what he wants
                output.writeUTF("What do you want? [Send | Pull | Exit]..\n");

                // receive the answer from client
                received = input.readUTF();

                if (received.equals("exit")) {
                    System.out.println("Client " + this.connection + " sends exit...");
                    System.out.println("Closing this connection.");
                    this.connection.close();
                    System.out.println("Connection closed");
                    break;
                }

                // write on output stream based on the
                // answer from the client
                //Create and print the parsed message
                MessageParser parser = new MessageParser(received);
                System.out.println("Parsed KV string: " + parser.getParsedKV());
                System.out.println("Parsed Key: " + parser.getKey());
                System.out.println("Parsed Value: " + parser.getValue());

                switch (parser.getKey()) {
                    case "send":
                        String chunkServList = "";
                        String forwardList = "";
                        for (int i = 0; i < Integer.parseInt(parser.getValue()); i++) {
                            String toBeAdded = Controller.getChunkServer();
                            if (i == Integer.parseInt(parser.getValue()) - 1) {
                                chunkServList += toBeAdded;
                                forwardList += getForwardList(2, toBeAdded);
                            } else {
                                chunkServList += toBeAdded + ",";
                                forwardList += getForwardList(2, toBeAdded) + ",";
                            }
                        }
                        payload.put("sendTo", chunkServList);
                        toreturn = MessageParser.mapToString("sendTo", payload);
                        output.writeUTF(toreturn);
                        System.out.println("Replied with sendTo");

                        payload.put("forwardTo", forwardList);
                        toreturn = MessageParser.mapToString("forwardTo", payload);
                        output.writeUTF(toreturn);
                        System.out.println("Replied with forwardTo" + "\n");

                        break;
                    case "pull":
                        String filename = parser.getValue();

                        String fileList = "";
                        String serverList = "";
                        boolean first = true;
                        for (Map.Entry<String, String> e : Controller.files.entrySet()) {
                            if (e.getKey().contains(filename)) {
                                String[] serversPerFile = e.getValue().split(",");
                                Random r = new Random();
                                String server = serversPerFile[r.nextInt(serversPerFile.length)];

                                if (first) {
                                    serverList += server;
                                    fileList += e.getKey();
                                    first = false;
                                } else {
                                    serverList += "," + server;
                                    fileList += "," + e.getKey();
                                }
                            }
                        }
                        payload.put("pullFrom", serverList);
                        toreturn = MessageParser.mapToString("pullFrom", payload);
                        output.writeUTF(toreturn);
                        System.out.println("Replied with pullFrom" + "\n");

                        payload.put("fileList", fileList);
                        toreturn = MessageParser.mapToString("fileList", payload);
                        output.writeUTF(toreturn);
                        System.out.println("Replied with fileList" + "\n");

                        break;
                    default:
                        output.writeUTF("Invalid input");
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
