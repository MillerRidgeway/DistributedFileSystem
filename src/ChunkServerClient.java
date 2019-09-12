import java.net.*;
import java.util.Scanner;
import java.io.*;


public class ChunkServerClient extends Thread {
    final Socket s;
    final DataInputStream dis;
    final DataOutputStream out;

    public ChunkServerClient(Socket s, DataInputStream dis, DataOutputStream out){
        this.s = s;
        this.dis = dis;
        this.out = out;
    }

    @Override
    public void run() {
        try {
            Scanner scn = new Scanner(System.in);

            out.writeInt(ConnectionType.CHUNK.getValue());
            System.out.println("Sent connect ID type: " + ConnectionType.CHUNK.getValue());

            // the following loop performs the exchange of
            // information between client and client handler
            while (true) {
                System.out.println(dis.readUTF());
                String tosend = scn.nextLine();
                out.writeUTF(tosend);

                // If client sends exit,close this connection
                // and then break from the while loop
                if (tosend.equals("Exit")) {
                    System.out.println("Closing this connection : " + s);
                    s.close();
                    System.out.println("Connection closed");
                    break;
                }

                // printing date or time as requested by client
                String received = dis.readUTF();
                System.out.println(received);
            }

            // closing resources
            scn.close();
            dis.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}