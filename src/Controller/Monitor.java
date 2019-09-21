package Controller;

import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
                                "Current connected servers: " + Controller.currentChunkConnections.size());
                    }
                }
            }

            //Create an index of files and servers to get them at
            //these files must also have been present on the failed server
            ArrayList<String> forwardFromList = new ArrayList<>();
            ArrayList<String> files = new ArrayList<>();

            for (Map.Entry<String, String> entry : Controller.files.entrySet()) {
                if (entry.getValue().contains(addr + "_" + port)) {
                    entry.getValue().replace(addr + "_" + port, "");
                    System.out.println("New entry in file list is: " + entry.getValue());

                    forwardFromList.add(entry.getValue());
                    files.add(entry.getKey());
                }
            }

            //Create forwardTo requests which are servers that don't already
            //have copies of the given file
//            for (int i = 0; i < files.size(); i++) {
//                try {
//                    String destination = ControllerClientHandler.getForwardList(1, forwardFromList.get(i));
//                } catch (UnknownHostException ex) {
//                    ex.printStackTrace();
//                }
//            }
        }
    }
}
