package first;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

class MovieMapper extends Mapper<Object, Text, Text, Text> {

    private final Text id = new Text();
    private final Text content = new Text();

    @Override
    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        String fileName = ((FileSplit) context.getInputSplit()).getPath().getName();

        if (fileName.contains("movies")) {
            String valS = value.toString();
            int last = valS.lastIndexOf(",");
            String genreS = valS.substring(0, last);
            String[] line = genreS.split(",", 2);
            String idS = line[0];
            String titleS = line[1];
            if (idS.equals("movieId")) {
                return;
            }
            id.set(idS);
            content.set("t:" + titleS);
            context.write(id, content);
        } else {
            String[] line = value.toString().split(",");
            String userId = line[0];
            String movieId = line[1];
            String rateS = line[2];
            if (movieId.equals("movieId")) {
                return;
            }
            id.set(movieId);
            content.set("r:" + userId + "|" + rateS);
            context.write(id, content);
        }
    }
}

class FirstReducer extends Reducer<Text, Text, Text, Text> {

    private final Text id = new Text();
    private final Text ret = new Text();

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        String title = "";

        for (Text t : values) {
            String[] val = t.toString().split(":", 2);
            String letter = val[0];
            String content = val[1];
            if (letter.equals("t") && title.equals("")) {
                title = content;
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Text t : values) {
            String[] val = t.toString().split(":", 2);
            String letter = val[0];
            String content = val[1];
            if (letter.equals("r")) {
                sb.append(content).append(",");
            }
        }

        id.set(title);
        ret.set(sb.toString());
        context.write(id, ret); // movieTitle userId|rating,userId|rating,...
    }
}

class UserMapper extends Mapper<Object, Text, Text, Text> {

    private final Text id = new Text();
    private final Text ret = new Text();

    @Override
    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        String title = key.toString();
        String[] vals = value.toString().split(",");
        for (String v : vals) {
            String[] val = v.split("\\|", 2);
            if (val.length < 2) {
                continue;
            }
            String userId = val[0];
            String rate = val[1];
            id.set(userId);
            ret.set(title + "|" + rate);
            context.write(id, ret);
        }
    }
}

class SecondReducer extends Reducer<Text, Text, Text, Text> {

    private final Text ret = new Text();

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        float max = 0;
        String title = "";
        for (Text t : values) {
            String[] val = t.toString().split("\\|", 2);
            if (val.length < 2) {
                continue;
            }
            String movieTitle = val[0];
            String rate = val[1];
            float rateF = Float.parseFloat(rate);
            if (max < rateF) {
                max = rateF;
                title = movieTitle;
            }
        }
        ret.set(title + " " + max);
        context.write(key, ret);
    }
}

public class Chain extends Configured implements Tool {

    static Configuration cf;

    @Override
    public int run(String args[]) throws IOException, InterruptedException, ClassNotFoundException {
        cf = new Configuration();
        // Configura e lancia Job 1
        Job job1 = Job.getInstance(cf, "Job 1");
        job1.setJarByClass(Chain.class);
        // Configura mapper e reducer per Job 1
        job1.setMapperClass(MovieMapper.class);
        job1.setMapOutputKeyClass(Text.class);
        job1.setMapOutputValueClass(Text.class);
        job1.setReducerClass(FirstReducer.class);
        job1.setOutputKeyClass(Text.class);
        job1.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job1, new Path(args[0]));
        Path intermediateOutput = new Path("intermediate_output");
        FileOutputFormat.setOutputPath(job1, intermediateOutput);
        intermediateOutput.getFileSystem(cf).delete(intermediateOutput, true);

        if (!job1.waitForCompletion(true)) {
            return 1;
        }

        // Configura e lancia Job 2
        Job job2 = Job.getInstance(cf, "Job 2");
        job2.setJarByClass(Chain.class);
        // Configura mapper e reducer per Job 2
        job2.setInputFormatClass(KeyValueTextInputFormat.class);
        job2.setMapperClass(UserMapper.class);
        job2.setMapOutputKeyClass(Text.class);
        job2.setMapOutputValueClass(Text.class);
        job2.setReducerClass(SecondReducer.class);
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job2, intermediateOutput);
        Path p = new Path(args[1]);
        FileOutputFormat.setOutputPath(job2, p);
        p.getFileSystem(cf).delete(p, true);
        return job2.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String args[]) throws Exception {
        int res = ToolRunner.run(cf, new Chain(), args);
        System.exit(res);
    }
}
