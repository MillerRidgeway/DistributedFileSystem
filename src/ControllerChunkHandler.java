import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
        String toreturn;

        try {
            while (true) {
                output.writeUTF("Connected to controller - [Exit] to exit");

                // receive the answer from client
                received = input.readUTF();

                if (received.equalsIgnoreCase("exit")) {
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
                System.out.println("Parsed Value: " + parser.getValue() + "\n");

                switch (parser.getKey()) {
                    case "minorHeartbeat":
                        String[] files = parser.getValue().split(",");
                        System.out.println("Updating controller index with the following files: " + parser.getValue() + "\n");
                        if (parser.getValue() != "null") {
                            for (int i = 0; i < files.length; i++) {
                                Controller.addFile(files[i], connection.getInetAddress().getHostAddress());
                                if (Controller.files.get(files[i]).split(",").length < 3) {
                                    System.out.println("Less than three copies of " + files[i] + " forwarding to active server");
                                }
                            }
                        }
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
