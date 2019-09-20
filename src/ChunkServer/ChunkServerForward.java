package ChunkServer;

import Messages.ConnectionType;
import Messages.MessageParser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ChunkServerForward extends Thread {
    String addr, fileName, message;
    int port;

    public ChunkServerForward(String addr, int port, String fileName, String message) {
        this.addr = addr;
        this.port = port;
        this.fileName = fileName;
        this.message = message;
    }

    @Override
    public void run() {
        try {
            Socket s = new Socket(InetAddress.getByName(addr), port);
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            out.writeInt(ConnectionType.CHUNK.getValue());
            System.out.println("Sent connect ID type: " + ConnectionType.CHUNK_FORWARD);

            String forwardMessage = message;
            MessageParser parseForward = new MessageParser(forwardMessage);
            System.out.println("Parsed KV string: " + parseForward.getParsedKV());
            System.out.println("Parsed Key: " + parseForward.getKey());
            System.out.println("Parsed Value: " + parseForward.getValue());
            System.out.println("");

            Map<String, String> modifiedForward = new HashMap<>();
            try {
                modifiedForward.put("forwardTo", parseForward.getValue().split(",")[1]);
            }catch (ArrayIndexOutOfBoundsException e){
                modifiedForward.put("forwardTo","null");
            }
            forwardMessage = MessageParser.mapToString("forwardTo", modifiedForward);

            //Send fileChunkName to ChunkServer.ChunkServerRecv
            //as well as the forwarding locations
            out.writeUTF(fileName);
            out.writeUTF(forwardMessage);

            //Send a chunk to the chunk server
            File chunk = new File(ChunkServer.storageDir + fileName);
            byte[] buf = Files.readAllBytes(chunk.toPath());

            out.write(buf);

            // closing resources
            dis.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
