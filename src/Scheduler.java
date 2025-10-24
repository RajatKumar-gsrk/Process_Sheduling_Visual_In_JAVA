import java.util.ArrayList;
import java.util.Scanner;

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

        scanner.close();
    }
}
