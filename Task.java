import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String title;
    private String description;
    private String priority;
    private LocalDate dueDate;
    private boolean completed;
    private LocalTime reminderTime;

    public Task(String title, String description, String priority,
                LocalDate dueDate, boolean completed) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.completed = completed;
    }

    public Task(String title, String description, String priority, LocalDate dueDate) {
        this(title, description, priority, dueDate, false);
    }

    public LocalTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(LocalTime reminderTime) {
        this.reminderTime = reminderTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getDueDateAsString() {
        return (dueDate == null) ? "None" : dueDate.format(DATE_FORMAT);
    }

    public static LocalDate parseDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        String status = completed ? "[DONE]" : "[TODO]";
        return status + " " + title
                + "   (Priority: " + priority + ")"
                + "   Due: " + getDueDateAsString();
    }
}
