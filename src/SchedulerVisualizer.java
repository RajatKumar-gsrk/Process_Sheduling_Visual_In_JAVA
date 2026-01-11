import javax.swing.*;
import java.awt.*;

public class SchedulerVisualizer extends JFrame {

    public SchedulerVisualizer() {
        setTitle("OS Process Scheduler Visualizer");
        setSize(800, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // This is where our Gantt Chart will be drawn
        JPanel canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGanttChart(g);
            }
        };

        add(canvas);
    }

    private void drawGanttChart(Graphics g) {
        g.drawString("Gantt Chart will appear here...", 50, 50);
        // We will implement the actual drawing logic next!
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SchedulerVisualizer().setVisible(true);
        });
    }
}
