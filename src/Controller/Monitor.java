package Controller;

import Messages.ConnectionType;
import Messages.MessageParser;

import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimerTask;

public class Monitor extends TimerTask {
    private final String addr;
    private final int port;
    public boolean status;

    public Monitor(String addr, int port) {
        this.addr = addr;
        this.port = port;
    }

    public boolean getStatus() {
        return status;
    }

    @Override
    public void run() {
        try (Socket s = new Socket(InetAddress.getByName(addr), port)) {
            status = true;
        } catch (Exception e) {
            System.out.println("Server at " + addr + "_" + port + " is down.");
            status = false;
            this.cancel();

            //Remove the server from the current connections list
            //make sure we aren't trying to get something from the list
            //when we do this
            synchronized (Controller.currentChunkConnections) {
                for (Socket s : Controller.currentChunkConnections) {
                    if (s.getInetAddress().getHostAddress().equals(addr) && Controller.serverPorts.get(s) == port) {
                        Controller.currentChunkConnections.remove(s);
                        System.out.println("Removed " + addr + "_" + port + " from chunk connections." +
                                "Current connected servers: " + Controller.currentChunkConnections.size() + "\n");
                        break;
                    }
                }
            }

            //Update the file:server map and the server:file map to reflect the downed server.
            //If the server has never had files uploaded, the maps will have no content. In
            //this case, simply notify via a print.
            if (Controller.servers.get(addr + "_" + port) != null) {
                String[] files = Controller.servers.get(addr + "_" + port).split(",");
                synchronized (Controller.files) {
                    for (String file : files) {
                        String[] fileToServers = Controller.files.get(file).split(",");
                        String newFileToServers = "";
                        boolean first = true;
                        for (int i = 0; i < fileToServers.length; i++) {
                            if (fileToServers[i].contains(addr + "_" + port)) {
                                fileToServers[i] = "";
                            }
                            if (first && !fileToServers[i].equals("")) {
                                newFileToServers += fileToServers[i];
                                first = false;
                            } else if (!fileToServers[i].equals("")) {
                                newFileToServers += "," + fileToServers[i];
                            }
                        }
                        Controller.files.put(file,newFileToServers);
                        System.out.println("New entry for file " + file + " is: " + newFileToServers);
                    }
                }
                synchronized (Controller.servers) {
                    Controller.servers.remove(addr + "_" + port);
                }

                //Create forwardTo requests which contain servers that don't already
                //have copies of the given file. Then send the forwardTo to a server
                //that does have a copy of the given file.
                for (int i = 0; i < files.length; i++) {
                    try {
                        Map<String, String> payload = new HashMap<>();
                        String destination = ControllerClientHandler.getForwardList(1, Controller.files.get(files[i]));
                        payload.put("forwardTo", destination);
                        System.out.println("Destination is: " + destination + "\n");

                        String[] serversWithFile = Controller.files.get(files[i]).split(",");
                        String addr = serversWithFile[i % serversWithFile.length].split("_")[0];
                        int port = Integer.parseInt(serversWithFile[i % serversWithFile.length].split("_")[1]);
                        System.out.println("Sending forwardTo request to: " + port);

                        Socket s = new Socket(addr, port);
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeInt(ConnectionType.FORWARD_TO.getValue());
                        out.writeUTF(MessageParser.mapToString("forwardTo", payload));
                        out.writeUTF(files[i]);
                        out.close();
                        s.close();


                        System.out.println("Finished sending request for " + files[i]);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            } else
                System.out.println("No files stored at this server.");
        }
    }
}
