package unipi.cloudcomputing;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import unipi.cloudcomputing.mapreduce.WordCoOccurrenceMapper;
import unipi.cloudcomputing.mapreduce.WordCoOccurrenceReducer;

import java.io.*;
import java.util.*;

public class WordCoOccurrenceMapReduce {
    public static void main(String[] args) throws Exception {
        // Configurazione di Hadoop
        Configuration conf = new Configuration();

        // Creazione di un nuovo job con la configurazione e il nome
        Job job = Job.getInstance(conf, "word co-occurrence");

        // Specifica della classe main, mapper e reducer del job
        job.setJarByClass(WordCoOccurrenceMapReduce.class);
        job.setMapperClass(WordCoOccurrenceMapper.class);
        job.setReducerClass(WordCoOccurrenceReducer.class);

        // Specifica del tipo di output del mapper
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(MapWritable.class);

        // Specifica del tipo di output del reducer
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        // Lettura dei parametri da linea di comando
        String inputDir = null;
        String outputDir = null;
        int windowSize = 0;
        boolean filterCombinations = true; // Filtro delle combinazioni
        int numReducers = 1; // Numero di reducer di default

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-i")) {
                inputDir = args[i + 1];
            } else if (args[i].equals("-o")) {
                outputDir = args[i + 1];
            } else if (args[i].equals("-w")) {
                windowSize = Integer.parseInt(args[i + 1]);
            } else if (args[i].equals("-f")) {
                filterCombinations = Boolean.parseBoolean(args[i + 1]);
            } else if (args[i].equals("-r")) {
                numReducers = Integer.parseInt(args[i + 1]);
            }
        }

        // Verifica dei parametri obbligatori
        if (inputDir == null || outputDir == null) {
            System.out.println("Errore: Assicurati di specificare i parametri corretti.");
            System.out.println("-i <inputDir> -o <outputDir> [-w <windowSize>] [-f <filterCombinations>] [-r <numReducers>]");
            System.exit(1);
        }

        // Impostazione del numero di reducer
        job.setNumReduceTasks(numReducers);

        // Impostazione della finestra nella configurazione
        if (windowSize > 0) {
            job.getConfiguration().setInt("windowSize", windowSize);
        }

        // Specifica dei percorsi di input e output per il job
        FileInputFormat.addInputPaths(job, inputDir); // Percorso della directory di input
        FileOutputFormat.setOutputPath(job, new Path(outputDir)); // Percorso della directory di output

        long startTime = System.currentTimeMillis();
        // Esecuzione del job e uscita dal programma con stato di successo o errore
        boolean success = job.waitForCompletion(true);
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Se l'esecuzione del job è completata con successo, leggi i file di output e scrivilo nel file "soluzione"
        if (success) {
            // Ottieni il percorso completo del file "soluzione" nella directory di output
            String outputFile = outputDir + "/soluzione";

            // Apri il file di output
            FileSystem fs = FileSystem.get(conf);
            FSDataOutputStream outputStream = fs.create(new Path(outputFile));

            // Lettura dei file di output nella cartella di output
            FileStatus[] fileStatuses = fs.globStatus(new Path(outputDir + "/part-r-*"));
            for (FileStatus status : fileStatuses) {
                Path filePath = status.getPath();

                // Leggi il contenuto del file
                FSDataInputStream inputStream = fs.open(filePath);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                // Filtra le combinazioni se richiesto
                if (filterCombinations) {
                    Map<String, Integer> combinations = new HashMap<>();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(" : ");
                        String[] words = parts[0].trim().split(" - ");
                        Arrays.sort(words); // Ordina le parole
                        String combination = words[0] + " - " + words[1];
                        int count = Integer.parseInt(parts[1].trim());
                        combinations.put(combination, count);
                    }

                    // Scrivi le combinazioni filtrate nel file di output "soluzione"
                    for (Map.Entry<String, Integer> entry : combinations.entrySet()) {
                        outputStream.writeBytes(entry.getKey() + " : " + entry.getValue() + "\n");
                    }
                } else {
                    // Scrivi il contenuto del file nel file di output "soluzione" senza filtrare
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputStream.writeBytes(line + "\n");
                    }
                }
                
                // Chiudi lo stream di input del file corrente
                inputStream.close();
            }

            // Chiudi lo stream di output del file "soluzione"
            outputStream.close();

            // Salva il tempo di esecuzione in un file
            String executionTimeFile = outputDir + "/execution_time";
            FSDataOutputStream executionTimeStream = fs.create(new Path(executionTimeFile));
            executionTimeStream.writeBytes("Tempo di esecuzione: " + executionTime + " millisecondi\n");
            executionTimeStream.close();
        }

        // Uscita dal programma con stato di successo o errore
        System.exit(success ? 0 : 1);
    }
}