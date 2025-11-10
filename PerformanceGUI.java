import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class PerformanceGUI extends JFrame {
    private JTabbedPane tabbedPane;
    
    public PerformanceGUI() {
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("Thread Performance and Synchronization Tests");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Performance Test", new PerformancePanel());
        tabbedPane.addTab("Synchronization Test", new SynchronizationPanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
        setSize(800, 600);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new PerformanceGUI().setVisible(true);
        });
    }
}

// Interface for progress updates
interface ProgressUpdater {
    void updateProgress(int progress);
    void setStatus(String status);
    void appendResult(String text);
}

class PerformancePanel extends JPanel implements ProgressUpdater {
    private JTextArea resultArea;
    private JProgressBar progressBar;
    private JButton startButton;
    private JLabel statusLabel;
    private JPanel chartPanel;
    private long singleThreadTime;
    private long multiThreadTime;
    
    public PerformancePanel() {
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout());
        startButton = new JButton("Start Performance Test");
        statusLabel = new JLabel("Ready to work");
        
        startButton.addActionListener(new StartTestListener());
        
        controlPanel.add(startButton);
        controlPanel.add(statusLabel);
        
        // Progress Bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        
        // Result Area
        resultArea = new JTextArea(15, 50);
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        
        // Chart Panel
        chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawChart(g);
            }
        };
        chartPanel.setPreferredSize(new Dimension(400, 200));
        chartPanel.setBackground(Color.WHITE);
        
        // Layout
        add(controlPanel, BorderLayout.NORTH);
        add(progressBar, BorderLayout.SOUTH);
        add(scrollPane, BorderLayout.CENTER);
        add(chartPanel, BorderLayout.EAST);
    }
    
private void drawChart(Graphics g) {
    if (singleThreadTime == 0 && multiThreadTime == 0) return;
    
    Graphics2D g2d = (Graphics2D) g;
    int width = chartPanel.getWidth();
    int height = chartPanel.getHeight();
    int padding = 50;
    int chartWidth = width - 2 * padding;
    int chartHeight = height - 2 * padding;
    
    long maxTime = Math.max(singleThreadTime, multiThreadTime);
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setColor(Color.BLACK);
    g2d.drawLine(padding, padding, padding, height - padding);
    g2d.drawLine(padding, height - padding, width - padding, height - padding);
    g2d.rotate(-Math.PI/2);
    
    FontMetrics fm = g2d.getFontMetrics();
    String timeLabel = "Time (ms)";
    int timeLabelWidth = fm.stringWidth(timeLabel);
    int centerY = (height - padding - padding) / 2 + padding;
    int timeLabelX = -centerY - timeLabelWidth / 2;
    int timeLabelY = padding - 15;
    
    g2d.drawString(timeLabel, timeLabelX, timeLabelY);
    g2d.rotate(Math.PI/2);

    int barWidth = chartWidth / 4;

    int singleHeight = (int)((singleThreadTime / (double)maxTime) * chartHeight);
    int singleX = padding + barWidth/2;
    g2d.setColor(new Color(70, 130, 180));
    g2d.fillRect(singleX, height - padding - singleHeight, barWidth, singleHeight);
    g2d.setColor(Color.BLACK);
    
    String singleText = singleThreadTime + " ms";
    int textWidth = fm.stringWidth(singleText);
    g2d.drawString(singleText, singleX + (barWidth - textWidth)/2, height - padding - singleHeight - 5);
    
    int multiHeight = (int)((multiThreadTime / (double)maxTime) * chartHeight);
    int multiX = padding + 2 * barWidth;
    g2d.setColor(new Color(34, 139, 34));
    g2d.fillRect(multiX, height - padding - multiHeight, barWidth, multiHeight);
    g2d.setColor(Color.BLACK);
    
    String multiText = multiThreadTime + " ms";
    textWidth = fm.stringWidth(multiText);
    g2d.drawString(multiText, multiX + (barWidth - textWidth)/2, height - padding - multiHeight - 5);
    
    String singleLabel = "Single-thread";
    textWidth = fm.stringWidth(singleLabel);
    g2d.drawString(singleLabel, singleX + (barWidth - textWidth)/2, height - padding + 20);
    
    String multiLabel = "Multi-thread";
    textWidth = fm.stringWidth(multiLabel);
    g2d.drawString(multiLabel, multiX + (barWidth - textWidth)/2, height - padding + 20);
}
    @Override
    public void updateProgress(int progress) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(progress);
        });
    }
    
    @Override
    public void appendResult(String text) {
        SwingUtilities.invokeLater(() -> {
            resultArea.append(text + "\n");
            resultArea.setCaretPosition(resultArea.getDocument().getLength());
        });
    }
    
    @Override
    public void setStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
        });
    }
    
    public void setChartData(long singleTime, long multiTime) {
        this.singleThreadTime = singleTime;
        this.multiThreadTime = multiTime;
        SwingUtilities.invokeLater(() -> {
            chartPanel.repaint();
        });
    }
    
    private class StartTestListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            startButton.setEnabled(false);
            
            new Thread(() -> {
                try {
                    runPerformanceTest();
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        startButton.setEnabled(true);
                    });
                }
            }).start();
        }
    }
    
    private void runPerformanceTest() {
        setStatus("Performance test in progress...");
        appendResult("=== PERFORMANCE TEST ===");
        updateProgress(0);
        
        setStatus("Single-threaded test...");
        long startTime = System.currentTimeMillis();
        Long singleResult = runSingleThreadedWithProgress(this);
        long singleTime = System.currentTimeMillis() - startTime;
        
        appendResult("Single-threaded result: " + singleResult);
        appendResult("Time: " + singleTime + " ms");
        appendResult("");
        
        setStatus("Multi-threaded test...");
        startTime = System.currentTimeMillis();
        Long multiResult = runMultiThreadedWithProgress(this);
        long multiTime = System.currentTimeMillis() - startTime;
        
        appendResult("Multi-threaded result: " + multiResult);
        appendResult("Time: " + multiTime + " ms");
        appendResult("");
        
        double speedup = (double) singleTime / multiTime;
        appendResult(String.format("Speedup: %.2f times", speedup));
        appendResult("============================\n");
        
        setChartData(singleTime, multiTime);
        setStatus("Performance test completed");
        updateProgress(0);
    }
    
    private Long runSingleThreadedWithProgress(ProgressUpdater updater) {
        Long summa = 0L;
        int total = Processor.STR_COUNT;
        
        for (int i = 0; i < total; i++) {
            Processor p = new Processor();
            summa += p.process();
            
            int progress = (int)((i + 1) / (double)total * 50);
            updater.updateProgress(progress);
            updater.setStatus("Single-threaded: " + (i + 1) + "/" + total);
        }
        return summa;
    }
    
    private Long runMultiThreadedWithProgress(ProgressUpdater updater) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(availableProcessors);
        Long summa = 0L;
        
        try {
            List<Callable<Long>> tasks = new ArrayList<>();
            for (int i = 0; i < Processor.STR_COUNT; i++) {
                final int taskNumber = i;
                tasks.add(() -> {
                    Processor p = new Processor();
                    Long result = p.process();
                    
                    // Update progress
                    SwingUtilities.invokeLater(() -> {
                        int progress = 50 + (int)((taskNumber + 1) / (double)Processor.STR_COUNT * 50);
                        updater.updateProgress(progress);
                        updater.setStatus("Multi-threaded: " + (taskNumber + 1) + "/" + Processor.STR_COUNT);
                    });
                    
                    return result;
                });
            }
            
            List<Future<Long>> results = executor.invokeAll(tasks);
            for (Future<Long> future : results) {
                summa += future.get();
            }
            
            executor.shutdown();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        
        return summa;
    }
}

