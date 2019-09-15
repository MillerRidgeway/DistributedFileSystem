import java.io.File;
import java.util.ArrayList;
import java.util.TimerTask;

public class Heartbeat extends TimerTask {
    ArrayList <String> files, newFiles;
    String addr;

    public Heartbeat(ArrayList<String> files, ArrayList<String> newFiles, String addr){
        this.files = files;
        this.newFiles = newFiles;
        this.addr = addr;
    }

    @Override
    public void run() {

    }
}
