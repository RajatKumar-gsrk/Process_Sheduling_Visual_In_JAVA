import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import java.util.Queue;

public class SchedulerVisualizer extends JFrame {

    private List<Process> processList;
    private List<Process> currentTimeLine;
    private DefaultTableModel tableModel; // Keep this to update the table
    private JLabel statsLabel;

    private SchedulerVisualizer() {
        setTitle("OS Process Scheduler Visualizer");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout()); // Using BorderLayout for structure

        // 1. Data Setup
        processList = new ArrayList<>(Arrays.asList(
                new Process("P1", 0, 8),
                new Process("P2", 1, 4),
                new Process("P3", 2, 1)
        ));//Arrays.aslist returns fixed size list can't add or remove so we wrap it
        calculateFCFS(processList);

        // 2. Gantt Chart Panel (Top)
        JPanel ganttPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGanttChart(g, currentTimeLine); // Pass the list here
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
        panel.setPreferredSize(new Dimension(300, 600 - 150));
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

        // Create a slider: Min 1, Max 10, Initial 2
        JSlider quantumSlider = new JSlider(JSlider.HORIZONTAL, 1, 10, 2);
        quantumSlider.setMajorTickSpacing(1);
        quantumSlider.setPaintTicks(true);
        quantumSlider.setPaintLabels(true);
        // Create a label to show the current value
        JLabel quantumValueLabel = new JLabel("Value: "+(Integer)quantumSlider.getValue());
        // Add them to the panel
        panel.add(new JLabel("Time Quantum (RR):"));
        quantumSlider.setSnapToTicks(true);
        // Sub-panel for better alignment
        JPanel sliderGroup = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sliderGroup.add(quantumSlider);
        sliderGroup.add(quantumValueLabel);
        panel.add(sliderGroup);

        JButton addButton = new JButton("Add Process");
        JButton clearButton = new JButton("Clear All");

        panel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacer
        panel.add(addButton);
        panel.add(clearButton);

        // Inside your createInputPanel() or as a class member
        String[] algorithms = {"FCFS", "SJF (Non-Preemptive)", "SRTF", "Round Robin"};
        JComboBox<String> algoSelector = new JComboBox<>(algorithms);

        panel.add(algoSelector);

        // When the user clicks "Add Process" or changes the selection:
        algoSelector.addActionListener(e -> {
            String selected = (String) algoSelector.getSelectedItem();
            if ("FCFS".equals(selected)) {
                calculateFCFS(processList);
            } else if("SJF (Non-Preemptive)".equals(selected)) {
                calculateSJF(processList);
            } else if("SRTF".equals(selected)) {
                calculateSRTF(processList);
            } else if("Round Robin".equals(selected)) {
                calculateRR(processList, (Integer) quantumSlider.getValue());
            }

            refreshUI();
        });

        // --- The Logic for add button ---

