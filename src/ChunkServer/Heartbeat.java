package ChunkServer;

import java.io.DataOutputStream;
import java.io.File;
import java.net.Socket;
import java.util.TimerTask;

public class Heartbeat extends TimerTask {
    DataOutputStream out;
    Socket s;

    public Heartbeat(Socket s, DataOutputStream out) {
        this.out = out;
        this.s = s;
    }

    @Override
    public void run() {
        synchronized (ChunkServerClient.newFiles) {
            try {
                String payload = "null";
                boolean firstIter = true;
                File f = new File(ChunkServer.storageDir);
                long usableSpace = f.getUsableSpace();

                if (ChunkServerClient.newFiles.size() > 0) {
                    for (String s : ChunkServerClient.newFiles) {
                        if (firstIter) {
                            payload = s;
                            firstIter = false;
                        } else
                            payload += "," + s;
                    }
                }
                out.writeUTF("{minorHeartbeat:" + payload + "}");
                out.writeLong(usableSpace);
                ChunkServerClient.newFiles.clear();
            } catch (Exception e) {
                System.out.println("Error in sending minor heartbeat: " + e);
            }
        }
    }
}
