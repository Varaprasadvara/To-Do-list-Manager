import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicButtonUI;

public class UIHelper {

    private static final Color ADD_BG = new Color(0x2E, 0x7D, 0x32);
    private static final Color COMPLETE_BG = new Color(0x15, 0x65, 0xC0);
    private static final Color DELETE_BG = new Color(0xC6, 0x28, 0x28);
    private static final Color CLEAR_BG = new Color(0xEF, 0x6C, 0x00);
    private static final Color EXIT_BG = new Color(0x42, 0x42, 0x42);
    private static final Color MUTE_BG = new Color(0x75, 0x75, 0x75);
    private static final Color DARK_BG = new Color(0x20, 0x27, 0x2B);
    private static final Color DARK_CARD_BG = new Color(0x30, 0x3A, 0x40);
    private static final Color HEADER_BG = new Color(0x1B, 0x5E, 0x20);
    private static final Color INPUT_BORDER = new Color(0xB0, 0xBE, 0xC5);

    private static final Color BUTTON_FG = Color.WHITE;
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 26);
    private static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    private final TaskManager taskManager;
    private final SoundNotifier soundNotifier;

    private JFrame frame;
    private JPanel contentPanel;
    private JTextField titleField;
    private JTextArea descriptionArea;
    private JComboBox<String> priorityCombo;
    private JTextField dueDateField;
    private JCheckBox reminderCheckBox;
    private JSpinner hourSpinner;
    private JSpinner minuteSpinner;
    private JButton notificationToggleButton;
    private JTextField searchField;
    private JLabel totalTasksLabel;
    private JLabel completedTasksLabel;
    private JLabel pendingTasksLabel;
    private JButton darkModeButton;
    private boolean darkMode;
    private final Map<Task, ScheduledFuture<?>> reminderJobs = new IdentityHashMap<>();
    private JPanel taskCardsPanel;
    private final ScheduledExecutorService reminderScheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "task-reminders");
                thread.setDaemon(true);
                return thread;
            });

    public UIHelper(TaskManager taskManager, SoundNotifier soundNotifier) {
        this.taskManager = taskManager;
        this.soundNotifier = soundNotifier;
    }

    public void createAndShowGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        frame = new JFrame("ToDo List Manager");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(620, 620);
        frame.setMinimumSize(new Dimension(560, 560));
        frame.setLocationRelativeTo(null);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                taskManager.saveTasks();
                reminderScheduler.shutdownNow();
                frame.dispose();
            }
        });

        contentPanel = buildContentPanel();
        frame.setContentPane(contentPanel);

        refreshTaskList();
        scheduleSavedReminders();
        frame.setVisible(true);
    }

    private JPanel buildContentPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(0xF5, 0xF5, 0xF5));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        panel.add(createAppHeader(), gbc);

        gbc.gridy = 1;
        panel.add(createStatisticsPanel(), gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;
        JLabel lblTitle = new JLabel("Title:");
        lblTitle.setFont(LABEL_FONT);
        panel.add(lblTitle, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        titleField = new JTextField(20);
        styleInput(titleField);
        titleField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER),
                BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        titleField.setToolTipText("Enter a short, clear task title");
        panel.add(titleField, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 3;
        gbc.gridx = 0;
        JLabel lblDesc = new JLabel("Description:");
        lblDesc.setFont(LABEL_FONT);
        panel.add(lblDesc, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        descriptionArea = new JTextArea(4, 20);
        styleInput(descriptionArea);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setToolTipText("Add helpful details about this task");
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        panel.add(descScroll, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 4;
        gbc.gridx = 0;
        JLabel lblPriority = new JLabel("Priority:");
        lblPriority.setFont(LABEL_FONT);
        panel.add(lblPriority, gbc);
        gbc.gridx = 1;
        priorityCombo = new JComboBox<>(new String[]{"High", "Medium", "Low"});
        priorityCombo.setFont(LABEL_FONT);
        panel.add(priorityCombo, gbc);

        gbc.gridx = 2;
        JLabel lblDue = new JLabel("Due Date:");
        lblDue.setFont(LABEL_FONT);
        panel.add(lblDue, gbc);
        gbc.gridx = 3;
        dueDateField = new JTextField(10);
        dueDateField.setFont(LABEL_FONT);
        dueDateField.setToolTipText("yyyy-MM-dd (optional)");
        panel.add(dueDateField, gbc);

        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        JLabel lblTimer = new JLabel("Reminder Time:");
        lblTimer.setFont(LABEL_FONT);
        panel.add(lblTimer, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        panel.add(createTimePicker(), gbc);

        gbc.gridy = 6;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(LABEL_FONT);
        panel.add(searchLabel, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        searchField = new JTextField(20);
        searchField.setFont(LABEL_FONT);
        searchField.setToolTipText("Search task titles and descriptions");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshTaskList(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshTaskList(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshTaskList(); }
        });
        panel.add(searchField, gbc);

        gbc.gridy = 7;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        taskCardsPanel = new JPanel();
        taskCardsPanel.setLayout(new javax.swing.BoxLayout(taskCardsPanel,
                javax.swing.BoxLayout.Y_AXIS));
        taskCardsPanel.setBackground(Color.WHITE);
        JScrollPane listScroll = new JScrollPane(taskCardsPanel);
        listScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Tasks",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                LABEL_FONT));
        panel.add(listScroll, gbc);

        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridwidth = 4;
        gbc.gridy = 8;
        gbc.gridx = 0;
        panel.add(buildButtonRow(), gbc);

        return panel;
    }

    private JPanel buildButtonRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(new Color(0xF5, 0xF5, 0xF5));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JButton addBtn = makeButton("Add", ADD_BG);
        JButton clearBtn = makeButton("Clear", CLEAR_BG);
        notificationToggleButton = makeButton("Sound: ON", COMPLETE_BG);
        darkModeButton = makeButton("Dark Mode", EXIT_BG);
        JButton exitBtn = makeButton("Exit", EXIT_BG);

        addBtn.addActionListener(e -> handleAdd());
        clearBtn.addActionListener(e -> handleClear());
        notificationToggleButton.addActionListener(e -> toggleNotifications());
        darkModeButton.addActionListener(e -> toggleDarkMode());
        exitBtn.addActionListener(e -> handleExit());

        gbc.gridx = 0;
        row.add(addBtn, gbc);
        gbc.gridx = 1;
        row.add(clearBtn, gbc);
        gbc.gridx = 2;
        row.add(notificationToggleButton, gbc);
        gbc.gridx = 3;
        row.add(darkModeButton, gbc);
        gbc.gridx = 4;
        row.add(exitBtn, gbc);

        return row;
    }

    private JButton makeButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setForeground(BUTTON_FG);
        button.setBackground(bg);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setUI(new RoundedButtonUI(bg));
        return button;
    }

    private void handleAdd() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            showError("Please enter a task title.");
            return;
        }
        String description = descriptionArea.getText().trim();
        String priority = (String) priorityCombo.getSelectedItem();
        LocalDate dueDate = Task.parseDate(dueDateField.getText());
        if (!dueDateField.getText().trim().isEmpty() && dueDate == null) {
            showError("Invalid due date format. Use yyyy-MM-dd.");
            return;
        }
        LocalTime reminderTime = getSelectedReminderTime();
        if (reminderTime != null && dueDate == null) {
            showError("Enter a due date for the reminder.");
            return;
        }
        if (reminderTime != null && !LocalDateTime.of(dueDate, reminderTime)
                .isAfter(LocalDateTime.now())) {
            showError("Reminder date and time must be in the future.");
            return;
        }

        Task task = new Task(title, description, priority, dueDate);
        task.setReminderTime(reminderTime);
        if (taskManager.addTask(task)) {
            soundNotifier.notifyTaskAdded();
            if (reminderTime != null) {
                scheduleReminder(task);
            }
            clearInputFields();
            refreshTaskList();
            taskManager.saveTasks();
        } else {
            showError("Could not add the task.");
        }
    }

    private void handleComplete(Task task) {
        int index = taskManager.getTaskIndex(task);
        if (index < 0) {
            return;
        }
        boolean newState = !task.isCompleted();
        taskManager.setCompleted(index, newState);
        if (newState) {
            soundNotifier.notifyTaskCompleted();
            cancelReminder(task);
        } else if (task.getReminderTime() != null) {
            scheduleReminder(task);
        }
        refreshTaskList();
        taskManager.saveTasks();
    }

    private void handleDelete(Task task) {
        int index = taskManager.getTaskIndex(task);
        if (index < 0) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(frame,
                "Are you sure you want to delete this task?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            cancelReminder(task);
            taskManager.deleteTask(index);
            soundNotifier.notifyTaskDeleted();
            refreshTaskList();
            taskManager.saveTasks();
        }
    }

    private void handleClear() {
        clearInputFields();
        refreshTaskList();
    }

    private void handleExit() {
        taskManager.saveTasks();
        reminderScheduler.shutdownNow();
        frame.dispose();
    }

    private void toggleNotifications() {
        boolean enableNotifications = !soundNotifier.isSoundEnabled();
        soundNotifier.setSoundEnabled(enableNotifications);
        soundNotifier.setVoiceEnabled(enableNotifications);
        notificationToggleButton.setText(enableNotifications ? "Sound: ON" : "Muted");
        notificationToggleButton.setUI(new RoundedButtonUI(
                enableNotifications ? COMPLETE_BG : MUTE_BG));
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        darkModeButton.setText(darkMode ? "Light Mode" : "Dark Mode");
        applyTheme(contentPanel);
        Color statsBackground = darkMode ? new Color(0x45, 0x55, 0x5D)
                : new Color(0xE8, 0xF5, 0xE9);
        Color statsForeground = darkMode ? Color.WHITE : new Color(0x26, 0x32, 0x38);
        totalTasksLabel.setBackground(statsBackground);
        completedTasksLabel.setBackground(statsBackground);
        pendingTasksLabel.setBackground(statsBackground);
        totalTasksLabel.setForeground(statsForeground);
        completedTasksLabel.setForeground(statsForeground);
        pendingTasksLabel.setForeground(statsForeground);
        refreshTaskList();
    }

    private void applyTheme(Component component) {
        if (component instanceof javax.swing.JComponent
                && Boolean.TRUE.equals(((javax.swing.JComponent) component)
                        .getClientProperty("themeLocked"))) {
            return;
        }
        Color background = darkMode ? DARK_BG : new Color(0xF5, 0xF5, 0xF5);
        Color foreground = darkMode ? new Color(0xF5, 0xF5, 0xF5)
                : new Color(0x26, 0x32, 0x38);
        if (component instanceof JPanel) {
            JPanel panel = (JPanel) component;
            if (Boolean.TRUE.equals(panel.getClientProperty("taskCard"))) {
                panel.setBackground(darkMode ? DARK_CARD_BG : Color.WHITE);
            } else if (panel.isOpaque()) {
                panel.setBackground(background);
            }
        } else if (component instanceof JLabel) {
            ((JLabel) component).setForeground(foreground);
        } else if (component instanceof JTextField || component instanceof JTextArea) {
            component.setBackground(darkMode ? DARK_CARD_BG : Color.WHITE);
            component.setForeground(foreground);
        }
        if (component instanceof java.awt.Container) {
            for (Component child : ((java.awt.Container) component).getComponents()) {
                applyTheme(child);
            }
        }
    }

    private void handleEdit(Task task) {
        JTextField editTitle = new JTextField(task.getTitle());
        JTextArea editDescription = new JTextArea(task.getDescription(), 4, 22);
        editDescription.setLineWrap(true);
        editDescription.setWrapStyleWord(true);
        JComboBox<String> editPriority = new JComboBox<>(
                new String[]{"High", "Medium", "Low"});
        editPriority.setSelectedItem(task.getPriority());
        JTextField editDate = new JTextField(task.getDueDateAsString());

        JPanel editor = new JPanel();
        editor.setLayout(new javax.swing.BoxLayout(editor,
                javax.swing.BoxLayout.Y_AXIS));
        editor.add(new JLabel("Title"));
        editor.add(editTitle);
        editor.add(javax.swing.Box.createVerticalStrut(6));
        editor.add(new JLabel("Description"));
        editor.add(new JScrollPane(editDescription));
        editor.add(javax.swing.Box.createVerticalStrut(6));
        editor.add(new JLabel("Priority"));
        editor.add(editPriority);
        editor.add(javax.swing.Box.createVerticalStrut(6));
        editor.add(new JLabel("Due date (yyyy-MM-dd)"));
        editor.add(editDate);

        int result = JOptionPane.showConfirmDialog(frame, editor, "Edit Task",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        String title = editTitle.getText().trim();
        LocalDate dueDate = Task.parseDate(editDate.getText());
        if (title.isEmpty()) {
            showError("Please enter a task title.");
            return;
        }
        if (!editDate.getText().trim().isEmpty() && dueDate == null) {
            showError("Invalid due date format. Use yyyy-MM-dd.");
            return;
        }
        if (task.getReminderTime() != null && (dueDate == null
                || !LocalDateTime.of(dueDate, task.getReminderTime())
                        .isAfter(LocalDateTime.now()))) {
            showError("A task with a reminder needs a future due date.");
            return;
        }
        cancelReminder(task);
        task.setTitle(title);
        task.setDescription(editDescription.getText().trim());
        task.setPriority((String) editPriority.getSelectedItem());
        task.setDueDate(dueDate);
        if (!task.isCompleted() && task.getReminderTime() != null) {
            scheduleReminder(task);
        }
        taskManager.saveTasks();
        refreshTaskList();
    }

    private void refreshTaskList() {
        taskCardsPanel.removeAll();
        String query = searchField == null ? "" : searchField.getText().trim();
        java.util.List<Task> toShow = query.isEmpty() ? taskManager.getAllTasks()
                : taskManager.searchTasks(query);
        for (Task t : toShow) {
            taskCardsPanel.add(createTaskCard(t));
            taskCardsPanel.add(javax.swing.Box.createVerticalStrut(8));
        }
        if (toShow.isEmpty()) {
            JLabel emptyLabel = new JLabel("No tasks yet.");
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLabel.setForeground(darkMode ? Color.WHITE : new Color(0x26, 0x32, 0x38));
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
            taskCardsPanel.add(emptyLabel);
        }
        taskCardsPanel.revalidate();
        taskCardsPanel.repaint();
        updateStatistics();
    }

    private JPanel createAppHeader() {
        JPanel header = new JPanel();
        header.setLayout(new javax.swing.BoxLayout(header,
                javax.swing.BoxLayout.Y_AXIS));
        header.setBackground(HEADER_BG);
        header.putClientProperty("themeLocked", Boolean.TRUE);
        header.setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 12));

        JLabel appName = new JLabel("To-Do List", JLabel.CENTER);
        appName.setFont(TITLE_FONT);
        appName.setForeground(Color.WHITE);
        appName.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = new JLabel("Stay organized. Get things done.", JLabel.CENTER);
        subtitle.setFont(SUBTITLE_FONT);
        subtitle.setForeground(new Color(0xDC, 0xED, 0xC8));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(appName);
        header.add(javax.swing.Box.createVerticalStrut(5));
        header.add(subtitle);
        return header;
    }

    private JPanel createStatisticsPanel() {
        JPanel stats = new JPanel(new GridLayout(1, 3, 8, 0));
        stats.setOpaque(false);
        totalTasksLabel = createStatisticLabel();
        completedTasksLabel = createStatisticLabel();
        pendingTasksLabel = createStatisticLabel();
        stats.add(totalTasksLabel);
        stats.add(completedTasksLabel);
        stats.add(pendingTasksLabel);
        updateStatistics();
        return stats;
    }

    private JLabel createStatisticLabel() {
        JLabel label = new JLabel("", JLabel.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC5, 0xD5, 0xC5)),
                BorderFactory.createEmptyBorder(8, 6, 8, 6)));
        label.setOpaque(true);
        label.setBackground(new Color(0xE8, 0xF5, 0xE9));
        return label;
    }

    private void updateStatistics() {
        if (totalTasksLabel == null) {
            return;
        }
        int total = taskManager.getTaskCount();
        int completed = 0;
        for (Task task : taskManager.getAllTasks()) {
            if (task.isCompleted()) {
                completed++;
            }
        }
        totalTasksLabel.setText("Total: " + total);
        completedTasksLabel.setText("Completed: " + completed);
        pendingTasksLabel.setText("Pending: " + (total - completed));
    }

    private void styleInput(javax.swing.text.JTextComponent input) {
        input.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        input.setBackground(Color.WHITE);
        input.setForeground(new Color(0x26, 0x32, 0x38));
        input.setBorder(BorderFactory.createEmptyBorder(7, 9, 7, 9));
    }

    private JPanel createTaskCard(Task task) {
        JPanel card = new JPanel(new BorderLayout(10, 8));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        card.putClientProperty("taskCard", Boolean.TRUE);
        card.setBackground(darkMode ? DARK_CARD_BG : (task.isCompleted()
                ? new Color(0xE8, 0xF5, 0xE9) : Color.WHITE));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCF, 0xD8, 0xDC)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new javax.swing.BoxLayout(details,
                javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel(task.getTitle());
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(darkMode ? Color.WHITE : new Color(0x26, 0x32, 0x38));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(title);

        JLabel info = new JLabel("Priority: " + task.getPriority()
                + "  |  Due: " + task.getDueDateAsString()
                + (task.isCompleted() ? "  |  Completed" : ""));
        info.setFont(LABEL_FONT);
        info.setForeground(darkMode ? new Color(0xDC, 0xED, 0xC8)
                : new Color(0x45, 0x55, 0x5D));
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(javax.swing.Box.createVerticalStrut(4));
        details.add(info);
        if (task.getReminderTime() != null) {
            JLabel timer = new JLabel("Reminder: "
                    + task.getReminderTime().toString());
            timer.setFont(LABEL_FONT);
            timer.setForeground(darkMode ? new Color(0xDC, 0xED, 0xC8)
                    : new Color(0x45, 0x55, 0x5D));
            timer.setAlignmentX(Component.LEFT_ALIGNMENT);
            details.add(javax.swing.Box.createVerticalStrut(3));
            details.add(timer);
        }
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            JTextArea description = new JTextArea(task.getDescription());
            description.setFont(LABEL_FONT);
            description.setLineWrap(true);
            description.setWrapStyleWord(true);
            description.setEditable(false);
            description.setFocusable(false);
            description.setOpaque(false);
            description.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
            description.setForeground(darkMode ? new Color(0xE0, 0xE0, 0xE0)
                    : new Color(0x45, 0x55, 0x5D));
            description.setAlignmentX(Component.LEFT_ALIGNMENT);
            details.add(javax.swing.Box.createVerticalStrut(4));
            details.add(description);
        }
        card.add(details, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        JButton complete = makeButton(task.isCompleted() ? "Undo" : "Complete",
                COMPLETE_BG);
        JButton edit = makeButton("Edit", CLEAR_BG);
        JButton delete = makeButton("Delete", DELETE_BG);
        complete.addActionListener(e -> handleComplete(task));
        edit.addActionListener(e -> handleEdit(task));
        delete.addActionListener(e -> handleDelete(task));
        actions.add(complete);
        actions.add(edit);
        actions.add(delete);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void clearInputFields() {
        titleField.setText("");
        descriptionArea.setText("");
        priorityCombo.setSelectedIndex(0);
        dueDateField.setText("");
        reminderCheckBox.setSelected(false);
        hourSpinner.setValue(0);
        minuteSpinner.setValue(0);
    }

    private JPanel createTimePicker() {
        JPanel picker = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        picker.setOpaque(false);
        reminderCheckBox = new JCheckBox("Enable reminder");
        reminderCheckBox.setFont(LABEL_FONT);
        reminderCheckBox.setOpaque(false);
        hourSpinner = createTimeSpinner(0, 23);
        minuteSpinner = createTimeSpinner(0, 59);
        hourSpinner.setEnabled(false);
        minuteSpinner.setEnabled(false);
        reminderCheckBox.addActionListener(e -> {
            boolean enabled = reminderCheckBox.isSelected();
            hourSpinner.setEnabled(enabled);
            minuteSpinner.setEnabled(enabled);
        });
        picker.add(reminderCheckBox);
        picker.add(hourSpinner);
        picker.add(new JLabel("hour"));
        picker.add(minuteSpinner);
        picker.add(new JLabel("min"));
        return picker;
    }

    private JSpinner createTimeSpinner(int minimum, int maximum) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, minimum,
                maximum, 1));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "00"));
        spinner.setPreferredSize(new Dimension(58, 28));
        return spinner;
    }

    private LocalTime getSelectedReminderTime() {
        if (!reminderCheckBox.isSelected()) {
            return null;
        }
        return LocalTime.of((Integer) hourSpinner.getValue(),
                (Integer) minuteSpinner.getValue());
    }

    private void scheduleReminder(Task task) {
        cancelReminder(task);
        LocalDateTime reminderDateTime = LocalDateTime.of(task.getDueDate(),
                task.getReminderTime());
        long delayMillis = java.time.Duration.between(LocalDateTime.now(),
                reminderDateTime).toMillis();
        if (delayMillis <= 0) {
            return;
        }
        ScheduledFuture<?> job = reminderScheduler.schedule(() -> SwingUtilities.invokeLater(() -> {
            reminderJobs.remove(task);
            if (taskManager.getTaskIndex(task) < 0 || task.isCompleted()) {
                return;
            }
            soundNotifier.notifyTaskReminder(task.getTitle());
            JOptionPane.showMessageDialog(frame,
                    "Reminder: " + task.getTitle(), "Task Reminder",
                    JOptionPane.INFORMATION_MESSAGE);
        }), delayMillis, TimeUnit.MILLISECONDS);
        reminderJobs.put(task, job);
    }

    private void cancelReminder(Task task) {
        ScheduledFuture<?> job = reminderJobs.remove(task);
        if (job != null) {
            job.cancel(false);
        }
    }

    private void scheduleSavedReminders() {
        for (Task task : taskManager.getAllTasks()) {
            if (!task.isCompleted() && task.getDueDate() != null
                    && task.getReminderTime() != null) {
                scheduleReminder(task);
            }
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    private static class RoundedButtonUI extends BasicButtonUI {

        private final Color background;
        private static final int ARC = 12;

        RoundedButtonUI(Color background) {
            this.background = background;
        }

        @Override
        public void installUI(javax.swing.JComponent c) {
            super.installUI(c);
            JButton button = (JButton) c;
            button.setOpaque(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
        }

        @Override
        public void paint(Graphics g, javax.swing.JComponent c) {
            JButton button = (JButton) c;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

            int width = button.getWidth();
            int height = button.getHeight();

            Color bg = background;
            if (button.getModel().isPressed()) {
                bg = background.darker();
            } else if (button.getModel().isRollover()) {
                bg = background.brighter();
            }
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, width - 1, height - 1, ARC, ARC);

            g2.dispose();
            super.paint(g, c);
        }
    }
}
