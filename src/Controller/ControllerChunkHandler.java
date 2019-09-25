package Controller;

import Messages.MessageParser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ControllerChunkHandler extends Thread {
    final DataInputStream input;
    final DataOutputStream output;
    final Socket connection;

    public ControllerChunkHandler(Socket s, DataInputStream in, DataOutputStream out) {
        this.connection = s;
        this.input = in;
        this.output = out;
    }

    @Override
    public void run() {
        String received;
        try {
            while (true) {
                output.writeUTF("Connection to controller successful.");

                received = input.readUTF();

                MessageParser parser = new MessageParser(received);
                switch (parser.getKey()) {
                    case "minorHeartbeat":
                        long spaceHere = input.readLong();
                        Controller.spaceAtServer.put(connection.getInetAddress().getHostAddress() +
                                "_" + Controller.serverPorts.get(connection), spaceHere);

                        if (!parser.getValue().equals("null")) {
                            String[] files = parser.getValue().split(",");
                            System.out.println("New files at: " + connection.getInetAddress().getHostAddress() + "_" + connection.getPort());
                            System.out.println("Updating controller index with the following files: " + parser.getValue());
                            synchronized (Controller.files) {
                                for (int i = 0; i < files.length; i++) {
                                    Controller.addFile(files[i], connection.getInetAddress().getHostAddress()
                                            + "_" + Controller.serverPorts.get(connection));
                                }
                            }
                        }
                        break;
                    default:
                        output.writeUTF("Invalid message type in ControllerChunkHandler");
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Likely have lost a connection to a chunk server");
            System.out.println("Error message is: " + e);
        }
    }

}
