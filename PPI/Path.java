
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Path {
    public static String sourcePath;
    public static String settingPath;
    public static String inputPath;
    public static String outputPath;
    public static RandomAccessFile raf;

    static{
        String sourceString = System.getProperty("user.dir");
        String separator = System.getProperties().getProperty("file.separator");
        if(separator.equals("\\")){
            String input = sourceString + separator + "filepath.txt";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < input.length(); i++) {
                if(input.charAt(i) == '\\')
                    sb.append("\\\\");
                else
                    sb.append(input.charAt(i));
            }
            settingPath = sb.toString();
        }else{
            settingPath = sourceString + separator + "filepath.txt";
        }
        try {
            raf = new RandomAccessFile(settingPath, "r");
            raf.seek(0);
            String[] pathArray = new String[10];
            for(int i = 0; (pathArray[i] = raf.readLine()) != null; i++);
            inputPath = pathArray[0];
            outputPath = pathArray[1];
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void printLine(String string) {
        try {
            raf = new RandomAccessFile(outputPath, "rw");
            raf.seek(raf.length());
            String content = string + "\n";
            raf.writeBytes(new String(content.getBytes(), "utf-8"));
            raf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}