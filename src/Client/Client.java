package Client;

import ChunkServer.ChunkServer;
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

                //Create and print the parsed message
                String received = dis.readUTF();
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
                        String[] forwardServers = parseForward.getValue().split(",");

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

                                payload.put("forwardTo", forwardServers[i]);
                                outUpload.writeUTF(MessageParser.mapToString("forwardTo", payload));

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
                        String fileListMessage = dis.readUTF();
                        MessageParser parsedFileList = new MessageParser(fileListMessage);
                        System.out.println("Parsed KV string: " + parsedFileList.getParsedKV());
                        System.out.println("Parsed Key: " + parsedFileList.getKey());
                        System.out.println("Parsed Value: " + parsedFileList.getValue());
                        System.out.println("");

                        String[] fileList = parsedFileList.getValue().split(",");
                        String[] serverList = parser.getValue().split(",");
                        ArrayList<String> filesToMerge = new ArrayList<>();

                        for (int i = 0; i < serverList.length; i++) {
                            InetAddress ipPull = InetAddress.getByName(serverList[i].split("_")[0]);
                            int port = Integer.parseInt(serverList[i].split("_")[1]);
                            Socket sPull = new Socket(ipPull, port);
                            DataInputStream disPull = new DataInputStream(sPull.getInputStream());
                            DataOutputStream outPull = new DataOutputStream(sPull.getOutputStream());

                            System.out.println("Getting file " + fileList[i] +
                                    " from " + serverList[i]);

                            //Send connection type and name of file to search for
                            outPull.writeInt(ConnectionType.CLIENT_PULL.getValue());
                            outPull.writeUTF(fileList[i]);

                            //Get file from chunk server
                            int count;
                            byte[] buf = new byte[64000];
                            FileOutputStream fos = new FileOutputStream(fileList[i]);
                            while ((count = disPull.read(buf)) > 0) {
                                fos.write(buf, 0, count);
                            }
                            filesToMerge.add(fileList[i]);
                        }

                        //Merge the file chunks into an output file once again.
                        Collections.sort(filesToMerge);
                        FileChunkManager.mergeChunks(filesToMerge, payload.get("pull"));

                        for (String fName : filesToMerge) {
                            File f = new File(fName);
                            f.delete();
                        }
                        break;
                    default:
                        System.out.println("Unknown reply from controller");
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