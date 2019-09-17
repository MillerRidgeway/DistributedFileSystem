package Client;

import Messages.ConnectionType;
import Messages.MessageParser;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;

// Client.Client class
public class Client {
    //-----FILE CHUNKING----------------------------------
    //Split a given file into 64KB chunks
    public static int chunkFile(File f) throws IOException {
        int chunkCount = 1;
        int fileSize = 64000;

        byte[] buffer = new byte[fileSize];

        String fName = f.getName();

        try (FileInputStream fis = new FileInputStream(f)) {
            BufferedInputStream bis = new BufferedInputStream(fis);

            //Init to 0, assign in while
            int bytesRemain = 0;
            while ((bytesRemain = bis.read(buffer)) > 0) {
                String chunkFileName = String.format("%s.%03d", fName, chunkCount++);
                File newFile = new File(f.getParent(), chunkFileName);

                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, bytesRemain);
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

    public static void main(String[] args) throws IOException {
//        //Test file chunking
//        String dir = System.getProperty("user.dir");
//        System.out.println(dir);
//        File testingChunk = new File("C:\\Users\\Miller Ridgeway\\IdeaProjects\\DistributedFilesystem\\src\\testingFile.pdf");
//        int numChunks = Client.Client.chunkFile(testingChunk);
//
//        //Test merge together
//        System.out.println("Chunking complete!");
//        System.out.println("Now merging back together...");
//
//        List<File> chunkFiles = new ArrayList<>();
//        for (int i = 1; i <= 4; i++) {
//            String chunkNum = String.format("%03d", i);
//            chunkFiles.add(new File("C:\\Users\\Miller Ridgeway\\Desktop\\testingFile.pdf." + chunkNum));
//        }
//
//        File dest = new File("Merged.pdf");
//        dest.createNewFile();
//        mergeChunks(chunkFiles, dest);

//        System.out.println("Merge complete.");

        try {
            Scanner scn = new Scanner(System.in);

            // getting localhost ip
            InetAddress ip = InetAddress.getByName("localhost");

            // establish the connection to the controller with server port 444
            Socket s = new Socket(ip, 444);

            // obtaining input and out streams
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            Map<String, String> payload = new HashMap<>();
            int chunks = -1;
            File fileToSend = null;

            //Send identification byte
            out.writeInt(ConnectionType.CLIENT.getValue());
            System.out.println("Sent connect ID type: " + ConnectionType.CLIENT + "\n");

            // the following loop performs the exchange of
            // information between client and Controller.Controller Client.Client Handler class
            net_recv:
            while (true) {
                System.out.println(dis.readUTF());
                String tosend = scn.nextLine();

                //Parse the command and then take the appropriate action
                switch (tosend.toLowerCase()) {
                    case "exit":
                        System.out.println("Closing this connection : " + s);
                        s.close();
                        System.out.println("Connection closed");
                        break net_recv;

                    case "send":
                        System.out.println("Please input filename to send: ");
                        tosend = scn.nextLine();

                        fileToSend = new File(tosend);
                        chunks = Client.chunkFile(fileToSend);
                        payload.put("send", Integer.toString(chunks));
                        tosend = MessageParser.mapToString("send", payload);

                        out.writeUTF(tosend);
                        break;

                    case "pull":
                        System.out.println("Please input filename to pull: ");
                        tosend = scn.nextLine();

                        payload.put("pull", tosend);
                        tosend = MessageParser.mapToString("pull", payload);

                        System.out.println("Sending " + tosend);

                        out.writeUTF(tosend);
                        break;

                    default:
                        System.out.println("Unrecognized command " + tosend);
                        System.out.println("Please try again: [Send | Pull | Exit]");
                        break;
                }

                //Print & process received data
                String received = dis.readUTF();
                System.out.println("The message from the controller was:" + received);

                //Create and print the parsed message
                MessageParser parser = new MessageParser(received);
                System.out.println("Parsed KV string: " + parser.getParsedKV());
                System.out.println("Parsed Key: " + parser.getKey());
                System.out.println("Parsed Value: " + parser.getValue());
                System.out.println("");

                switch (parser.getKey()) {
                    case "sendTo":
                        System.out.println("Sending " + chunks + " chunks to the following: " + parser.getValue());
                        String[] sendServers = parser.getValue().split(",");
                        for (int i = 0; i < chunks; i++) {
                            if (chunks == -1)
                                System.out.println("Error in chunking the file - chunks =-1");
                            else {
                                InetAddress ipUpload = InetAddress.getByName(sendServers[i]);
                                Socket sUpload = new Socket(ipUpload, 555);
                                DataInputStream disUpload = new DataInputStream(sUpload.getInputStream());
                                DataOutputStream outUpload = new DataOutputStream(sUpload.getOutputStream());

                                //Init connect and file metadeta
                                outUpload.writeInt(ConnectionType.CLIENT_SEND.getValue());

                                //Send fileChunkName to ChunkServer.ChunkServerRecv
                                String fileChunkName = String.format("%s.%03d", fileToSend.getName(), i + 1);
                                outUpload.writeUTF(fileChunkName);

                                //Send a chunk to the chunk server
                                File chunk = new File(fileToSend.getParent() + "\\" + fileChunkName);
                                byte[] buf = Files.readAllBytes(chunk.toPath());

                                outUpload.write(buf);
                                disUpload.close();
                                outUpload.close();
                            }
                        }
                        break;
                    case "pullFrom":
                        String[] pullServers = parser.getValue().split(",");
                        List<File> files = new ArrayList<>();
                        for (int i = 0; i < pullServers.length; i++) {
                            InetAddress ipPull = InetAddress.getByName(pullServers[i]);
                            Socket sPull = new Socket(ipPull, 555);
                            DataInputStream disPull = new DataInputStream(sPull.getInputStream());
                            DataOutputStream outPull = new DataOutputStream(sPull.getOutputStream());

                            //Send connection type and name of file to search for
                            outPull.writeInt(ConnectionType.CLIENT_PULL.getValue());
                            outPull.writeUTF(payload.get("pull"));

                            //Read in the chunks available on a given server
                            int numChunks = disPull.readInt();
                            System.out.println("This file has " + numChunks + " pieces at " + pullServers[i]);
                            byte[] buf = new byte[64000 * numChunks];
                            int n = 0;
                            boolean remain = true;
                            while (remain) {
                                try {
                                    //Read the file name and size first
                                    String filename = disPull.readUTF();
                                    long fileSize = disPull.readLong();

                                    //Create a new file for the output stream to write to
                                    File f = new File(filename);
                                    f.createNewFile();
                                    FileOutputStream fos = new FileOutputStream(f);
                                    System.out.println("File to read is: " + filename + "," + fileSize);

                                    //Now read the contents of the file, looping at each new file
                                    while (fileSize > 0 && (n = disPull.read(buf, 0, (int) Math.min(buf.length, fileSize))) != -1) {
                                        fos.write(buf, 0, n);
                                        fileSize -= n;
                                    }
                                    files.add(f);
                                    fos.close();
                                } catch (Exception e) {
                                    System.out.println("Read all chunks.");
                                    remain = false;
                                }
                            }
                            //Merge the file chunks into an output file once again.
                            File merged = new File(payload.get("pull"));
                            merged.createNewFile();
                            mergeChunks(files, merged);

                            for (File f : files) {
                                f.delete();
                            }
                        }
                        break;
                    default:
                        System.out.println("Unknown reply from chunk server");
                        break;

                }
            }

            // closing resources
            scn.close();
            dis.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 