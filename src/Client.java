import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.util.List;
import java.util.Scanner;

// Client class 
public class Client
{
    //Split a given file into 64KB chunks
    public static void chunkFile(File f){
        int chunkCount = 1;
        int fileSize = 64000;

        byte [] buffer = new byte[fileSize];

        String fName = f.getName();

        try(FileInputStream fis = new FileInputStream(f)){
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
        catch (Exception e)
        {
            System.out.println("Error chunking file: " + e);
        }
    }

    public static void mergeChunks(List<File> files, File dest){

    }

    public static void main(String[] args) throws IOException
    {
        String dir = System.getProperty("user.dir");
        System.out.println(dir);
        File testingChunk = new File("C:\\Users\\Miller Ridgeway\\Desktop\\Distributed Systems\\DistributedFileSystem\\src\\Sample.mp4");
        Client.chunkFile(testingChunk);

//        try
//        {
//            Scanner scn = new Scanner(System.in);
//
//            // getting localhost ip
//            InetAddress ip = InetAddress.getByName("localhost");
//
//            // establish the connection with server port 5056
//            Socket s = new Socket(ip, 444);
//
//            // obtaining input and out streams
//            DataInputStream dis = new DataInputStream(s.getInputStream());
//            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
//
//            // the following loop performs the exchange of
//            // information between client and client handler
//            while (true)
//            {
//                System.out.println(dis.readUTF());
//                String tosend = scn.nextLine();
//                dos.writeUTF(tosend);
//
//                // If client sends exit,close this connection
//                // and then break from the while loop
//                if(tosend.equals("Exit"))
//                {
//                    System.out.println("Closing this connection : " + s);
//                    s.close();
//                    System.out.println("Connection closed");
//                    break;
//                }
//
//                // printing date or time as requested by client
//                String received = dis.readUTF();
//                System.out.println(received);
//            }
//
//            // closing resources
//            scn.close();
//            dis.close();
//            dos.close();
//        }catch(Exception e){
//            e.printStackTrace();
//        }
    }
} 