package Controller;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
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


            if (Controller.servers.get(addr + "_" + port) != null) {
                String[] files = Controller.servers.get(addr + "_" + port).split(",");
                synchronized (Controller.files) {
                    //Update the file:server map and the server:file map to reflect the downed server
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
                        System.out.println("New entry for file " + file + " is: " + newFileToServers);
                    }
                }
                synchronized (Controller.servers) {
                    Controller.servers.remove(addr + "_" + port);
                }

                //Create forwardTo requests which are servers that don't already
                //have copies of the given file
                for (int i = 0; i < files.length; i++) {
                    try {
                        String destination = ControllerClientHandler.getForwardList(1, Controller.files.get(files[i]));
                        System.out.println("Destination is: " + destination);
                    } catch (UnknownHostException ex) {
                        ex.printStackTrace();
                    }
                }
            } else
                System.out.println("No files stored at this server.");
        }
    }
}
