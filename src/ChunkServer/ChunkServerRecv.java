package ChunkServer;

import Messages.MessageParser;

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
            System.out.println("Receiving new file: " + filename);

            //Get the locations to forward to
            String forwardMessage = dis.readUTF();
            MessageParser parseForward = new MessageParser(forwardMessage);

            //Get file from client
            int count;
            byte[] buf = new byte[64000];
            FileOutputStream fos = new FileOutputStream(ChunkServer.storageDir + filename);
            while ((count = dis.read(buf)) > 0) {
                fos.write(buf, 0, count);
            }

            //Store the file hash
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA1");
            File newChunk = new File(ChunkServer.storageDir + filename);
            String checksum = FileHash.getFileChecksum(sha1Digest, newChunk);
            synchronized (ChunkServerClient.fileHashes) {
                ChunkServerClient.fileHashes.put(filename, checksum);
            }
            System.out.println("Hash for file " + filename + " is:" + checksum);

            //Add the files to locally tracked list of files
            synchronized (ChunkServerClient.files) {
                ChunkServerClient.newFiles.add(filename);
            }
            synchronized (ChunkServerClient.files) {
                ChunkServerClient.files.add(filename);
            }

            System.out.println("Upload complete: " + filename);

            //Forward the file based on the contents of the forward message
            if (!parseForward.getValue().contains("null")) {
                String addrPort = parseForward.getValue().split("-")[0];
                String addr = addrPort.split("_")[0];
                int port = Integer.parseInt(addrPort.split("_")[1]);


                ChunkServerForward forward = new ChunkServerForward(addr, port, filename, forwardMessage);
                forward.start();
            }

            // closing resources
            fos.close();
            dis.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
