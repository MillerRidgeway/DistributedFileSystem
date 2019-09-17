package Controller;

import java.net.InetAddress;
import java.net.Socket;
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
            System.out.println("Server at " + addr + " is down.");
            status = false;
        }
    }
}
