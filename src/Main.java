import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        // Open the file
        FileInputStream fstream = new FileInputStream("file.txt");


        Path filePath = Paths.get("file.txt");
        FileChannel fileChannel = FileChannel.open(filePath);

        int fileSize = (int) fileChannel.size();


        final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_PREFERRED;


        InputStream inputStream = new BufferedInputStream(fstream);

        int BUFFER_SIZE = SPECIES.vectorByteSize();

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = -1;

        byte[] semicolonMaskRaw = new byte[]{
                0x003b, 0x003b, 0x003b, 0x003b,
                0x003b, 0x003b, 0x003b, 0x003b,
                0x003b, 0x003b, 0x003b, 0x003b,
                0x003b, 0x003b, 0x003b, 0x003b
        };

        byte[] lineFeedMaskRaw = new byte[]{
                0x000A, 0x000A, 0x000A, 0x000A,
                0x000A, 0x000A, 0x000A, 0x000A,
                0x000A, 0x000A, 0x000A, 0x000A,
                0x000A, 0x000A, 0x000A, 0x000A,
        };

        ByteVector semicolonMask = ByteVector.fromArray(SPECIES, semicolonMaskRaw, 0);
        ByteVector lineFeedMask = ByteVector.fromArray(SPECIES, lineFeedMaskRaw, 0);

        int byteOffset = 0;
        List<Integer> semicolonPositions = new ArrayList<Integer>();
        List<Integer> lineFeedPositions = new ArrayList<Integer>();

        byte[] fileBytes  = new byte[fileSize];

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            ByteVector byteVector = ByteVector.fromArray(SPECIES, buffer, 0);
            VectorMask<Byte> semicolonLine = byteVector.eq(semicolonMask);
            VectorMask<Byte> lineFeedLine = byteVector.eq(lineFeedMask);

            for (var i = 0; i < lineFeedMask.length(); i++) {
                if (semicolonLine.laneIsSet(i)) { semicolonPositions.add(byteOffset + i);}
                if (lineFeedLine.laneIsSet(i)) { lineFeedPositions.add(byteOffset + i);}
                if (byteOffset + i < fileSize) { fileBytes[i+byteOffset] = byteVector.lane(i); }
            }
            byteOffset += bytesRead;
        }

        for (var i = 0; i < semicolonPositions.size(); i++) {
            var city = "";
            var temperature = 0.0f;
            if (i == 0) {
                city = new String(Arrays.copyOfRange(fileBytes, 0, semicolonPositions.get(0)), StandardCharsets.UTF_8);
                temperature = Float.parseFloat(new String(Arrays.copyOfRange(fileBytes, semicolonPositions.get(0) + 1, lineFeedPositions.get(0)), StandardCharsets.UTF_8));
            } else {
                city = new String(Arrays.copyOfRange(fileBytes, lineFeedPositions.get(i-1) + 1, semicolonPositions.get(i)), StandardCharsets.UTF_8);
                temperature = Float.parseFloat(new String(Arrays.copyOfRange(fileBytes, semicolonPositions.get(i) + 1, lineFeedPositions.get(i)), StandardCharsets.UTF_8));
            }

            System.out.println(city + "=" + temperature);
        }


        inputStream.close();
        fstream.close();
    }
}