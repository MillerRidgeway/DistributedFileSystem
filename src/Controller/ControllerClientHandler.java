package Controller;

import Messages.ConnectionType;
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

    static String getForwardList(int forwardCount, String alreadyAtList) throws UnknownHostException {
        synchronized (Controller.currentChunkConnections) {
            if (Controller.currentChunkConnections.size() < forwardCount) {
                System.out.println("Need more than " + forwardCount + " servers to forward");
                return "null";
            }
            String list = "";
            String toBeAdded = Controller.getChunkServer();
            boolean first = true;
            for (int i = 0; i < forwardCount; i++) {
                while (alreadyAtList.contains(toBeAdded) || list.contains(toBeAdded)) {
                    //System.out.println("Finding new forward addr");
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

                        if (Controller.replicationScheme.equalsIgnoreCase("erasure"))
                            payload.put("forwardTo", "null");
                        else
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
                    case "corruptChunkFound":
                        String corruptFileName = "";
                        String corruptServer = "";
                        String[] serversWithFile = null;
                        synchronized (Controller.files) {
                            corruptFileName = parser.getValue().split(",")[0];
                            corruptServer = parser.getValue().split(",")[1];
                            serversWithFile = Controller.files.get(corruptFileName).split(",");
                            payload.put("forwardTo", corruptServer);
                        }

                        System.out.println("Found corrupt chunk at:" + corruptServer);

                        Random r = new Random();
                        String sendFromServer = serversWithFile[r.nextInt(serversWithFile.length)];
                        while (sendFromServer.equals(corruptServer)) {
                            sendFromServer = serversWithFile[r.nextInt(serversWithFile.length)];
                        }

                        String addr = sendFromServer.split("_")[0];
                        int port = Integer.parseInt(sendFromServer.split("_")[1]);
                        System.out.println("Sending forwardTo request to: " + sendFromServer + "\n");

                        Socket s = new Socket(addr, port);
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeInt(ConnectionType.FORWARD_TO.getValue());
                        out.writeUTF(MessageParser.mapToString("forwardTo", payload));
                        out.writeUTF(corruptFileName);
                        s.close();


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