class SynchronizationPanel extends JPanel implements ProgressUpdater {
    private JTextArea resultArea;
    private JButton startButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    
    public SynchronizationPanel() {
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        JPanel controlPanel = new JPanel(new FlowLayout());
        startButton = new JButton("Start Synchronization Test");
        statusLabel = new JLabel("Ready to work");
        
        startButton.addActionListener(new StartTestListener());
        
        controlPanel.add(startButton);
        controlPanel.add(statusLabel);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        resultArea = new JTextArea(10, 50);
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        
        add(controlPanel, BorderLayout.NORTH);
        add(progressBar, BorderLayout.SOUTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    @Override
    public void appendResult(String text) {
        SwingUtilities.invokeLater(() -> {
            resultArea.append(text + "\n");
            resultArea.setCaretPosition(resultArea.getDocument().getLength());
        });
    }
    
    @Override
    public void setStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
        });
    }
    
    @Override
    public void updateProgress(int progress) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(progress);
        });
    }
    
    private class StartTestListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            startButton.setEnabled(false);
            
            new Thread(() -> {
                try {
                    runSynchronizationTest();
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        startButton.setEnabled(true);
                    });
                }
            }).start();
        }
    }
    
    private void runSynchronizationTest() {
        setStatus("Synchronization test in progress...");
        appendResult("=== SYNCHRONIZATION TEST ===");
        
        Counter counter = new Counter();
        List<Thread> threads = new ArrayList<>();
        int threadCount = 200;
        int incrementsPerThread = 1000;
        
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new CounterThread(counter);
            threads.add(thread);
            
            int progress = (int)((i + 1) / (double)threadCount * 50);
            updateProgress(progress);
            setStatus("Creating threads: " + (i + 1) + "/" + threadCount);
        }
        
        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).start();
            
            int progress = 50 + (int)((i + 1) / (double)threads.size() * 50);
            updateProgress(progress);
            setStatus("Starting threads: " + (i + 1) + "/" + threads.size());
        }
        
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        long expected = threadCount * incrementsPerThread;
        long actual = counter.getCounter();
        
        appendResult("Expected value: " + expected);
        appendResult("Actual value: " + actual);
        appendResult("Difference: " + (expected - actual));
        
        if (expected == actual) {
            appendResult("SUCCESS: Synchronization works correctly!!!");
        } else {
            appendResult("ERROR: Synchronization problem!");
        }
        appendResult("===========================\n");
        
        setStatus("Synchronization test completed");
        updateProgress(0);
        
        System.out.println("Counter:" + counter.getCounter());
    }
}