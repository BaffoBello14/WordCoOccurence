import os
import re
import pandas as pd
import numpy as np
from collections import defaultdict
from scipy.sparse import coo_matrix
import time

def tokenize(text):
    # Funzione di tokenizzazione per suddividere il testo in parole
    return re.findall(r'\b\w+\b', text.lower())

def co_occurrence_matrix(input_dir, window_size):
    # Inizializza il dizionario per le frequenze delle co-occorrenze
    co_occurrence_freq = defaultdict(lambda: defaultdict(int))

    # Processa i file di input nella directory "snippets"
    for filename in os.listdir(input_dir):
        if filename.startswith("line"):
            with open(os.path.join(input_dir, filename), 'r') as file:
                text = file.read()
                words = tokenize(text)
                
                # Costruisci la matrice di co-occorrenza
                for i, word in enumerate(words):
                    for j in range(max(0, i - window_size), min(len(words), i + window_size + 1)):
                        if i != j:
                            co_occurrence_freq[word][words[j]] += 1

    # Trasforma il dizionario di frequenze in un DataFrame di Pandas
    co_occurrence_df = pd.DataFrame(co_occurrence_freq).fillna(0)

    return co_occurrence_df

def main():
    # Imposta i parametri
    input_dir = "./snippets"
    window_size = 1

    # Misura il tempo di inizio
    start_time = time.time()

    # Costruisci la matrice di co-occorrenza
    co_occurrence_df = co_occurrence_matrix(input_dir, window_size)

    # Converti il DataFrame in una matrice sparsa usando coo_matrix di SciPy
    co_occurrence_sparse = coo_matrix(co_occurrence_df.values)

    # Misura il tempo di fine
    end_time = time.time()

    # Stampa la matrice sparsa
    #print(co_occurrence_sparse)

    # Calcola il tempo di esecuzione in millisecondi
    execution_time = (end_time - start_time) * 1000
    print("Tempo di esecuzione:", execution_time, "millisecondi")

    # Salva la matrice sparsa in un file NPZ
    '''np.savez("co_occurrence_matrix.npz", data=co_occurrence_sparse.data, row=co_occurrence_sparse.row,
             col=co_occurrence_sparse.col, shape=co_occurrence_sparse.shape)'''

if __name__ == "__main__":
    main()
