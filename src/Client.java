import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;

// Client class 
public class Client
{
    //-----FILE CHUNKING----------------------------------
    //Split a given file into 64KB chunks
    public static int chunkFile(File f) throws IOException {
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

        return chunkCount - 1;
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

            Map<String,String> payload = new HashMap<>();

            //Send identification byte
            dos.writeInt(ConnectionType.CLIENT.getValue());
            System.out.println("Sent connect ID type: " + ConnectionType.CLIENT + "\n");

            // the following loop performs the exchange of
            // information between client and Controller Client Handler class
            net_recv: while (true)
            {
                System.out.println(dis.readUTF());
                String tosend = scn.nextLine();

                // If client sends exit,close this connection
                // and then break from the while loop
                switch(tosend){
                    case "Exit":
                        System.out.println("Closing this connection : " + s);
                        s.close();
                        System.out.println("Connection closed");
                        break net_recv;

                    case "Send":
                        System.out.println("Please input filename to send: ");
                        tosend = scn.nextLine();

                        File fileToSend = new File(tosend);
                        int chunks = Client.chunkFile(fileToSend);
                        payload.put("send", Integer.toString(chunks));
                        tosend = MessageParser.mapToString("send", payload);

                        dos.writeUTF(tosend);
                        break;
                }


                //Print & process received data
                String received = dis.readUTF();
                System.out.println("The message received was:" + received);

                //Create and print the parsed message
                MessageParser parser = new MessageParser(received);
                System.out.println("Parsed KV string: " + parser.getParsedKV());
                System.out.println("Parsed Key: " + parser.getKey());
                System.out.println("Parsed Value: "+ parser.getValue());
                System.out.println("");

                switch(parser.getKey()){
                    case "sendTo":
                        System.out.println("sendTo received");
                }
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