        quantumSlider.addChangeListener(e -> {
            quantumValueLabel.setText("Value: " + quantumSlider.getValue());

            // Only re-calculate if we are currently in Round Robin mode
            if ("Round Robin".equals((String)algoSelector.getSelectedItem())) {
                calculateRR(processList, quantumSlider.getValue());
                refreshUI();
            }
        });

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
                } else if("SRTF".equals(selected)) {
                    calculateSRTF(processList);
                } else if("Round Robin".equals(selected)) {
                    calculateRR(processList, (Integer)quantumSlider.getValue());
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

        clearButton.addActionListener(e -> {
           processList.clear();
           currentTimeLine.clear();
           refreshUI();
        });

        return panel;
    }

    private void drawGanttChart(Graphics g,  List<Process> processes) {
        int x = 50; // Starting X-coordinate
        int y = 50; // Starting Y-coordinate
        int height = 50;
        int totalTime = processes.get(processes.size()-1).completionTime;
        int availableWidth = getWidth() - 100;
        double scale = (double)availableWidth / totalTime;
        int currentTime = 0;

        for (Process p : processes) {
            // Handle Idle Time visually
            if (p.arrivalTime > currentTime) {
                int idleWidth = (int)((p.arrivalTime - currentTime) * scale);
                g.setColor(Color.LIGHT_GRAY);
                g.drawRect(x, y, idleWidth, height);
                g.drawString("Idle", x + 5, y + 30);
                x += idleWidth;
                currentTime = p.arrivalTime;
            }

            // Draw Process block
            int width = (int)(p.burstTime * scale);
            g.setColor(new Color((Math.abs(p.pid.hashCode() * p.pid.hashCode()) % 255), (Math.abs(p.pid.hashCode() * p.pid.hashCode()) % 255), 220));
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

    private void calculateFCFS(List<Process> processes) {
        // 1. Sort processes by arrival time
        currentTimeLine = new ArrayList<>(processes);
        currentTimeLine.sort(Comparator.comparingInt(p -> p.arrivalTime));

        int currentTime = 0;
        for (Process p : currentTimeLine) {
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

            for(Process pp : processes) {
                if (pp.pid.equals(p.pid)) {
                    pp.completionTime = p.completionTime;
                    pp.turnAroundTime = p.turnAroundTime;
                    pp.waitingTime = p.waitingTime;
                }
            }
        }
    }

    private void calculateSJF(List<Process> processes) {
        int n = processes.size();
        if (n == 0) return;
        List<Process> sortedProcesses = new ArrayList<>();
        currentTimeLine = new ArrayList<>(processes);

        // Reset all processes first
        for(Process p : currentTimeLine) p.completionTime = 0;

        int currentTime = 0;
        int completed = 0;
        boolean[] isCompleted = new boolean[n];

        while (completed != n) {
            int idx = -1;
            int minBurst = Integer.MAX_VALUE;

            // Find the process that has arrived and has the shortest burst time
            for (int i = 0; i < n; i++) {
                Process p = currentTimeLine.get(i);
                if (p.arrivalTime <= currentTime && !isCompleted[i]) {
                    if (p.burstTime < minBurst) {
                        minBurst = p.burstTime;
                        idx = i;
                    }
                    // If burst times are equal, pick the one that arrived first (FCFS tie-break)
                    else if (p.burstTime == minBurst) {
                        if (p.arrivalTime < currentTimeLine.get(idx).arrivalTime) {
                            idx = i;
                        }
                    }
                }
            }

            if (idx != -1) {
                Process p = processList.get(idx);
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

        currentTimeLine.clear();
        currentTimeLine = sortedProcesses;
    }

    public void calculateSRTF(List<Process> processes) {
        List<GanttBlock> timeline = new ArrayList<>();
        int n = processes.size();
        int currentTime = 0;
        int completed = 0;
        String lastPid = "";
        int blockStart = 0;
        currentTimeLine = new ArrayList<>(processes);

        // We must work on copies because we will be subtracting from remainingTime
        for (Process p : currentTimeLine) {
            p.remainingTime = p.burstTime;
        }

        while (completed != n) {
            int idx = -1;
            int minRemaining = Integer.MAX_VALUE;

            for (int i = 0; i < n; i++) {
                Process p = currentTimeLine.get(i);
                if (p.arrivalTime <= currentTime && p.remainingTime > 0) {
                    if (p.remainingTime < minRemaining) {
                        minRemaining = p.remainingTime;
                        idx = i;
                    }
                }
            }

            if (idx != -1) {
                Process p = currentTimeLine.get(idx);

                // Check for Context Switch: Did a new process take over?
                if (!p.pid.equals(lastPid)) {
                    if (!lastPid.equals("")) {
                        timeline.add(new GanttBlock(lastPid, blockStart, currentTime));
                    }
                    blockStart = currentTime;
                    lastPid = p.pid;
                }

                p.remainingTime--;
                currentTime++;

                if (p.remainingTime == 0) {
                    processList.get(idx).completionTime = currentTime;
                    processList.get(idx).turnAroundTime = p.completionTime - p.arrivalTime;
                    processList.get(idx).waitingTime = p.turnAroundTime - p.burstTime;
                    completed++;

                    // Add block to timeline when process finishes
                    timeline.add(new GanttBlock(p.pid, blockStart, currentTime));
                    lastPid = ""; // Reset for next potential process
                }
            } else {
                // CPU is Idle
                if (!lastPid.equals("Idle")) {
                    if (!lastPid.equals("")) {
                        timeline.add(new GanttBlock(lastPid, blockStart, currentTime));
                    }
                    blockStart = currentTime;
                    lastPid = "Idle";
                }
                currentTime++;
            }
        }
        currentTimeLine.clear();
        for(GanttBlock g : timeline) {
            if(!g.pid.equals("Idle")) {
                currentTimeLine.add(new Process(g.pid, g.startTime, g.endTime - g.startTime));
                currentTimeLine.get(currentTimeLine.size() - 1).completionTime = g.endTime;
            }
        }
    }

    public void calculateRR(List<Process> processes, int timeQuanta) {
        List<GanttBlock> timeline = new ArrayList<>();
        int n = processes.size();
        int currentTime = 0;
        int completed = 0;
        int addedToQueue = 0;
        String lastPid = "";
        int blockStart = 0;
        currentTimeLine = new ArrayList<>(processes);
        Queue<Process> processQueue = new LinkedList<>();
        boolean[] isAddedToQueue = new boolean[n];

        // We must work on copies because we will be subtracting from remainingTime
        for (Process p : currentTimeLine) {
            p.remainingTime = p.burstTime;
        }

        while (completed != n) {

            //context switch
            for (int i = 0; i < n && addedToQueue < n; i++) {
                Process p = currentTimeLine.get(i);
                if (p.arrivalTime <= currentTime && p.remainingTime > 0) {

                    if (!isAddedToQueue[i]) {
                        isAddedToQueue[i] = true;
                        processQueue.add(p);
                        addedToQueue++;
                    }
                }
            }

            if (!processQueue.isEmpty()) {
                Process p = processQueue.poll();

                // Check for Context Switch: Did a new process take over?
                if (!p.pid.equals(lastPid)) {
                    if (!lastPid.equals("")) {
                        timeline.add(new GanttBlock(lastPid, blockStart, currentTime));
                    }
                    blockStart = currentTime;
                    lastPid = p.pid;
                }

                int newRemianingTime = Math.max(0, p.remainingTime - timeQuanta);
                currentTime += Math.min(timeQuanta, p.remainingTime - newRemianingTime);
                p.remainingTime = newRemianingTime;

                if (p.remainingTime == 0) {
                    for(Process pp : processList){
                        if(pp.pid.equals(p.pid)){
                            pp.completionTime = currentTime;
                            pp.turnAroundTime = p.completionTime - p.arrivalTime;
                            pp.waitingTime = p.turnAroundTime - p.burstTime;
                        }
                    }

                    completed++;

                    // Add block to timeline when process finishes
                    timeline.add(new GanttBlock(p.pid, blockStart, currentTime));
                    lastPid = ""; // Reset for next potential process
                }else{
                    //check for context switch
                    for (int i = 0; i < n && addedToQueue < n; i++) {
                        Process q = currentTimeLine.get(i);
                        if (q.arrivalTime <= currentTime && q.remainingTime > 0) {

                            if (!isAddedToQueue[i]) {
                                isAddedToQueue[i] = true;
                                processQueue.add(q);
                                addedToQueue++;
                            }
                        }
                    }
                    processQueue.add(p);
                }
            } else {
                // CPU is Idle
                if (!lastPid.equals("Idle")) {
                    if (!lastPid.equals("")) {
                        timeline.add(new GanttBlock(lastPid, blockStart, currentTime));
                    }
                    blockStart = currentTime;
                    lastPid = "Idle";
                }
                currentTime++;
            }
        }
        currentTimeLine.clear();
        for(GanttBlock g : timeline) {
            if(!g.pid.equals("Idle")) {
                currentTimeLine.add(new Process(g.pid, g.startTime, g.endTime - g.startTime));
                currentTimeLine.get(currentTimeLine.size() - 1).completionTime = g.endTime;
            }
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
        //lost commits due to email conflicts
    }
}
