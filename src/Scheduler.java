import java.util.ArrayList;
import java.util.Scanner;
import java.util.Collections;
import java.util.Comparator;

public class Scheduler {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n;

        System.out.print("Enter the number of processes: ");
        n = scanner.nextInt();

        ArrayList<Process> processes = new ArrayList<>();

        System.out.println("\nEnter process details (Arrival Time and BurstTime):");

        for (int i = 0; i < n; i++) {
            System.out.println("Process P" + (i + 1) + ":");

            System.out.print("  Arrival Time: ");
            int arrival = scanner.nextInt();

            System.out.print("  Burst Time: ");
            int burst = scanner.nextInt();

            String pid = "P" + (i + 1);
            Process newProcess = new Process(pid, arrival, burst);
            processes.add(newProcess);
        }

        System.out.println("\n--- You entered the following processes ---");
        System.out.printf("%-5s %-10s %-10s\n", "PID", "Arrival", "Burst");

        for (Process p : processes) {
            System.out.printf("%-5s %-10d %-10d\n", p.pid, p.arrivalTime, p.burstTime);
        }

        runFCFS(processes);

        scanner.close();
    }

    public static void runFCFS(ArrayList<Process> processes) {
        if(processes.isEmpty()){
            return;
        }
        // --- 1. Sort the processes by Arrival Time ---
        Collections.sort(processes, Comparator.comparingInt(p -> p.arrivalTime));

        int currentTime = 0;
        double totalTurnaroundTime = 0;
        double totalWaitingTime = 0;

        System.out.println("\n--- FCFS Scheduling ---");
        System.out.println("Gantt Chart (simulation):");

        for (Process p : processes) {

            // --- 2. Handle CPU Idle Time ---
            if (currentTime < p.arrivalTime) {
                System.out.println(currentTime + " --[IDLE]-- " + p.arrivalTime);
                currentTime = p.arrivalTime;
            }

            // --- 3. Run the Process (Non-Preemptive) ---
            // The process starts running at 'currentTime'.
            // It will finish at 'currentTime + burstTime' as it's non-preemptive.

            p.completionTime = currentTime + p.burstTime;
            p.turnAroundTime = p.completionTime - p.arrivalTime;
            p.waitingTime = p.turnAroundTime - p.burstTime;

            // Update totals
            totalTurnaroundTime += p.turnAroundTime;
            totalWaitingTime += p.waitingTime;

            // visual Gantt chart
            System.out.println(currentTime + " --[" + p.pid + "]-- " + p.completionTime);

            currentTime = p.completionTime;
        }

        // --- 4. Display Results ---
        System.out.println("\n--- FCFS Results ---");
        System.out.printf("%-5s %-10s %-10s %-12s %-12s %-10s\n",
                "PID", "Arrival", "Burst", "Completion", "Turnaround", "Waiting");

        for (Process p : processes) {
            System.out.printf("%-5s %-10d %-10d %-12d %-12d %-10d\n",
                    p.pid, p.arrivalTime, p.burstTime,
                    p.completionTime, p.turnAroundTime, p.waitingTime);
        }

        // --- 5. Calculate and Display Averages ---
        int n = processes.size();
        double avgTurnaroundTime = totalTurnaroundTime / n;
        double avgWaitingTime = totalWaitingTime / n;

        System.out.printf("\nAverage Turnaround Time: %.2f\n", avgTurnaroundTime);
        System.out.printf("Average Waiting Time:    %.2f\n", avgWaitingTime);
    }
}
