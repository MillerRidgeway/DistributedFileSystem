package ChunkServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.*;
import java.io.FileOutputStream;
import java.net.Socket;
import java.nio.file.Files;

public class ChunkServerPush extends Thread {
    final Socket s;
    final DataInputStream dis;
    final DataOutputStream out;

    public ChunkServerPush(Socket s, DataInputStream dis, DataOutputStream out) {
        this.s = s;
        this.dis = dis;
        this.out = out;
    }

    @Override
    public void run() {
        try {
            String filename = dis.readUTF();
            System.out.println("Searching for " + filename);

            //Send a chunk to the chunk server
            File chunk = new File(ChunkServer.storageDir + filename);
            byte[] buf = Files.readAllBytes(chunk.toPath());

            out.write(buf);

            System.out.println("Sent " + filename);

            dis.close();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
