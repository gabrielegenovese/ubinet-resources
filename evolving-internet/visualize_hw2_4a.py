import numpy as np
import matplotlib.pyplot as plt

# Parametri
a = 7.5 *1500*5      # coefficiente angolare
b = 0.5 *1500*5    # intercetta
p = 5        # valore limite per x

# Genera i valori di x limitati
x = np.linspace(0, p, 100)   # sostituisci -10 con l'inizio del tuo range se necessario
y = a * x + b

# Grafico
plt.plot(x, y, label=f"y = {a}x + {b}")
plt.xlabel("x")
plt.ylabel("y")
plt.title("Graph of y = ax + b with x < 5")
plt.axvline(x=p, color="red", linestyle="--", label=f"x = {p}")
plt.legend()
plt.show()

