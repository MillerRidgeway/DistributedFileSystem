import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

// Client class 
public class Client
{
    //-----FILE CHUNKING----------------------------------
    //Split a given file into 64KB chunks
    public static void chunkFile(File f) throws IOException {
        int chunkCount = 1;
        int fileSize = 64000;

        byte [] buffer = new byte[fileSize];

        String fName = f.getName();

        try (FileInputStream fis = new FileInputStream(f)){
            BufferedInputStream  bis = new BufferedInputStream(fis);

            //Init to 0, assign in while
            int bytesRemain = 0;
            while((bytesRemain = bis.read(buffer)) > 0){
                String chunkFileName = String.format("%s.%03d", fName, chunkCount++);
                File newFile = new File(f.getParent(), chunkFileName);

                try(FileOutputStream out = new FileOutputStream(newFile)){
                    out.write(buffer,0,bytesRemain);
                }
            }
        }
    }

    public static void mergeChunks(List<File> files, File dest) throws IOException {
        try (FileOutputStream out = new FileOutputStream(dest);
             BufferedOutputStream mergingStream = new BufferedOutputStream(out)) {
            for (File f : files) {
                Files.copy(f.toPath(), mergingStream);
            }
        }
    }

    public static void main(String[] args) throws IOException
    {
//        //Test file chunking
//        String dir = System.getProperty("user.dir");
//        System.out.println(dir);
//        File testingChunk = new File("C:\\Users\\Miller Ridgeway\\Desktop\\Distributed Systems\\DistributedFileSystem\\src\\Sample.mp4");
//        Client.chunkFile(testingChunk);
//
//        //Test merge together
//        System.out.println("Chunking complete!");
//        System.out.println("Now merging back together...");
//
//        List<File> chunks = new ArrayList<>();
//        for(int i = 1; i <= 82; i++){
//            String chunkNum = String.format("%03d", i);
//            chunks.add(new File("C:\\Users\\Miller Ridgeway\\Desktop\\Distributed Systems\\DistributedFileSystem\\src\\Sample.mp4." + chunkNum));
//        }
//
//        File dest = new File("Merged.mp4");
//        dest.createNewFile();
//        mergeChunks(chunks, dest);
//
//        System.out.println("Merge complete.");

        try
        {
            Scanner scn = new Scanner(System.in);

            // getting localhost ip
            InetAddress ip = InetAddress.getByName("localhost");

            // establish the connection with server port 444
            Socket s = new Socket(ip, 444);

            // obtaining input and out streams
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());

            //Send identification byte
            dos.writeInt(ConnectionType.CLIENT.getValue());
            System.out.println("Sent connect ID type: " + ConnectionType.CLIENT.getValue());

            // the following loop performs the exchange of
            // information between client and Controller Client Handler class
            while (true)
            {
                System.out.println(dis.readUTF());
                String tosend = scn.nextLine();
                dos.writeUTF(tosend);

                // If client sends exit,close this connection
                // and then break from the while loop
                if(tosend.equals("Exit"))
                {
                    System.out.println("Closing this connection : " + s);
                    s.close();
                    System.out.println("Connection closed");
                    break;
                }

                // printing date or time as requested by client
                String received = dis.readUTF();
                System.out.println(received);
            }

            // closing resources
            scn.close();
            dis.close();
            dos.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
} 