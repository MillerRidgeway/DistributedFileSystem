package Client;

import Messages.ConnectionType;
import Messages.MessageParser;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;

// Client.Client class
public class Client {

    public static void main(String[] args) throws IOException {
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
                        chunks = FileChunkManager.chunkFile(fileToSend);
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
                        //Include the forwardTo message from the controller
                        //so the forwardTo doesn't have to be requested later
                        String forwardMessage = dis.readUTF();
                        MessageParser parseForward = new MessageParser(forwardMessage);
                        System.out.println("Parsed KV string: " + parseForward.getParsedKV());
                        System.out.println("Parsed Key: " + parseForward.getKey());
                        System.out.println("Parsed Value: " + parseForward.getValue());
                        System.out.println("");

                        //Send the corresponding chunks to the servers returned from
                        //the controller
                        System.out.println("Sending " + chunks + " chunks to the following: " + parser.getValue());
                        String[] sendServers = parser.getValue().split(",");
                        for (int i = 0; i < chunks; i++) {
                            int port = Integer.parseInt(sendServers[i].split("_")[1]);
                            String addr = sendServers[i].split("_")[0];
                            if (chunks == -1)
                                System.out.println("Error in chunking the file - chunks =-1");
                            else {
                                InetAddress ipUpload = InetAddress.getByName(addr);
                                Socket sUpload = new Socket(ipUpload, port);
                                DataInputStream disUpload = new DataInputStream(sUpload.getInputStream());
                                DataOutputStream outUpload = new DataOutputStream(sUpload.getOutputStream());

                                //Init connect and filename
                                outUpload.writeInt(ConnectionType.CLIENT_SEND.getValue());

                                //Send fileChunkName to ChunkServer.ChunkServerRecv
                                //as well as the forwarding locations
                                String fileChunkName = String.format("%s.%03d", fileToSend.getName(), i + 1);
                                outUpload.writeUTF(fileChunkName);
                                outUpload.writeUTF(forwardMessage);

                                //Send a chunk to the chunk server
                                File chunk = new File(fileToSend.getParent() + "\\" + fileChunkName);
                                byte[] buf = Files.readAllBytes(chunk.toPath());

                                outUpload.write(buf);
                                chunk.delete();
                                
                                disUpload.close();
                                outUpload.close();
                            }
                        }
                        break;
                    case "pullFrom":
                        String[] pullServers = parser.getValue().split(",");
                        ArrayList<String> files = new ArrayList<>();
                        for (int i = 0; i < pullServers.length; i++) {
                            InetAddress ipPull = InetAddress.getByName(pullServers[i].split("_")[0]);
                            int port = Integer.parseInt(pullServers[i].split("_")[1]);
                            Socket sPull = new Socket(ipPull, port);
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
                                    files.add(f.getName());
                                    fos.close();
                                } catch (Exception e) {
                                    System.out.println("Read all chunks.");
                                    remain = false;
                                }
                            }
                        }
                        //Merge the file chunks into an output file once again.
                        Collections.sort(files);
                        FileChunkManager.mergeChunks(files, payload.get("pull"));
                        for (String fName : files) {
                            File f = new File(fName);
                            f.delete();
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