import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ChunkServerRecv extends Thread {
    final Socket s;
    final DataInputStream dis;
    final DataOutputStream out;

    public ChunkServerRecv(Socket s, DataInputStream dis, DataOutputStream out) {
        this.s = s;
        this.dis = dis;
        this.out = out;
    }

    @Override
    public void run() {
        try {
            //Print file-chunk name
            String filename = dis.readUTF();
            System.out.println("Uploading: " + filename);

            //Get file from client
            int count;
            byte[] buf = new byte[64000];
            FileOutputStream fos = new FileOutputStream("C:\\Users\\Miller Ridgeway\\Desktop\\" + filename);
            while((count = dis.read(buf)) > 0){
                fos.write(buf, 0, count);
            }

            Controller.addFile(s.getRemoteSocketAddress().toString(), filename);
            System.out.println("Upload complete: " + filename);

            // closing resources
            fos.close();
            dis.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
