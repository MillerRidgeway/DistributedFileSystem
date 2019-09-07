import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ControllerClientHandler extends Thread {
    final DataInputStream input;
    final DataOutputStream output;
    final Socket connection;

    DateFormat fordate = new SimpleDateFormat("yyyy/MM/dd");
    DateFormat fortime = new SimpleDateFormat("hh:mm:ss");

    Map<String, String> payload = new HashMap<>();

    public ControllerClientHandler(Socket s, DataInputStream in, DataOutputStream out){
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


                // Ask user what he wants
                output.writeUTF("What do you want?[Date | Time | Send]..\n" +
                        "Type Exit to terminate connection.");

                // receive the answer from client
                received = input.readUTF();

                if (received.equals("Exit")) {
                    System.out.println("Client " + this.connection + " sends exit...");
                    System.out.println("Closing this connection.");
                    this.connection.close();
                    System.out.println("Connection closed");
                    break;
                }

                // creating Date object
                Date date = new Date();

                // write on output stream based on the
                // answer from the client
                switch (received) {
                    case "Date":
                        toreturn = fordate.format(date);
                        output.writeUTF(toreturn);
                        break;

                    case "Time":
                        toreturn = fortime.format(date);
                        output.writeUTF(toreturn);
                        break;
                    case "Send":
                        payload.put("sendTo", Controller.getChunkServer().getHostAddress());
                        toreturn = "{sendTo: " + payload.get("sendTo") + "}";
                        output.writeUTF(toreturn);
                        break;
                    default:
                        output.writeUTF("Invalid input");
                        break;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

}
