package hdfs;

import java.io.File;
import java.util.Objects;

//import java.nio.file.*;
//import java.util.stream.*;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.util.*;
//import org.apache.hadoop.hdfs.*;

public class MyApp {
  public static class Runner extends Configured implements Tool {
    public int run(String[] args) throws Exception {
      System.out.println("Starting application...");

      String hdfsUrl = "hdfs://localhost:9000";

      Configuration conf = new Configuration();
      conf.set("fs.defaultFS", hdfsUrl);
      FileSystem fs = FileSystem.get(conf);

      for (File file : Objects.requireNonNull(new File("./test/").listFiles())) {
        String fileStr = file.getPath();
        System.out.println("Transfering " + fileStr);
        fs.copyFromLocalFile(new Path(fileStr), new Path("/test/"));
      }

      System.out.println("Done!");
      return 0;
    }
  }

  public static void main(String[] args) throws Exception {
    int returnCode = ToolRunner.run(new MyApp.Runner(), args);
    System.exit(returnCode);
  }
}
// =====================================================================
