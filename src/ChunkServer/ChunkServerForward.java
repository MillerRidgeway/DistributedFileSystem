package ChunkServer;

import Messages.ConnectionType;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ChunkServerForward extends Thread {
    String addr, fileName;
    int port;
    public ChunkServerForward(String addr, int port, String fileName){
        this.addr = addr;
        this.port = port;
        this.fileName = fileName;
    }

    @Override
    public void run() {
        try {

            Socket s = new Socket(InetAddress.getByName(addr), port);
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            out.writeInt(ConnectionType.CHUNK.getValue());
            System.out.println("Sent connect ID type: " + ConnectionType.CHUNK_FORWARD);


            // closing resources
            dis.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
