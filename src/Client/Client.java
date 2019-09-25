package Client;

import Messages.ConnectionType;
import Messages.MessageParser;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;

// Client.Client class
public class Client {

    public static void main(String[] args) {
        try {

            final InetAddress controllerAddr = InetAddress.getByName(args[0]);
            final int controllerPort = Integer.parseInt(args[1]);
            final String replicationScheme = args[2];

            System.out.println("Sent connect to " + args[0] + " on port " + controllerPort
                    + " using the " + replicationScheme + " encoding scheme.");

            Scanner scn = new Scanner(System.in);

            // establish the connection to the controller with server port 444
            Socket s = new Socket(controllerAddr, controllerPort);

            // obtaining input and out streams
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            Map<String, String> payload = new HashMap<>();
            int chunks = -1;
            int originalChunks = -1;
            File fileToSend = null;

            //Send identification byte
            out.writeInt(ConnectionType.CLIENT.getValue());
            System.out.println("Sent connect ID type: " + ConnectionType.CLIENT + "\n");

            // the following loop performs the exchange of
            // information between client and Controller.Controller Client.Client Handler class
            net_recv:
            while (true) {
                System.out.println("What do you want? [Send | Pull | Exit]");

                boolean isNotValid = true;
                while (isNotValid) {
                    //Parse the command and then take the appropriate action
                    String tosend = scn.nextLine();
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
                            int shards = 0;
                            if (replicationScheme.equalsIgnoreCase("erasure")) {
                                chunks = FileChunkManager.chunkFile(fileToSend);
                                originalChunks = chunks;
                                for (int i = 0; i < originalChunks; i++) {
                                    String fileChunkName = String.format("%s.%03d", fileToSend.getName(), i + 1);
                                    File chunkFileToEncode = new File("src\\" + fileChunkName);
                                    shards += FileChunkManager.chunkFileErasure(chunkFileToEncode);
                                    chunkFileToEncode.delete();
                                }
                                chunks = shards;
                            } else {
                                chunks = FileChunkManager.chunkFile(fileToSend);
                            }

                            payload.put("send", Integer.toString(chunks));
                            tosend = MessageParser.mapToString("send", payload);

                            out.writeUTF(tosend);
                            isNotValid = false;
                            break;

                        case "pull":
                            System.out.println("Please input filename to pull: ");
                            tosend = scn.nextLine();

                            payload.put("pull", tosend);
                            tosend = MessageParser.mapToString("pull", payload);

                            System.out.println("Sending " + tosend);

                            out.writeUTF(tosend);
                            isNotValid = false;
                            break;

                        default:
                            System.out.println("Unrecognized command " + tosend);
                            System.out.println("Please try again: [Send | Pull | Exit]");
                            break;
                    }
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

                        //Build an ArrayList of chunks/shards that match the filename we are
                        //trying to send
                        File dir = new File("src\\ ");
                        final File finalFileToSend = fileToSend;
                        File[] foundFiles = dir.listFiles();
                        ArrayList<File> matchingFiles = new ArrayList<>();
                        for (File f : foundFiles) {
                            if (f.getName().startsWith(fileToSend.getName() + "."))
                                matchingFiles.add(f);
                        }

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
                                String fileChunkName = "";
                                fileChunkName = matchingFiles.get(i).getName();
                                if (replicationScheme.equalsIgnoreCase("erasure")) {
                                    payload.put("forwardTo", "null");
                                } else {
                                    payload.put("forwardTo", forwardServers[i]);
                                }

                                outUpload.writeUTF(fileChunkName);
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
                        ArrayList<String> finalMergeList = new ArrayList<>();

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


                        if (replicationScheme.equalsIgnoreCase("erasure")) {
                            for (int j = 0; j < fileList.length / FileChunkManager.TOTAL_SHARDS; j++) {
                                List<String> chunkFileShards = filesToMerge.subList(j * FileChunkManager.TOTAL_SHARDS, (j * FileChunkManager.TOTAL_SHARDS) + 9);
                                String[] chunkFileShardsArr = chunkFileShards.toArray(new String[0]);
                                String chunkFileName = chunkFileShards.get(0).substring(0,
                                        chunkFileShards.get(0).length() - 7);
                                System.out.println("Merging shards into: " + chunkFileName);
                                FileChunkManager.mergeChunksErasure(chunkFileShardsArr, chunkFileName);
                                finalMergeList.add(chunkFileName);
                            }
                        }
                        //May find a corrupted chunk, if we do report the corrupted
                        //chunk to the controller and ask the user to re-download
                        else {
                            boolean noCorruptedChunks = true;
                            for (int i = 0; i < filesToMerge.size(); i++) {
                                File f = new File(filesToMerge.get(i));
                                if (f.length() == 0) {
                                    noCorruptedChunks = false;
                                    System.out.println("File chunk " + f + " is corrupted.");
                                    payload.put("corruptChunkFound", filesToMerge.get(i) + "," + serverList[i]);
                                    out.writeUTF(MessageParser.mapToString("corruptChunkFound", payload));
                                }
                                finalMergeList.add(filesToMerge.get(i));
                            }
                            if (!noCorruptedChunks) {
                                System.out.println("File you downloaded has corrupted chunk(s). " +
                                        "Please attempt to re-download the file");
                                break;
                            }
                        }

                        FileChunkManager.mergeChunks(finalMergeList, payload.get("pull"));

                        for (String fName : filesToMerge) {
                            File f = new File("C:\\Users\\Miller Ridgeway\\IdeaProjects\\DistributedFilesystem" + fName);
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