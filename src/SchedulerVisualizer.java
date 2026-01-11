import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Arrays;
import javax.swing.table.DefaultTableModel;

public class SchedulerVisualizer extends JFrame {

    private List<Process> processList = new ArrayList<>();
    private DefaultTableModel tableModel; // Keep this to update the table

    public SchedulerVisualizer() {
        setTitle("OS Process Scheduler Visualizer - FCFS");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout()); // Using BorderLayout for structure

        // 1. Data Setup
        List<Process> processes = Arrays.asList(
                new Process("P1", 0, 3),
                new Process("P2", 2, 4),
                new Process("P3", 8, 2)
        );
        calculateFCFS(processes);

        // 2. Gantt Chart Panel (Top)
        JPanel ganttPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGanttChart(g, processes); // Pass the list here
            }
        };
        ganttPanel.setPreferredSize(new Dimension(900, 150));
        add(ganttPanel, BorderLayout.NORTH);

        // 3. Table (Center)
        String[] columns = {"Process ID", "Arrival Time", "Burst Time", "Completion Time", "Turnaround Time", "Waiting Time"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        for (Process p : processes) {
            model.addRow(new Object[]{p.pid, p.arrivalTime, p.burstTime, p.completionTime, p.turnAroundTime, p.waitingTime});
        }
        JTable table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // 4. Stats Label (Bottom)
        JLabel statsLabel = new JLabel(calculateStats(processes), SwingConstants.CENTER);
        statsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statsLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(statsLabel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private void drawGanttChart(Graphics g,  List<Process> processes) {
        int x = 50; // Starting X-coordinate
        int y = 50; // Starting Y-coordinate
        int height = 50;
        int scale = 40; // 1 second = 40 pixels

        // Dummy Data for testing (Usually this comes from your input)
        calculateFCFS(processes);

        int currentTime = 0;

        for (Process p : processes) {
            // Handle Idle Time visually
            if (p.arrivalTime > currentTime) {
                int idleWidth = (p.arrivalTime - currentTime) * scale;
                g.setColor(Color.LIGHT_GRAY);
                g.drawRect(x, y, idleWidth, height);
                g.drawString("Idle", x + 5, y + 30);
                x += idleWidth;
                currentTime = p.arrivalTime;
            }

            // Draw Process block
            int width = p.burstTime * scale;
            g.setColor(Color.CYAN);
            g.fillRect(x, y, width, height);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, width, height);

            // Labels
            g.drawString(p.pid, x + (width / 4), y + 30);
            g.drawString(String.valueOf(currentTime), x, y + height + 20); // Start time

            x += width;
            currentTime = p.completionTime;
            g.drawString(String.valueOf(currentTime), x, y + height + 20); // End time
        }
    }

    public void calculateFCFS(List<Process> processes) {
        // 1. Sort processes by arrival time
        processes.sort(Comparator.comparingInt(p -> p.arrivalTime));

        int currentTime = 0;
        for (Process p : processes) {
            // If the CPU is idle because the process hasn't arrived yet
            if (currentTime < p.arrivalTime) {
                currentTime = p.arrivalTime;
            }

            // Start time for this process is the current time
            // completionTime = start time + burst time
            p.completionTime = currentTime + p.burstTime;

            // Turnaround Time = Completion - Arrival
            p.turnAroundTime = p.completionTime - p.arrivalTime;

            // Waiting Time = Turnaround - Burst
            p.waitingTime = p.turnAroundTime - p.burstTime;

            // Move the clock forward
            currentTime = p.completionTime;
        }
    }

    private String calculateStats(List<Process> processes) {
        double totalWT = 0;
        double totalTAT = 0;

        for (Process p : processes) {
            totalWT += p.waitingTime;
            totalTAT += p.turnAroundTime;
        }

        double avgWT = totalWT / processes.size();
        double avgTAT = totalTAT / processes.size();

        // Returning a formatted string for the label
        return String.format("Average Waiting Time: %.2f ms | Average Turnaround Time: %.2f ms", avgWT, avgTAT);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SchedulerVisualizer().setVisible(true));
    }
}
