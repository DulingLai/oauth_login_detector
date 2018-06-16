package utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtils {
    public static void printFile(String fileName, String content){
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName,true))) {
            bw.write(content);
            // no need to close it.
            //bw.close();
            //System.out.println("Done");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
