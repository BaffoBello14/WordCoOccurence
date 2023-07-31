package unipi.cloudcomputing.mapreduce;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WordCoOccurrenceMapper extends Mapper<Object, Text, Text, MapWritable> {

    private Map<Text, MapWritable> stripes;
    private int windowSize;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        stripes = new HashMap<>();
        windowSize = context.getConfiguration().getInt("windowSize", 2);
    }

    @Override
    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        String[] tokens = value.toString().split(" ");

        for (int i = 0; i < tokens.length; i++) {
            Text word = new Text(tokens[i]);

            // Ottiene o crea la stripe per la parola corrente
            MapWritable stripe = stripes.getOrDefault(word, new MapWritable());

            // Conta le co-occorrenze all'interno della finestra
            int start = Math.max(0, i - windowSize);
            int end = Math.min(tokens.length - 1, i + windowSize);

            for (int j = start; j <= end; j++) {
                if (j != i) {
                    Text neighbor = new Text(tokens[j]);
                    IntWritable count = (IntWritable) stripe.getOrDefault(neighbor, new IntWritable(0));
                    stripe.put(neighbor, new IntWritable(count.get() + 1));
                }
            }

            // Aggiorna la stripe per la parola corrente
            stripes.put(word, stripe);
        }
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        for (Map.Entry<Text, MapWritable> entry : stripes.entrySet()) {
            context.write(entry.getKey(), entry.getValue());
        }
    }
}
