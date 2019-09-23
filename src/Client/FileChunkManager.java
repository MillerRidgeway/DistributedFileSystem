package Client;

import erasure.ReedSolomon;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.nio.ByteBuffer;

import static java.nio.file.StandardOpenOption.*;

public class FileChunkManager {
    public static final int DATA_SHARDS = 4;
    public static final int PARITY_SHARDS = 2;
    public static final int TOTAL_SHARDS = 6;
    public static final int BYTES_IN_INT = 4;

    //Adapted from https://www.backblaze.com/open-source-reed-solomon.html
    public static int chunkFileErasure(File f) throws IOException {
        final int fileSize = (int) f.length();
        final int storedSize = fileSize + BYTES_IN_INT;
        final int shardSize = (storedSize + DATA_SHARDS - 1) / DATA_SHARDS;

        // Create a buffer holding the file size, followed by
        // the contents of the file.
        final int bufferSize = shardSize * DATA_SHARDS;
        final byte[] allBytes = new byte[bufferSize];
        ByteBuffer.wrap(allBytes).putInt(fileSize);
        InputStream in = new FileInputStream(f);
        int bytesRead = in.read(allBytes, BYTES_IN_INT, fileSize);
        if (bytesRead != fileSize) {
            throw new IOException("not enough bytes read");
        }

        // Make the buffers to hold the shards.
        byte[][] shards = new byte[TOTAL_SHARDS][shardSize];

        // Fill in the data shards
        for (int i = 0; i < DATA_SHARDS; i++) {
            System.arraycopy(allBytes, i * shardSize, shards[i], 0, shardSize);
        }

        // Use Reed-Solomon to calculate the parity.
        ReedSolomon reedSolomon = new ReedSolomon(DATA_SHARDS, PARITY_SHARDS);
        reedSolomon.encodeParity(shards, 0, shardSize);

        // Write out the resulting files.
        for (int i = 0; i < TOTAL_SHARDS; i++) {
            String chunkFileName = String.format("%s.%03d", f.getName(), i);
            File outputFile = new File(f.getParentFile(), chunkFileName);
            OutputStream out = new FileOutputStream(outputFile);
            out.write(shards[i]);
            out.close();
            System.out.println("Wrote new shard file:" + outputFile);
        }
        in.close();
        return TOTAL_SHARDS;
    }

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

    public static void mergeChunksErasure(String[] fileNames, String destName) throws IOException {
        // Read in any of the shards that are present.
        // (There should be checking here to make sure the input
        // shards are the same size, but there isn't.)
        final byte[][] shards = new byte[TOTAL_SHARDS][];
        final boolean[] shardPresent = new boolean[TOTAL_SHARDS];
        int shardSize = 0;
        int shardCount = 0;
        File destination = new File(destName);
        for (int i = 0; i < TOTAL_SHARDS; i++) {
            String chunkFileName = String.format("%s.%03d", destination.getName(), i);
            File shardFile = new File(destination.getParentFile(), chunkFileName);
            if (shardFile.exists()) {
                shardSize = (int) shardFile.length();
                shards[i] = new byte[shardSize];
                shardPresent[i] = true;
                shardCount += 1;
                InputStream in = new FileInputStream(shardFile);
                in.read(shards[i], 0, shardSize);
                in.close();
                System.out.println("Read " + shardFile);
            }
        }

        // We need at least DATA_SHARDS to be able to reconstruct the file.
        if (shardCount < DATA_SHARDS) {
            System.out.println("Not enough shards present");
            return;
        }

        // Make empty buffers for the missing shards.
        for (int i = 0; i < TOTAL_SHARDS; i++) {
            if (!shardPresent[i]) {
                shards[i] = new byte[shardSize];
            }
        }

        // Use Reed-Solomon to fill in the missing shards
        ReedSolomon reedSolomon = new ReedSolomon(DATA_SHARDS, PARITY_SHARDS);
        reedSolomon.decodeMissing(shards, shardPresent, 0, shardSize);

        // Combine the data shards into one buffer for convenience.
        // (This is not efficient, but it is convenient.)
        byte[] allBytes = new byte[shardSize * DATA_SHARDS];
        for (int i = 0; i < DATA_SHARDS; i++) {
            System.arraycopy(shards[i], 0, allBytes, shardSize * i, shardSize);
        }

        // Extract the file length
        int fileSize = ByteBuffer.wrap(allBytes).getInt();

        // Write the decoded file
        File decodedFile = new File(destination.getParentFile(), destination.getName());
        OutputStream out = new FileOutputStream(decodedFile);
        out.write(allBytes, BYTES_IN_INT, fileSize);
        System.out.println("Wrote new file: " + decodedFile);
    }

    public static void mergeChunks(ArrayList<String> fNames, String dest) throws IOException {
        Path outFile = Paths.get(dest);
        System.out.println("Merging chunks back into file: " + outFile);
        try (FileChannel out = FileChannel.open(outFile, CREATE, WRITE)) {
            for (int i = 0, size = fNames.size(); i < size; i++) {
                Path inFile = Paths.get(fNames.get(i));
                if (Files.size(inFile) == 0)
                    throw new IOException("Chunk " + fNames.get(i) + " corrupted. ");
                System.out.println(inFile + "...");
                try (FileChannel in = FileChannel.open(inFile, READ)) {
                    for (long j = 0, size2 = in.size(); j < size2; )
                        j += in.transferTo(j, size2 - j, out);
                }
            }
        }
        System.out.println("Done merging.");
    }
}
