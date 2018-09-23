package dulinglai.android.alode;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class ResultWriter {

    private String outputPath;
    private String packageName;

    public ResultWriter(String packageName, String outputPath){
        this.packageName = packageName;
        this.outputPath = outputPath;
    }

    public void appendStringToResultFile(String fileName, String toAppend) throws IOException {
        FileWriter fw = new FileWriter(outputPath+"/"+fileName, true);
        BufferedWriter resultWriter = new BufferedWriter(fw);

        resultWriter.write(packageName + " : " + toAppend);
        resultWriter.newLine();
        resultWriter.close();
    }
}
