public class Process {
    String pid;          // Process ID (e.g., "P1")
    int arrivalTime;     // When the process enters the system
    int burstTime;       // Total CPU time required
    int remainingTime;   // Time left to finish (Crucial for Preemptive SRJF/RR)
    int completionTime;  // When it finishes
    int waitingTime;     // Time spent waiting in the 'Ready Queue'
    int turnAroundTime;  // Total time from arrival to completion

    public Process(String pid, int arrivalTime, int burstTime) {
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime; // Initially, remaining = burst
    }
}
