package org.faudroids.mrhyde.git;

import android.content.Context;
import android.support.annotation.NonNull;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.faudroids.mrhyde.github.GitHubRepository;
import org.faudroids.mrhyde.github.LoginManager;
import org.faudroids.mrhyde.utils.ObservableUtils;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import timber.log.Timber;

/**
 * Creates {@link GitManager} instances.
 */
@Singleton
public class GitManagerFactory {

  private final Context context;
  private final LoginManager loginManager;
  private final FileUtils fileUtils;

  @Inject
  public GitManagerFactory(Context context, LoginManager loginManager, FileUtils fileUtils) {
    this.context = context;
    this.loginManager = loginManager;
    this.fileUtils = fileUtils;
  }

  public GitManager openRepository(@NonNull final GitHubRepository repository) {
    try {
      File rootDir = getRepoRootDir(repository);
      Git client = Git.open(rootDir);
      return new GitManager(repository, client, rootDir, fileUtils);
    } catch (IOException e) {
      Timber.e(e, "Failed to open local git repository");
      return null;
    }
  }

  public Observable<GitManager> cloneRepository(@NonNull GitHubRepository repository) {
    return ObservableUtils.fromSynchronousCall(() -> {
      String cloneUrl = "https://"
          + loginManager.getAccount().getAccessToken()
          + ":x-oauth-basic@"
          + repository.getCloneUrl().replaceFirst("https://", "");
      File rootDir = getRepoRootDir(repository);
      Git client = Git
          .cloneRepository()
          .setURI(cloneUrl)
          .setDirectory(rootDir)
          .call();
      return new GitManager(repository, client, rootDir, fileUtils);
    });
  }

  public Observable<Boolean> hasRepositoryBeenCloned(@NonNull GitHubRepository repository) {
    return ObservableUtils.fromSynchronousCall(() -> {
      try {
        Git client = Git.open(getRepoRootDir(repository));
        Timber.d("" + client.getRepository().getDirectory().exists());
        Timber.d("" + client.getRepository().getDirectory());
        return client.getRepository().getDirectory().exists();
      } catch (RepositoryNotFoundException e) {
        return false;
      }
    });
  }

  private File getRepoRootDir(@NonNull GitHubRepository repository) {
    return new File(context.getFilesDir(), repository.getFullName());
  }

}
