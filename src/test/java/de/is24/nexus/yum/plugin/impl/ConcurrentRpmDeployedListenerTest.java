package de.is24.nexus.yum.plugin.impl;

import static de.is24.nexus.yum.repository.task.YumMetadataGenerationTask.ID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.sonatype.scheduling.TaskState.RUNNING;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.proxy.events.RepositoryItemEventStoreCreate;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.scheduling.ScheduledTask;

import com.google.code.tempusfugit.concurrency.ConcurrentRule;
import com.google.code.tempusfugit.concurrency.RepeatingRule;
import com.google.code.tempusfugit.concurrency.annotations.Concurrent;

import de.is24.nexus.yum.AbstractRepositoryTester;
import de.is24.nexus.yum.plugin.ItemEventListener;
import de.is24.nexus.yum.plugin.RepositoryRegistry;
import de.is24.nexus.yum.repository.service.YumService;
import de.is24.nexus.yum.repository.utils.RepositoryTestUtils;


/**
 * @author sherold
 * @author bvoss
 */
public class ConcurrentRpmDeployedListenerTest extends AbstractRepositoryTester {
  private static final Logger log = LoggerFactory.getLogger(ConcurrentRpmDeployedListenerTest.class);

  @Rule
  public ConcurrentRule concurrently = new ConcurrentRule();

  @Rule
  public RepeatingRule repeatedly = new RepeatingRule();

  @Inject
	private NexusScheduler nexusScheduler;

	@Inject
  private ItemEventListener listener;

  @Inject
  private RepositoryRegistry repositoryRegistry;

  @Inject
  private YumService yumService;

  @Before
  public void activateRepo() {
    yumService.activate();
  }

  @After
  public void reactivateRepo() {
    yumService.activate();
  }

  @Concurrent(count = 1)
  @Test
  public void shouldCreateRepoForPom() throws Exception {
    for (int j = 0; j < 5; j++) {
      shouldCreateRepoForRpm(j);
    }
    log.info("done");
  }

  private void shouldCreateRepoForRpm(int index) throws URISyntaxException, MalformedURLException,
    NoSuchAlgorithmException, IOException {
    MavenRepository repo = createRepository(true, "repo" + index);
    repositoryRegistry.registerRepository(repo);
    for (int version = 0; version < 5; version++) {
      assertNotMoreThan10ThreadForRpmUpload(repo, version);
    }
  }

  private void assertNotMoreThan10ThreadForRpmUpload(MavenRepository repo, int version) throws URISyntaxException,
    MalformedURLException, NoSuchAlgorithmException, IOException {
    String versionStr = version + ".1";
    File outputDirectory = new File(new URL(repo.getLocalUrl() + "/blalu/" +
        versionStr).toURI());
    File rpmFile = RepositoryTestUtils.createDummyRpm("test-artifact", versionStr, outputDirectory);

    StorageItem storageItem = createItem(versionStr, rpmFile.getName());

    listener.onEvent(new RepositoryItemEventStoreCreate(repo, storageItem));

		final int activeWorker = getRunningTasks();
		log.info("active worker: " + activeWorker);
    assertThat(activeWorker, is(lessThanOrEqualTo(10)));
  }

	private int getRunningTasks() {
		List<ScheduledTask<?>> tasks = nexusScheduler.getActiveTasks().get(ID);
		int count = 0;
		if (tasks != null) {
			for (ScheduledTask<?> task : tasks) {
				if (RUNNING.equals(task.getTaskState())) {
					count++;
				}
			}
		}
		return count;
	}
}
