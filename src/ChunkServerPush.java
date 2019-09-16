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

            //Array of chunks that match the filename on this server
            File dir = new File("C:\\Users\\Miller Ridgeway\\Desktop");
            File[] foundFiles = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith(filename);
                }
            });

            //Tell the client how many chunks you will be sending
            out.writeInt(foundFiles.length);
            int n = 0;
            byte [] buf = new byte[64000];
            //Send the chunks
            for (int i = 0; i < foundFiles.length; i++) {
                out.writeUTF(foundFiles[i].getName());
                out.writeLong(foundFiles[i].length());
                FileInputStream fis = new FileInputStream(foundFiles[i]);
                while((n =fis.read(buf)) != -1){
                    out.write(buf,0,n);
                    out.flush();
                }
            }

            System.out.println("Sent " + foundFiles.length + " chunks of the file " + filename);

            dis.close();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
