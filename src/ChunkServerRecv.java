import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;

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
            //Get the filename
            String filename = dis.readUTF();
            System.out.println("Uploading: " + filename);

            //Get file from client
            int count;
            byte[] buf = new byte[64000];
            FileOutputStream fos = new FileOutputStream("C:\\Users\\Miller Ridgeway\\Desktop\\Chunk1" + filename);
            while ((count = dis.read(buf)) > 0) {
                fos.write(buf, 0, count);
            }

            //Add the files to locally tracked list of files
            synchronized (ChunkServerClient.files) {
                ChunkServerClient.newFiles.add(filename);
            }
            synchronized (ChunkServerClient.files) {
                ChunkServerClient.files.add(filename);
            }

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
