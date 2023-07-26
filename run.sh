#!/bin/bash

# Comando per compilare e creare il file JAR
MVN_COMMAND="mvn package"
OUTPUT_DIR_BASE="/user/hadoop/project2/output"
INPUT_DIR="/user/hadoop/project2/snippets/line*"
MAIN_CLASS="unipi.cloudcomputing.WordCoOccurrenceMapReduce"
WINDOW_SIZES=(0 1 2 3 5)
NUM_REDUCERS=(1 5 10 15 20 25 30)
NUM_RUNS=10

# Funzione per eseguire una singola iterazione
function run_iteration {
  local w=$1
  local r=$2
  local run=$3
  local output_dir="${OUTPUT_DIR_BASE}/test_reducer${r}_window${w}/run${run}"

  # Comando per eseguire il programma
  COMMAND="(cd target/ && hadoop jar ${JAR_FILE} ${MAIN_CLASS} -i ${INPUT_DIR} -w ${w} -r ${r} -o ${output_dir})"
  
  # Esegui il comando
  echo "Esecuzione del comando: ${COMMAND}"
  eval "${COMMAND}"
}

# Esegui il comando mvn package per creare il file JAR
echo "Esecuzione del comando: ${MVN_COMMAND}"
eval "${MVN_COMMAND}"

# Verifica se la compilazione e la creazione del file JAR sono state completate con successo
if [ $? -ne 0 ]; then
  echo "Errore durante la compilazione e la creazione del file JAR. Lo script verrà interrotto."
  exit 1
fi

# Percorso del file JAR
JAR_FILE="WordCoOccurrence-1.0-SNAPSHOT.jar"

# Loop per eseguire il programma con diversi valori
for ((run=1; run<=$NUM_RUNS; run++)); do
  for w in "${WINDOW_SIZES[@]}"; do
    for r in "${NUM_REDUCERS[@]}"; do
      run_iteration $w $r $run
    done
  done
done

# Creazione dei file di media delle medie dei tempi di esecuzione per il numero di reducer
echo "Medie dei tempi di esecuzione per il numero di reducer:" > "execution_time_r"
for r in "${NUM_REDUCERS[@]}"; do
  execution_time_file="execution_time_r_${r}"
  total_time=0
  iteration_count=0
  for w in "${WINDOW_SIZES[@]}"; do
    for ((run=1; run<=$NUM_RUNS; run++)); do
      file_path="${OUTPUT_DIR_BASE}/test_reducer${r}_window${w}/run${run}/execution_time"
      execution_time=$(grep -oP 'Tempo di esecuzione: \K\d+' "$file_path")
      total_time=$((total_time + execution_time))
      iteration_count=$((iteration_count + 1))
    done
  done
  average_time=$((total_time / iteration_count))
  echo "Reducer=${r}, Average Time=${average_time}" >> "execution_time_r"
done

# Creazione dei file di media delle medie dei tempi di esecuzione per la finestra
echo "Medie dei tempi di esecuzione per la finestra:" > "execution_time_w"
for w in "${WINDOW_SIZES[@]}"; do
  execution_time_file="execution_time_w_${w}"
  total_time=0
  iteration_count=0
  for r in "${NUM_REDUCERS[@]}"; do
    for ((run=1; run<=$NUM_RUNS; run++)); do
      file_path="${OUTPUT_DIR_BASE}/test_reducer${r}_window${w}/run${run}/execution_time"
      execution_time=$(grep -oP 'Tempo di esecuzione: \K\d+' "$file_path")
      total_time=$((total_time + execution_time))
      iteration_count=$((iteration_count + 1))
    done
  done
  average_time=$((total_time / iteration_count))
  echo "Finestra=${w}, Average Time=${average_time}" >> "execution_time_w"
done

# Creazione dei file di media delle medie dei tempi di esecuzione per il numero di reducer
echo "Medie delle medie dei tempi di esecuzione per il numero di reducer:" >> "execution_time_r"
for r in "${NUM_REDUCERS[@]}"; do
  total_time=0
  iteration_count=0
  for w in "${WINDOW_SIZES[@]}"; do
    total_time=$((total_time + (reducer_avg_times[$r] * NUM_RUNS)))
    iteration_count=$((iteration_count + NUM_RUNS))
  done
  average_time=$((total_time / iteration_count))
  echo "Reducer=${r}, Average Time=${average_time}" >> "execution_time_r"
done

# Creazione dei file di media delle medie dei tempi di esecuzione per la finestra
echo "Medie delle medie dei tempi di esecuzione per la finestra:" >> "execution_time_w"
for w in "${WINDOW_SIZES[@]}"; do
  total_time=0
  iteration_count=0
  for r in "${NUM_REDUCERS[@]}"; do
    total_time=$((total_time + (window_avg_times[$w] * NUM_RUNS)))
    iteration_count=$((iteration_count + NUM_RUNS))
  done
  average_time=$((total_time / iteration_count))
  echo "Finestra=${w}, Average Time=${average_time}" >> "execution_time_w"
done

echo "Operazione completata."
