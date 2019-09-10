import java.net.*;
import java.util.Scanner;
import java.io.*;


public class ChunkServerClient extends Thread {
    Socket s;
    public ChunkServerClient(Socket s){
        this.s = s;
    }

    @Override
    public void run() {
        try {
            Scanner scn = new Scanner(System.in);

            // obtaining input and out streams
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());

            dos.writeInt(ConnectionType.CHUNK.getValue());
            System.out.println("Sent connect ID type: " + ConnectionType.CHUNK.getValue());

            // the following loop performs the exchange of
            // information between client and client handler
            while (true) {
                System.out.println(dis.readUTF());
                String tosend = scn.nextLine();
                dos.writeUTF(tosend);

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
            dos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
