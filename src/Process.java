public class Process {
    String pid;
    int arrivalTime;
    int burstTime;

    int remainingTime;
    int completionTime;
    int turnaroundTime;
    int waitingTime;

    public Process(String pid, int arrivalTime, int burstTime) {
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = this.burstTime;
        this.completionTime = 0;
        this.turnaroundTime = 0;
        this.waitingTime = 0;
    }
}
