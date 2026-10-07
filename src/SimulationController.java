package main;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * PRESENTATION STUDY NOTES - CONTROLLER LAYER (SimulationController.java):
 * ----------------------------------------------------------------------------------
 * 1. Purpose: Acts as the intermediary between LrtModel and DashboardView.
 * 2. For time: Uses ScheduledExecutorService to run a background timer loop
 *    every 1 real second, advancing simulated time by 10 minutes per pulse.
 * 3. Logic: Evaluates operational schedules, rush hour status,
 *    holiday recognition, and seasonal weather dynamics.
 */
public class SimulationController {
    private final LrtModel model;
    private final DashboardView view;
    private ScheduledExecutorService executor;

    // Simulation starts on Day 1: January 1, 2026 at 04:00 AM
    private LocalDateTime simTime = LocalDateTime.of(2026, 1, 1, 4, 0); 
    private final Set<MonthDay> holidays = new HashSet<>();

    public SimulationController(LrtModel model, DashboardView view) {
        this.model = model;
        this.view = view;
        initHolidays();
        setupController();
    }

    /**
     * HOLIDAYS:
     * - "How does the system know when it's a holiday?"
     * - MonthDay table stores official Philippine national holidays.
     *   On holidays, Rush Hour is overridden to OFF.
     */
    private void initHolidays() {
        holidays.add(MonthDay.of(1, 1));   // New Year's Day
        holidays.add(MonthDay.of(4, 9));   // Day of Valor
        holidays.add(MonthDay.of(5, 1));   // Labor Day
        holidays.add(MonthDay.of(6, 12));  // Independence Day
        holidays.add(MonthDay.of(8, 21));  // Ninoy Aquino Day
        holidays.add(MonthDay.of(11, 1));  // All Saints' Day
        holidays.add(MonthDay.of(11, 2));  // All Souls' Day
        holidays.add(MonthDay.of(11, 30)); // Bonifacio Day
        holidays.add(MonthDay.of(12, 25)); // Christmas Day
        holidays.add(MonthDay.of(12, 30)); // Rizal Day
        holidays.add(MonthDay.of(12, 31)); // New Year's Eve
    }

    private void setupController() {
        // Fetch baseline data on launch
        List<Object[]> initialData = model.getCongestionData(false);
        view.initializeTableRows(initialData);

        // Safe Thread Shutdown on Window Close
        view.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (executor != null && !executor.isShutdown()) {
                    executor.shutdownNow();
                }
            }
        });
    }

    /**
     * SIMULATION LOOP:
     * - "How does time progress in your project?"
     * - executor.scheduleAtFixedRate executes every 1 second.
     *   Each second advances simTime by 10 minutes (+10 mins/sec).
     * - Rush Hour Schedule:
     *   Morning Rush: 06:30 AM - 09:00 AM (Weekdays only)
     *   Evening Rush: 05:00 PM - 08:00 PM (Weekdays only)
     */
    public void startSimulation() {
        executor = Executors.newScheduledThreadPool(2);

        executor.scheduleAtFixedRate(() -> {
            try {
                // Advance clock by 10 simulated minutes every 1 real-world second
                simTime = simTime.plusMinutes(10);
                
                // 365-Day Cycle Reset
                if (simTime.getYear() > 2026) {
                    simTime = LocalDateTime.of(2026, 1, 1, 4, 0);
                }

                int dayOfYear = simTime.getDayOfYear();
                boolean isWeekend = simTime.getDayOfWeek() == DayOfWeek.SATURDAY || simTime.getDayOfWeek() == DayOfWeek.SUNDAY;
                boolean isHoliday = holidays.contains(MonthDay.of(simTime.getMonthValue(), simTime.getDayOfMonth()));
                boolean isWeekendOrHoliday = isWeekend || isHoliday;

                // Philippine Weather Season Toggle (Rainy Season = June to November)
                int month = simTime.getMonthValue();
                boolean isRainySeason = (month >= 6 && month <= 11);

                // Operating Hours Evaluation
                LocalTime time = simTime.toLocalTime();
                LocalTime openTime = isWeekendOrHoliday ? LocalTime.of(5, 0) : LocalTime.of(4, 30);
                LocalTime closeTime = isWeekendOrHoliday ? LocalTime.of(21, 45) : LocalTime.of(22, 15);
                boolean isOpen = !time.isBefore(openTime) && !time.isAfter(closeTime);

                // Rush Hour Active Window Check
                boolean isMorningRush = time.isAfter(LocalTime.of(6, 29)) && time.isBefore(LocalTime.of(9, 1));
                boolean isEveningRush = time.isAfter(LocalTime.of(16, 59)) && time.isBefore(LocalTime.of(20, 1));
                boolean isRushHour = isOpen && !isWeekendOrHoliday && (isMorningRush || isEveningRush);

                // Execute Model database actions
                int taps = model.generateMockTaps(isOpen, isRushHour, isRainySeason, isHoliday);
                List<Object[]> data = model.getCongestionData(isOpen);

                // String formatting for GUI display
                String formattedTime = simTime.format(DateTimeFormatter.ofPattern("EEE, MMM dd | hh:mm a"));
                String timeText = String.format("Day %d / 365 | %s", dayOfYear, formattedTime);
                String seasonText = String.format("Season: %s | %s", 
                        isRainySeason ? "RAINY" : "DRY", 
                        isHoliday ? "HOLIDAY" : (isWeekend ? "WEEKEND" : "WEEKDAY"));
                String statusText = String.format("LRT-1: %s | Rush Hour: %s", 
                        isOpen ? "OPEN" : "CLOSED", 
                        isRushHour ? "ACTIVE" : "OFF");

                String logTime = simTime.format(DateTimeFormatter.ofPattern("HH:mm"));

                // Update Swing GUI components
                SwingUtilities.invokeLater(() -> {
                    try {
                        view.updateClockHeader(timeText, seasonText, statusText, isOpen, isRushHour);
                        view.updateTableData(data, logTime);
                        if (taps > 0) {
                            view.appendLog("TAP GENERATOR: Simulated " + taps + " tap transactions.", logTime);
                        } else if (!isOpen && simTime.getMinute() == 0) {
                            view.appendLog("SYSTEM: LRT-1 is currently CLOSED.", logTime);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

            } catch (Exception e) {
                System.err.println("Error in simulation thread loop: " + e.getMessage());
                e.printStackTrace();
            }

        }, 1, 1, TimeUnit.SECONDS); // Runs every 1 second
    }
}