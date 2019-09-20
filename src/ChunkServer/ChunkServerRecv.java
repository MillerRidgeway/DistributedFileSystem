package ChunkServer;

import Messages.MessageParser;

import java.io.*;
import java.net.Socket;

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

            //Get the locations to forward to
            String forwardMessage = dis.readUTF();
            System.out.println("This was the forward message from cli: "+ forwardMessage);
            MessageParser parseForward = new MessageParser(forwardMessage);
            System.out.println("Parsed KV string: " + parseForward.getParsedKV());
            System.out.println("Parsed Key: " + parseForward.getKey());
            System.out.println("Parsed Value: " + parseForward.getValue());
            System.out.println("");

            //Get file from client
            int count;
            byte[] buf = new byte[64000];
            FileOutputStream fos = new FileOutputStream(ChunkServer.storageDir + filename);
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
//            if(!parseForward.getValue().equals("null")) {
//                String addrPort = parseForward.getValue().split(",")[0];
//                String addr = addrPort.split("_")[0];
//                int port = Integer.parseInt(addrPort.split("_")[1]);
//
//                ChunkServerForward forward = new ChunkServerForward(addr, port, filename, forwardMessage);
//                forward.start();
//            }

            // closing resources
            fos.close();
            dis.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
