import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class task2 {

    public static void main(String[] args) {
        // Путь к корневой директории файловой системы
        String rootPath = "/path/to/your/directory";

        // Создаем экземпляр ForkJoinPool
        ForkJoinPool forkJoinPool = new ForkJoinPool();

        // Поиск директорий, в которых больше 1000 файлов
        CountFilesTask countFilesTask = new CountFilesTask(new File(rootPath));
        int directoriesWithMoreThan1000Files = forkJoinPool.invoke(countFilesTask);

        System.out.println("Number of directories with more than 1000 files: " + directoriesWithMoreThan1000Files);

        // Поиск файлов по предикату (например, по размеру и расширению)
        SearchFilesTask searchFilesTask = new SearchFilesTask(new File(rootPath), file -> file.length() > 1000000 && file.getName().endsWith(".txt"));
        List<File> foundFiles = forkJoinPool.invoke(searchFilesTask);

        System.out.println("Found files:");
        for (File file : foundFiles) {
            System.out.println(file.getAbsolutePath());
        }

        // Завершаем работу ForkJoinPool
        forkJoinPool.shutdown();
    }

    static class CountFilesTask extends RecursiveTask<Integer> {
        private final File directory;

        public CountFilesTask(File directory) {
            this.directory = directory;
        }

        @Override
        protected Integer compute() {
            File[] files = directory.listFiles();
            if (files == null) {
                return 0;
            }

            int count = 0;
            List<CountFilesTask> subtasks = new ArrayList<>();

            for (File file : files) {
                if (file.isDirectory()) {
                    CountFilesTask subtask = new CountFilesTask(file);
                    subtask.fork();
                    subtasks.add(subtask);
                } else {
                    count++;
                }
            }

            for (CountFilesTask subtask : subtasks) {
                count += subtask.join();
            }

            return count;
        }
    }

    static class SearchFilesTask extends RecursiveTask<List<File>> {
        private final File directory;
        private final Predicate<File> predicate;

        public SearchFilesTask(File directory, Predicate<File> predicate) {
            this.directory = directory;
            this.predicate = predicate;
        }

        @Override
        protected List<File> compute() {
            File[] files = directory.listFiles();
            if (files == null) {
                return new ArrayList<>();
            }

            List<File> foundFiles = new ArrayList<>();
            List<SearchFilesTask> subtasks = new ArrayList<>();

            for (File file : files) {
                if (file.isDirectory()) {
                    SearchFilesTask subtask = new SearchFilesTask(file, predicate);
                    subtask.fork();
                    subtasks.add(subtask);
                } else {
                    if (predicate.test(file)) {
                        foundFiles.add(file);
                    }
                }
            }

            for (SearchFilesTask subtask : subtasks) {
                foundFiles.addAll(subtask.join());
            }

            return foundFiles;
        }
    }

    interface Predicate<T> {
        boolean test(T t);
    }
}
