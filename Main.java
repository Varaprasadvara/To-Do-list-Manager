import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        TaskManager taskManager = new TaskManager();
        SoundNotifier soundNotifier = new SoundNotifier();

        boolean loaded = taskManager.loadTasks();
        if (!loaded) {
            taskManager.addSampleTasksIfEmpty();
        }

        UIHelper uiHelper = new UIHelper(taskManager, soundNotifier);
        SwingUtilities.invokeLater(uiHelper::createAndShowGUI);
    }
}
