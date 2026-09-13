import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {

    private static final String DEFAULT_SAVE_FILE = "tasks.dat";

    private final List<Task> tasks;
    private final String saveFile;

    public TaskManager() {
        this(DEFAULT_SAVE_FILE);
    }

    public TaskManager(String saveFile) {
        this.tasks = new ArrayList<>();
        this.saveFile = saveFile;
    }

    public boolean addTask(Task task) {
        if (task == null || task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            return false;
        }
        tasks.add(task);
        return true;
    }

    public boolean deleteTask(int index) {
        if (!isValidIndex(index)) {
            return false;
        }
        tasks.remove(index);
        return true;
    }

    public boolean setCompleted(int index, boolean completed) {
        if (!isValidIndex(index)) {
            return false;
        }
        tasks.get(index).setCompleted(completed);
        return true;
    }

    public Task getTask(int index) {
        if (!isValidIndex(index)) {
            return null;
        }
        return tasks.get(index);
    }

    public int getTaskIndex(Task task) {
        return tasks.indexOf(task);
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public int getTaskCount() {
        return tasks.size();
    }

    public List<Task> searchTasks(String query) {
        List<Task> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return results;
        }
        String lowerQuery = query.toLowerCase();
        for (Task t : tasks) {
            boolean inTitle = t.getTitle() != null
                    && t.getTitle().toLowerCase().contains(lowerQuery);
            boolean inDesc = t.getDescription() != null
                    && t.getDescription().toLowerCase().contains(lowerQuery);
            if (inTitle || inDesc) {
                results.add(t);
            }
        }
        return results;
    }

    public boolean saveTasks() {
        File file = new File(saveFile);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(tasks);
            return true;
        } catch (IOException e) {
            System.err.println("Error saving tasks: " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean loadTasks() {
        File file = new File(saveFile);
        if (!file.exists()) {
            return false;
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = in.readObject();
            if (obj instanceof List<?>) {
                tasks.clear();
                for (Object o : (List<?>) obj) {
                    if (o instanceof Task) {
                        tasks.add((Task) o);
                    }
                }
                return true;
            }
        } catch (EOFException e) {
            return false;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading tasks: " + e.getMessage());
            return false;
        }
        return false;
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < tasks.size();
    }

    public void addSampleTasksIfEmpty() {
        if (!tasks.isEmpty()) {
            return;
        }
        tasks.add(new Task("Welcome to ToDoListManager",
                "Click Add to create your own tasks.",
                "High", LocalDate.now().plusDays(1)));
        tasks.add(new Task("Try marking a task complete",
                "Select a task and press Complete.",
                "Medium", LocalDate.now().plusDays(2)));
    }
}
