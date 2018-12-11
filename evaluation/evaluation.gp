set terminal pdf
set output 'result.pdf'
set xlabel 'Instância'
set ylabel 'Tempo (ms)'
set title 'Tempo de Execução do Algoritmo K-Core sob o Webgraph'
plot 'result.dat' using ($1):($2) title "Tempo de Execução" with linespoints