package Client;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static java.nio.file.StandardOpenOption.*;

public class FileChunkManager {
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

    public static void mergeChunks(ArrayList<String> fNames, String dest) throws IOException {
        Path outFile = Paths.get(dest);
        System.out.println("Merging chunks back into file:  " + outFile);
        try (FileChannel out = FileChannel.open(outFile, CREATE, WRITE)) {
            for (int i = 0, n = fNames.size(); i < n; i++) {
                Path inFile = Paths.get(fNames.get(i));
                System.out.println(inFile + "...");
                try (FileChannel in = FileChannel.open(inFile, READ)) {
                    for (long p = 0, l = in.size(); p < l; )
                        p += in.transferTo(p, l - p, out);
                }
            }
        }
        System.out.println("Done merging.");
    }
}
