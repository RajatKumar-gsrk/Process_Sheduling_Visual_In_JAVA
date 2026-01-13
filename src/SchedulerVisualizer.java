import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Arrays;
import javax.swing.table.DefaultTableModel;

public class SchedulerVisualizer extends JFrame {

    private List<Process> processList;
    private DefaultTableModel tableModel; // Keep this to update the table
    private JLabel statsLabel;

    public SchedulerVisualizer() {
        setTitle("OS Process Scheduler Visualizer");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout()); // Using BorderLayout for structure

        // 1. Data Setup
        processList = new ArrayList<>(Arrays.asList(
                new Process("P1", 0, 8),
                new Process("P2", 1, 2),
                new Process("P3", 2, 1)
        ));//Arrays.aslist returns fixed size list can't add or remove so we wrap it
        calculateSJF(processList);

        // 2. Gantt Chart Panel (Top)
        JPanel ganttPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGanttChart(g, processList); // Pass the list here
            }
        };
        ganttPanel.setPreferredSize(new Dimension(900, 150));
        add(ganttPanel, BorderLayout.NORTH);

        // 3. Table (Center)
        String[] columns = {"Process ID", "Arrival Time", "Burst Time", "Completion Time", "Turnaround Time", "Waiting Time"};
        tableModel = new DefaultTableModel(columns, 0);
        for (Process p : processList) {
            tableModel.addRow(new Object[]{p.pid, p.arrivalTime, p.burstTime, p.completionTime, p.turnAroundTime, p.waitingTime});
        }
        JTable table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // 4. Stats Label (Bottom)
        statsLabel = new JLabel(calculateStats(processList), SwingConstants.CENTER);
        statsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statsLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(statsLabel, BorderLayout.SOUTH);

        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.WEST);

        setLocationRelativeTo(null);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Add Process"));

        // Input fields
        JTextField pidField = new JTextField(5);
        JTextField arrivalField = new JTextField(5);
        JTextField burstField = new JTextField(5);

        panel.add(new JLabel("Process ID (e.g. P1):"));
        panel.add(pidField);
        panel.add(new JLabel("Arrival Time:"));
        panel.add(arrivalField);
        panel.add(new JLabel("Burst Time:"));
        panel.add(burstField);

        JButton addButton = new JButton("Add Process");
        JButton clearButton = new JButton("Clear All");

        panel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacer
        panel.add(addButton);
        panel.add(clearButton);

        // Inside your createInputPanel() or as a class member
        String[] algorithms = {"FCFS", "SJF (Non-Preemptive)"};
        JComboBox<String> algoSelector = new JComboBox<>(algorithms);

        panel.add(algoSelector);

        // When the user clicks "Add Process" or changes the selection:
        algoSelector.addActionListener(e -> {
            String selected = (String) algoSelector.getSelectedItem();
            if ("FCFS".equals(selected)) {
                calculateFCFS(processList);
            } else if("SJF (Non-Preemptive)".equals(selected)) {
                calculateSJF(processList);
            }
            refreshUI();
        });

        // --- The Logic for add button ---
        addButton.addActionListener(e -> {
            try {
                String pid = pidField.getText();
                int arrival = Integer.parseInt(arrivalField.getText());
                int burst = Integer.parseInt(burstField.getText());

                // 1. Add to our list
                processList.add(new Process(pid, arrival, burst));

                // 2. Re-calculate everything
                String selected = (String) algoSelector.getSelectedItem();
                if ("FCFS".equals(selected)) {
                    calculateFCFS(processList);
                } else if("SJF (Non-Preemptive)".equals(selected)) {
                    calculateSJF(processList);
                }

                // 3. Update the Table and Gantt Chart
                refreshUI();

                // Clear fields for next entry
                pidField.setText("");
                arrivalField.setText("");
                burstField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers for Time.");
            }
        });

        return panel;
    }

    private void drawGanttChart(Graphics g,  List<Process> processes) {
        int x = 50; // Starting X-coordinate
        int y = 50; // Starting Y-coordinate
        int height = 50;
        int scale = 40; // 1 second = 40 pixels

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

    private void refreshUI() {
        // Clear the table and re-add all processes
        tableModel.setRowCount(0);
        for (Process p : processList) {
            tableModel.addRow(new Object[]{p.pid, p.arrivalTime, p.burstTime, p.completionTime, p.turnAroundTime, p.waitingTime});
        }

        // Update the stats label (You'll need to make statsLabel a member variable)
        statsLabel.setText(calculateStats(processList));

        // This is the magic command that tells the Gantt Chart to redraw itself!
        repaint();
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

    public void calculateSJF(List<Process> processes) {
        int n = processes.size();
        if (n == 0) return;
        List<Process> sortedProcesses = new ArrayList<>();

        // Reset all processes first
        for(Process p : processes) p.completionTime = 0;

        int currentTime = 0;
        int completed = 0;
        boolean[] isCompleted = new boolean[n];

        while (completed != n) {
            int idx = -1;
            int minBurst = Integer.MAX_VALUE;

            // Find the process that has arrived and has the shortest burst time
            for (int i = 0; i < n; i++) {
                Process p = processes.get(i);
                if (p.arrivalTime <= currentTime && !isCompleted[i]) {
                    if (p.burstTime < minBurst) {
                        minBurst = p.burstTime;
                        idx = i;
                    }
                    // If burst times are equal, pick the one that arrived first (FCFS tie-break)
                    else if (p.burstTime == minBurst) {
                        if (p.arrivalTime < processes.get(idx).arrivalTime) {
                            idx = i;
                        }
                    }
                }
            }

            if (idx != -1) {
                Process p = processes.get(idx);
                p.completionTime = currentTime + p.burstTime;
                p.turnAroundTime = p.completionTime - p.arrivalTime;
                p.waitingTime = p.turnAroundTime - p.burstTime;

                currentTime = p.completionTime;
                isCompleted[idx] = true;
                completed++;
                sortedProcesses.add(p);
            } else {
                // No process has arrived yet, CPU is IDLE
                currentTime++;
            }
        }

        processList.clear();
        processList = sortedProcesses;
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
