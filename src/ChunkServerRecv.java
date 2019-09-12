import java.io.*;
import java.net.Socket;

public class ChunkServerRecv extends Thread {
    Socket s;
    DataInputStream dis;
    DataOutputStream out;
    public ChunkServerRecv(Socket s, DataInputStream dis, DataOutputStream out){
        this.s = s;
        this.dis = dis;
        this.out = out;
    }

    @Override
    public void run() {
        try {



            // the following loop performs the exchange of
            // information between client and chunk server receiving


            // closing resources
            dis.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
