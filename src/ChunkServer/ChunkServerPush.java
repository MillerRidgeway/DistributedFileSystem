package ChunkServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.*;
import java.io.FileOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.security.MessageDigest;

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

            //Check the file checksum before sending
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA1");
            File newChunk = new File(ChunkServer.storageDir + filename);
            String actualChecksum = FileHash.getFileChecksum(sha1Digest, newChunk);
            String expectedChecksum = ChunkServerClient.fileHashes.get(filename);
            if (!actualChecksum.equals(expectedChecksum)) {
                System.err.println("File hashes do not match, exiting connection for this chunk");
                s.close();
            } else {

                //Send a chunk to the chunk server
                File chunk = new File(ChunkServer.storageDir + filename);
                byte[] buf = Files.readAllBytes(chunk.toPath());

                out.write(buf);

                System.out.println("Sent " + filename);

                dis.close();
                out.close();
                s.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
