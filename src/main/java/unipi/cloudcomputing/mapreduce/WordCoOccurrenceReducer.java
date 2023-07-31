package unipi.cloudcomputing.mapreduce;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

public class WordCoOccurrenceReducer extends Reducer<Text, MapWritable, Text, IntWritable> {

    @Override
    public void reduce(Text key, Iterable<MapWritable> values, Context context) throws IOException, InterruptedException {

        MapWritable result = new MapWritable();

        // Combina le stripe di co-occorrenza per ogni parola
        for (MapWritable stripe : values) {
            for (Map.Entry<Writable, Writable> entry : stripe.entrySet()) {
                Text neighbor = (Text) entry.getKey();
                IntWritable count = (IntWritable) entry.getValue();
                IntWritable currentCount = (IntWritable) result.getOrDefault(neighbor, new IntWritable(0));
                currentCount.set(currentCount.get() + count.get());
                result.put(neighbor, currentCount);
            }
        }

        for (Entry<Writable, Writable> entry : result.entrySet()) {
            String k = key.toString() + " - " + ((Text)entry.getKey()).toString() + " :";        
            context.write(new Text(k), (IntWritable)entry.getValue());
        }
    }
}
