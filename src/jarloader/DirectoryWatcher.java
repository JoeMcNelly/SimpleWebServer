package jarloader;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class DirectoryWatcher implements Runnable {


	public void startTimer() throws IOException, InterruptedException {
		Path faxFolder = Paths.get("./MyPlugins");
		WatchService watchService = FileSystems.getDefault().newWatchService();
		faxFolder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

		boolean valid = true;
		do {
			WatchKey watchKey = watchService.take();

			for (WatchEvent event : watchKey.pollEvents()) {
				WatchEvent.Kind kind = event.kind();
				if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
					String fileName = event.context().toString();
					System.out.println("File Created:" + fileName);
				
				}
				if (StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind())) {
					String fileName = event.context().toString();
					System.out.println("File Removed: " + fileName);
				
				}

			}

			valid = watchKey.reset();

		} while (valid);

	}

	@Override
	public void run() {
		try {
			startTimer();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

	}
}
