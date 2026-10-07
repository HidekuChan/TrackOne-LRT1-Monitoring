package main;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * PRESENTATION STUDY NOTES - MAIN ENTRY POINT (Main.java):
 * ----------------------------------------------------------------------------------
 * 1. RESPONSIBILITY: Serves as the starting point of the Java program.
 * 2. GUI: Adapts Swing GUI components to match native Windows styling.
 * 3. Other stuff:
 *    - Instantiates LrtModel (Database connection setup)
 *    - Instantiates DashboardView (Swing GUI creation)
 *    - Passes Model and View into SimulationController to launch application threads.
 */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Launch GUI safely
        SwingUtilities.invokeLater(() -> {
            LrtModel model = new LrtModel();
            DashboardView view = new DashboardView();
            SimulationController controller = new SimulationController(model, view);
            
            controller.startSimulation();
            view.setVisible(true);
        });
    }
}